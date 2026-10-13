package com.tsix.immocrm.controller.equipe;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carte « À distribuer » : liste, compteur et répartition par filtre (FEATURE-a-distribuer-jamais-traitees.md §3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ADistribuerTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String ADMIN = "Administrateur";
    private static final String LIGNES = "/api/equipe/a-distribuer/lignes";
    private static final String COMPTEUR = "/api/equipe/a-distribuer";
    private static final String PROJET = "projet";
    private static final String TOUTES = "toutes";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComActionMessageRepository messageRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComProspectionKindRepository kindRepository;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgProject projetB;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser chef;
    private AdmUser admin;
    private AdmUser commercialA;
    private ComProspection jamais;
    private ComProspection systemeSeule;
    private ComProspection traitee;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("ADI-A");
        projetB = outils.projet("ADI-B");
        trancheA = outils.tranche("ADI-TA", projetA);
        trancheB = outils.tranche("ADI-TB", projetB);
        chef = outils.compte("adi.chef", CHEF);
        outils.affecter(chef, projetA);
        admin = outils.compte("adi.admin", ADMIN);
        commercialA = outils.compte("adi.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);

        LocalDateTime maintenant = LocalDateTime.now();
        jamais = outils.preProspection("ADI-J1", trancheA, maintenant.minusDays(6));
        systemeSeule = outils.preProspection("ADI-S1", trancheA, maintenant.minusDays(5));
        action(systemeSeule, true);
        traitee = outils.preProspection("ADI-T1", trancheA, maintenant.minusDays(4));
        action(traitee, false);
        outils.preProspection("ADI-B1", trancheB, maintenant.minusDays(3));
        em.flush();
    }

    // ---------------------------------------------------------------- règle « jamais traitée »

    @Test
    void parDefaut_actionSystemeSeule_jamaisTraitee_actionManuelle_exclue() throws Exception {
        mockMvc.perform(get(LIGNES).with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].code").value("ADI-J1"))
                .andExpect(jsonPath("$.content[0].projetId").value(projetA.getId().toString()))
                .andExpect(jsonPath("$.content[0].trancheCode").value("ADI-TA"))
                .andExpect(jsonPath("$.content[0].nom").value("Nom PROS-ADI-J1"))
                .andExpect(jsonPath("$.content[0].dejaTraitee").value(false))
                .andExpect(jsonPath("$.content[1].code").value("ADI-S1"));
    }

    @Test
    void toutes_montreAussiLesTraitees_avecDejaTraitee() throws Exception {
        mockMvc.perform(get(LIGNES).param(TOUTES, "true").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].dejaTraitee").value(false))
                .andExpect(jsonPath("$.content[1].dejaTraitee").value(false))
                .andExpect(jsonPath("$.content[2].code").value("ADI-T1"))
                .andExpect(jsonPath("$.content[2].dejaTraitee").value(true));
    }

    /** FEATURE-tri-a-jour.md §Règle 1 : 0 action mais étape changée = déjà traitée ; « 1 A TRAITER » ne compte pas. */
    @Test
    void etapeChangee_dejaTraitee_etapeDeDepart_resteJamaisTraitee() throws Exception {
        jamais.setComProspectionKind(etape("2 EN COURS DE TRAITEMENT"));
        systemeSeule.setComProspectionKind(etape("1 A TRAITER"));
        em.flush();

        mockMvc.perform(get(LIGNES).with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("ADI-S1"));
        mockMvc.perform(get(LIGNES).param(TOUTES, "true").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("ADI-J1"))
                .andExpect(jsonPath("$.content[0].dejaTraitee").value(true))
                .andExpect(jsonPath("$.content[1].dejaTraitee").value(false));
    }

    private ComProspectionKind etape(String code) {
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode(code);
        etape.setDescription(code);
        return kindRepository.save(etape);
    }

    @Test
    void ligneAffecteeOuCloturee_jamaisADistribuer() throws Exception {
        systemeSeule.setCfgCommercial(commercialA.getCfgEmployee());
        jamais.setIsClosed(true);
        em.flush();

        mockMvc.perform(get(LIGNES).param(TOUTES, "true").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("ADI-T1"));
    }

    // ---------------------------------------------------------------- compteur = liste

    @Test
    void compteur_egalAuTotalDeLaListe_toutesVraiEtFaux() throws Exception {
        for (String toutes : List.of("false", "true")) {
            long total = nombre(get(LIGNES).param(TOUTES, toutes), "$.totalElements");
            long compteur = nombre(get(COMPTEUR).param(TOUTES, toutes), "$.nombre");
            assertThat(compteur).as("toutes=%s", toutes).isEqualTo(total).isPositive();
        }
    }

    @Test
    void compteur_parDefaut_ignoreLesTraitees() throws Exception {
        mockMvc.perform(get(COMPTEUR).with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(2))
                .andExpect(jsonPath("$.agePlusAncienneJours").value(6));
    }

    // ---------------------------------------------------------------- filtre projet et refus

    @Test
    void admin_filtreProjet_seulementCeProjet() throws Exception {
        mockMvc.perform(get(LIGNES).param(PROJET, projetB.getId().toString())
                        .with(EquipeOutils.connecte(admin, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("ADI-B1"));
    }

    @Test
    void chef_projetHorsPerimetre_403_commeLeCompteur() throws Exception {
        String horsPerimetre = projetB.getId().toString();
        mockMvc.perform(get(LIGNES).param(PROJET, horsPerimetre).with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COMPTEUR).param(PROJET, horsPerimetre).with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void commercial_lignes_403() throws Exception {
        mockMvc.perform(get(LIGNES).with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void periodeInversee_400() throws Exception {
        mockMvc.perform(get(LIGNES).param("du", "2026-10-11").param("au", "2026-09-12")
                        .with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagination_secondePage() throws Exception {
        mockMvc.perform(get(LIGNES).param("size", "1").param("page", "1").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].code").value("ADI-S1"));
    }

    // ---------------------------------------------------------------- répartir par filtre

    @Test
    void repartirParFiltre_prendLesLignesAuDelaDeLaPremierePage() throws Exception {
        mockMvc.perform(get(LIGNES).param("size", "1").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(jsonPath("$.content.length()").value(1));

        repartir("{\"filtre\":{\"projet\":\"" + projetA.getId() + "\"},\"simulation\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributions[*].preProspectionId",
                        containsInAnyOrder(jamais.getId().toString(), systemeSeule.getId().toString())));
    }

    @Test
    void repartirParFiltre_apercuEgalEcriture() throws Exception {
        String corps = "{\"filtre\":{\"projet\":\"" + projetA.getId() + "\",\"toutes\":true},\"simulation\":%s}";
        String apercu = repartir(corps.formatted("true")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ecriture = repartir(corps.formatted("false")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> attendues = JsonPath.read(apercu, "$.attributions[*].commercialId");
        List<String> ecrites = JsonPath.read(ecriture, "$.attributions[*].commercialId");
        assertThat(ecrites).hasSize(3).isEqualTo(attendues);
        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(traitee.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(commercialA.getCfgEmployee().getId());
    }

    @Test
    void repartir_niIdsNiFiltre_400() throws Exception {
        repartir("{\"preProspectionIds\":[],\"simulation\":true}").andExpect(status().isBadRequest());
    }

    @Test
    void repartir_idsEtFiltre_400() throws Exception {
        repartir("{\"preProspectionIds\":[\"" + jamais.getId() + "\"],\"filtre\":{},\"simulation\":true}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void repartirParFiltre_chefProjetHorsPerimetre_403() throws Exception {
        repartir("{\"filtre\":{\"projet\":\"" + projetB.getId() + "\"},\"simulation\":true}")
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- outils

    private void action(ComProspection p, boolean systeme) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(LocalDateTime.now().minusDays(1));
        m.setDescription("action");
        m.setWithoutRevival(false);
        m.setIsUnpaid(false);
        m.setIsSystem(systeme);
        messageRepository.save(m);
    }

    private long nombre(MockHttpServletRequestBuilder requete, String chemin) throws Exception {
        String reponse = mockMvc.perform(requete.with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(reponse, chemin)).longValue();
    }

    private ResultActions repartir(String corps) throws Exception {
        em.flush();
        return mockMvc.perform(post("/api/equipe/repartir")
                .with(EquipeOutils.connecte(chef, CHEF))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }
}
