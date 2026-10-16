package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/journee} — FEATURE-parcours-commercial.md §3. Chaque test a son propre commercial : totaux exacts. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JourneeControllerTest {

    private static final String JOURNEE = "/api/journee";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParcoursOutils outils;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository messageRepository;

    @Autowired
    private FixtureInteraction fixtureInteraction;

    @Autowired
    private EntityManager em;

    @Autowired
    private Clock clock;

    private LocalDate aujourdhui;
    private AdmUser moi;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        aujourdhui = LocalDate.now(clock);
        moi = outils.compte("JR-MOI", "Commercial");
        tranche = outils.tranche("JR-T1");
    }

    @Test
    void nouveauxLeads_sertMesPreProspectionsRecentesSansInteraction() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.minusDays(2).atTime(9, 0));
        outils.preProspection(outils.compte("JR-AUTRE", "Commercial"), aujourdhui.atTime(9, 0));
        outils.preProspection(moi, aujourdhui.minusMonths(7).atStartOfDay());
        ComProspection dejaContacte = outils.preProspection(moi, aujourdhui.atTime(8, 0));
        outils.relance(dejaContacte, aujourdhui.plusDays(1).atTime(10, 0), null);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(moi.getCfgEmployee().getId().toString()))
                .andExpect(jsonPath("$.nouveauxLeads.total").value(1))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].prospectionId").value(lead.getId().toString()))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].ageJours").value(2))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].nom").value("Lead TEST"));
    }

    @Test
    void nouveauxLeads_ecarteUnePreProspectionFermee() throws Exception {
        ComProspection fermee = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        fermee.setIsClosed(true);
        prospectionRepository.saveAndFlush(fermee);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(0));
    }

    @Test
    void relancesEtVisitesDuJour_sontSepareesParLeResultat() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        ComProspection q = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        ComActionMessage appel = outils.relance(p, aujourdhui.atTime(11, 0),
                outils.resultat("Prospect ne répond pas", true));
        ComActionMessage visite = outils.relance(q, aujourdhui.atTime(16, 0),
                outils.resultat("Rendez vous pris", true));

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesDuJour.total").value(1))
                .andExpect(jsonPath("$.relancesDuJour.lignes[0].interactionId").value(appel.getId().toString()))
                .andExpect(jsonPath("$.relancesDuJour.lignes[0].projetCode").value("PRJ-JR-T1"))
                .andExpect(jsonPath("$.visitesDuJour.total").value(1))
                .andExpect(jsonPath("$.visitesDuJour.lignes[0].interactionId").value(visite.getId().toString()))
                .andExpect(jsonPath("$.enRetard.total").value(0));
    }

    /** Même bornage que le tableau de bord RETARD : prospection du mois civil, ouverte, qualifiée. */
    @Test
    void enRetard_reprendLaRegleDuTableauDeBord() throws Exception {
        ComProspection duMois = outils.prospection(moi, tranche, aujourdhui.withDayOfMonth(1).atStartOfDay());
        ComProspection ancienne = outils.prospection(moi, tranche,
                aujourdhui.withDayOfMonth(1).minusDays(1).atTime(12, 0));
        ComActionMessage retard = outils.relance(duMois, aujourdhui.minusDays(3).atStartOfDay(), null);
        outils.relance(ancienne, aujourdhui.minusDays(3).atStartOfDay(), null);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enRetard.total").value(1))
                .andExpect(jsonPath("$.enRetard.lignes[0].interactionId").value(retard.getId().toString()))
                .andExpect(jsonPath("$.enRetard.lignes[0].joursDeRetard").value(3))
                .andExpect(jsonPath("$.relancesDuJour.total").value(0));
    }

    @Test
    void relanceDUneProspectionFermeeOuDUnAutre_nApparaitPas() throws Exception {
        ComProspection fermee = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        fermee.setIsClosed(true);
        prospectionRepository.saveAndFlush(fermee);
        outils.relance(fermee, aujourdhui.atTime(10, 0), null);
        AdmUser autre = outils.compte("JR-AUTRE", "Commercial");
        outils.relance(outils.prospection(autre, tranche, aujourdhui.atStartOfDay()), aujourdhui.atTime(10, 0), null);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesDuJour.total").value(0));
    }

    // ---------------------------------------------------------------- refus et commercialId

    @Test
    @WithMockUser
    void sansCompteApplicatif_403() throws Exception {
        mockMvc.perform(get(JOURNEE)).andExpect(status().isForbidden());
    }

    @Test
    void commercialIdDemandeParUnCommercialSimple_403() throws Exception {
        AdmUser autre = outils.compte("JR-AUTRE", "Commercial");

        mockMvc.perform(get(JOURNEE).param("commercialId", autre.getCfgEmployee().getId().toString())
                        .with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrateur_voitSesLignesParDefautEtCellesDUnCommercialSurDemande() throws Exception {
        AdmUser admin = outils.compte("JR-ADM", "Administrateur");
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(0));
        mockMvc.perform(get(JOURNEE).param("commercialId", moi.getCfgEmployee().getId().toString())
                        .with(ParcoursOutils.connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].prospectionId").value(lead.getId().toString()));
    }

    @Test
    void chef_consulteUnMembreDeSonEquipeMaisPasUnAutre() throws Exception {
        AdmUser chef = outils.compte("JR-CHEF", "Commercial Chef");
        outils.affecter(chef, tranche);
        outils.affecter(moi, tranche);
        AdmUser horsEquipe = outils.compte("JR-HORS", "Commercial");
        outils.affecter(horsEquipe, outils.tranche("JR-T2"));

        mockMvc.perform(get(JOURNEE).param("commercialId", moi.getCfgEmployee().getId().toString())
                        .with(ParcoursOutils.connecte(chef, "Commercial Chef")))
                .andExpect(status().isOk());
        mockMvc.perform(get(JOURNEE).param("commercialId", horsEquipe.getCfgEmployee().getId().toString())
                        .with(ParcoursOutils.connecte(chef, "Commercial Chef")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lignesHorsDuPerimetreProjet_nApparaissentPas() throws Exception {
        CfgTranche autreProjet = outils.tranche("JR-T3");
        outils.affecter(moi, tranche);
        ComProspection horsPerimetre = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        horsPerimetre.setStkHierarchy(autreProjet);
        prospectionRepository.saveAndFlush(horsPerimetre);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(0));
    }

    // ---------------------------------------------------------------- à jour des actions faites ailleurs

    /** FEATURE-tri-a-jour.md §Règle 1 : une étape changée = traité, sans interaction écrite. */
    @Test
    void nouveauxLeads_etapeChangee_sortDesNouveauxLeads() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        ComProspectionKind enCours = outils.etape("2 EN COURS DE TRAITEMENT", false);

        mockMvc.perform(patch("/api/prospections/{id}/etape", lead.getId())
                        .with(ParcoursOutils.connecte(moi, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"etapeId\":\"%s\"}".formatted(enCours.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(0));
    }

    @Test
    void nouveauxLeads_aLEtapeDeDepart_resteNouveau() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        lead.setComProspectionKind(outils.etape("1 A TRAITER", false));
        prospectionRepository.saveAndFlush(lead);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(1))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].prospectionId").value(lead.getId().toString()));
    }

    /** §Règle 2 : une interaction sans nouvelle date close la relance en cours. */
    @Test
    void relanceDuJour_interactionSansDate_disparait() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        outils.relance(p, aujourdhui.atTime(11, 0), null);

        noter(p, null);

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesDuJour.total").value(0));
    }

    @Test
    void relanceDuJour_interactionAvecDate_remplaceLAncienne() throws Exception {
        ComProspection p = outils.prospection(moi, tranche, aujourdhui.atStartOfDay());
        outils.relance(p, aujourdhui.atTime(11, 0), null);

        String nouvelle = noter(p, aujourdhui.atTime(15, 0));

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesDuJour.total").value(1))
                .andExpect(jsonPath("$.relancesDuJour.lignes[0].interactionId").value(nouvelle));
    }

    /** §Règle 2 : la transformation close les relances vivantes du lead. */
    @Test
    void transformer_desarmeLesRelancesVivantes() throws Exception {
        ComProspection lead = outils.preProspection(moi, aujourdhui.atTime(9, 0));
        lead.setCfgProspect(outils.tier("JR-PROS"));
        prospectionRepository.saveAndFlush(lead);
        ComActionMessage relance = outils.relance(lead, aujourdhui.atTime(11, 0), null);

        mockMvc.perform(post("/api/prospections/{id}/transformer", lead.getId())
                        .with(ParcoursOutils.connecte(moi, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialId\":\"%s\",\"trancheId\":\"%s\"}"
                                .formatted(moi.getCfgEmployee().getId(), tranche.getId())))
                .andExpect(status().isOk());
        em.flush();

        assertThat(messageRepository.findById(relance.getId()))
                .get().extracting(ComActionMessage::getWithoutRevival).isEqualTo(true);
        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relancesDuJour.total").value(0));
    }

    /** Note postée par l'API ; {@code dateAlerte} nulle = sans nouvelle relance. Rend l'id de l'interaction. */
    private String noter(ComProspection p, LocalDateTime dateAlerte) throws Exception {
        String alerte = dateAlerte == null ? "" : ",\"dateAlerte\":\"%s\"".formatted(dateAlerte);
        String reponse = mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .with(ParcoursOutils.connecte(moi, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionTypeId\":\"%s\",\"description\":\"point fait\"%s}"
                                .formatted(fixtureInteraction.typeNote().getId(), alerte)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return JsonPath.read(reponse, "$.id");
    }

    @Test
    void nouveauxLeads_plusRecentsDAbord() throws Exception {
        LocalDateTime hier = aujourdhui.minusDays(1).atTime(9, 0);
        outils.preProspection(moi, hier);
        ComProspection recent = outils.preProspection(moi, aujourdhui.atTime(9, 0));

        mockMvc.perform(get(JOURNEE).with(ParcoursOutils.connecte(moi, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nouveauxLeads.total").value(2))
                .andExpect(jsonPath("$.nouveauxLeads.lignes[0].prospectionId").value(recent.getId().toString()));
    }
}
