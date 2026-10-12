package com.tsix.immocrm.controller.parcours;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
