package com.tsix.immocrm.controller.equipe;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Répartition automatique : {@code POST /api/equipe/repartir}, création et import (FEATURE-repartition-notifications.md §3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionLeadsTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String ADMIN = "Administrateur";
    private static final String ATTRIBUTIONS_0 = "$.attributions[0].commercialId";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StrHistoryRepository historyRepository;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgTranche trancheA;
    private CfgTranche trancheSansProjet;
    private AdmUser chef;
    private AdmUser admin;
    private AdmUser commercialA;
    private AdmUser commercialB;
    private LocalDateTime hier;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("REP-A");
        CfgProject projetB = outils.projet("REP-B");
        trancheA = outils.tranche("REP-TA", projetA);
        trancheSansProjet = outils.tranche("REP-TX", null);
        hier = LocalDateTime.now().minusDays(1);

        chef = outils.compte("rep.chef", CHEF);
        outils.affecter(chef, projetB);
        admin = outils.compte("rep.admin", ADMIN);
        // Noms « Nom EMP-rep.coma » < « Nom EMP-rep.comb » : départage final par nom.
        commercialA = outils.compte("rep.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        commercialB = outils.compte("rep.comb", COMMERCIAL);
        outils.affecter(commercialB, projetA);
    }

    // ---------------------------------------------------------------- le moins chargé

    @Test
    void repartir_egaliteDeCharge_parNomPuisAuFilDuLot() throws Exception {
        ComProspection l1 = lead("REP-L1", trancheA);
        ComProspection l2 = lead("REP-L2", trancheA);

        repartir(admin, ADMIN, List.of(l1.getId(), l2.getId()), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulation").value(false))
                .andExpect(jsonPath(ATTRIBUTIONS_0).value(employe(commercialA).toString()))
                .andExpect(jsonPath("$.attributions[0].motif").value("MOINS_CHARGE"))
                .andExpect(jsonPath("$.attributions[1].commercialId").value(employe(commercialB).toString()))
                .andExpect(jsonPath("$.parCommercial.length()").value(2));

        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(l1.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(employe(commercialA));
        assertThat(historyRepository.findByObjectIdInOrderByDocDateDesc(List.of(l1.getId())))
                .extracting(StrHistory::getDescription)
                .anyMatch(d -> d.startsWith("Commercial assigné : ") && d.endsWith("répartition automatique"));
    }

    @Test
    void repartir_versLeMoinsCharge() throws Exception {
        ComProspection aTrier = lead("REP-C1", trancheA);
        aTrier.setCfgCommercial(commercialA.getCfgEmployee());
        ComProspection nouveau = lead("REP-C2", trancheA);
        em.flush();

        repartir(admin, ADMIN, List.of(nouveau.getId()), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath(ATTRIBUTIONS_0).value(employe(commercialB).toString()));
    }

    @Test
    void repartir_egaliteDeCharge_laDerniereAttributionLaPlusAncienneGagne() throws Exception {
        // Close : hors de la charge, mais c'est la dernière pré-prospection reçue par A.
        ComProspection ancienne = lead("REP-E1", trancheA);
        ancienne.setCfgCommercial(commercialA.getCfgEmployee());
        ancienne.setIsClosed(true);
        ComProspection nouveau = lead("REP-E2", trancheA);
        em.flush();

        repartir(admin, ADMIN, List.of(nouveau.getId()), true)
                .andExpect(jsonPath(ATTRIBUTIONS_0).value(employe(commercialB).toString()));
    }

    // ---------------------------------------------------------------- lead connu

    @Test
    void repartir_leadConnu_versSonCommercialMemePlusCharge() throws Exception {
        String telephone = telephone();
        ComProspection suivie = outils.prospection("REP-K1", trancheA, commercialB.getCfgEmployee(), hier);
        suivie.setTempTel(telephone);
        ComProspection chargeB = lead("REP-K2", trancheA);
        chargeB.setCfgCommercial(commercialB.getCfgEmployee());
        ComProspection nouveau = lead("REP-K3", trancheA);
        nouveau.setTempTel("+212 " + telephone.substring(1));
        em.flush();

        repartir(admin, ADMIN, List.of(nouveau.getId()), true)
                .andExpect(jsonPath(ATTRIBUTIONS_0).value(employe(commercialB).toString()))
                .andExpect(jsonPath("$.attributions[0].motif").value("CONNU"));
    }

    @Test
    void repartir_leadConnuDUnCommercialNonEligible_moinsCharge() throws Exception {
        AdmUser horsProjet = outils.compte("rep.comc", COMMERCIAL);
        String telephone = telephone();
        ComProspection suivie = outils.prospection("REP-N1", trancheA, horsProjet.getCfgEmployee(), hier);
        suivie.setTempTel(telephone);
        ComProspection nouveau = lead("REP-N2", trancheA);
        nouveau.setTempTel(telephone);
        em.flush();

        repartir(admin, ADMIN, List.of(nouveau.getId()), true)
                .andExpect(jsonPath(ATTRIBUTIONS_0).value(employe(commercialA).toString()))
                .andExpect(jsonPath("$.attributions[0].motif").value("MOINS_CHARGE"));
    }

    // ---------------------------------------------------------------- lignes laissées

    @Test
    void repartir_sansProjet_dejaAttribuee_nonAttribuees() throws Exception {
        ComProspection sansProjet = lead("REP-S1", trancheSansProjet);
        ComProspection deja = lead("REP-S2", trancheA);
        deja.setCfgCommercial(commercialA.getCfgEmployee());
        em.flush();

        repartir(admin, ADMIN, List.of(sansProjet.getId(), deja.getId()), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributions.length()").value(0))
                .andExpect(jsonPath("$.nonAttribuees[0].raison").value("SANS_PROJET"))
                .andExpect(jsonPath("$.nonAttribuees[1].raison").value("DEJA_ATTRIBUEE"));

        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(sansProjet.getId()).orElseThrow().getCfgCommercial()).isNull();
        assertThat(prospectionRepository.findById(deja.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(employe(commercialA));
    }

    @Test
    void repartir_desactiveSansAffectationOuSansRestriction_aucunEligible() throws Exception {
        CfgProject projetC = outils.projet("REP-C");
        CfgTranche trancheC = outils.tranche("REP-TC", projetC);
        AdmUser desactive = outils.compte("rep.desact", outils.tier("EMP-rep.desact"), true, COMMERCIAL);
        outils.affecter(desactive, projetC);
        outils.compte("rep.libre", COMMERCIAL);
        ComProspection l = lead("REP-D1", trancheC);

        repartir(admin, ADMIN, List.of(l.getId()), false)
                .andExpect(jsonPath("$.nonAttribuees[0].raison").value("AUCUN_ELIGIBLE"));
    }

    @Test
    void repartir_simulation_nEcritRien() throws Exception {
        ComProspection l = lead("REP-SIM", trancheA);

        repartir(admin, ADMIN, List.of(l.getId()), true)
                .andExpect(jsonPath("$.simulation").value(true))
                .andExpect(jsonPath("$.attributions.length()").value(1));

        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(l.getId()).orElseThrow().getCfgCommercial()).isNull();
        assertThat(historyRepository.findByObjectIdInOrderByDocDateDesc(List.of(l.getId()))).isEmpty();
    }

    // ---------------------------------------------------------------- refus

    @Test
    void repartir_chefHorsDeSesProjets_403() throws Exception {
        ComProspection l = lead("REP-F1", trancheA);

        repartir(chef, CHEF, List.of(l.getId()), true).andExpect(status().isForbidden());
    }

    @Test
    void repartir_commercial_403() throws Exception {
        ComProspection l = lead("REP-F2", trancheA);

        repartir(commercialA, COMMERCIAL, List.of(l.getId()), true).andExpect(status().isForbidden());
    }

    @Test
    void repartir_ligneInconnue_404() throws Exception {
        repartir(admin, ADMIN, List.of(UUID.randomUUID()), true).andExpect(status().isNotFound());
    }

    @Test
    void repartir_listeVide_400() throws Exception {
        repartir(admin, ADMIN, List.of(), true).andExpect(status().isBadRequest());
    }

    @Test
    void repartir_ligneCloturee_409() throws Exception {
        ComProspection l = lead("REP-F3", trancheA);
        l.setIsClosed(true);
        em.flush();

        repartir(admin, ADMIN, List.of(l.getId()), true).andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- création manuelle

    @Test
    void creation_parUnCommercialHabilite_luiRevient() throws Exception {
        String id = creer(commercialB, COMMERCIAL, trancheA);

        assertThat(commercialDe(id)).isEqualTo(employe(commercialB));
    }

    @Test
    void creation_parUnExempte_repartie() throws Exception {
        String id = creer(admin, ADMIN, trancheA);

        assertThat(commercialDe(id)).isEqualTo(employe(commercialA));
    }

    @Test
    void creation_parUnCommercialSansHabilitationSurLeProjet_repartie() throws Exception {
        // Sans restriction : il peut écrire partout, mais n'est rattaché à aucun projet.
        AdmUser autre = outils.compte("rep.autre", COMMERCIAL);

        String id = creer(autre, COMMERCIAL, trancheA);

        assertThat(commercialDe(id)).isEqualTo(employe(commercialA));
    }

    @Test
    void creation_sansProjet_resteADistribuer() throws Exception {
        String id = creer(commercialA, COMMERCIAL, trancheSansProjet);

        assertThat(commercialDe(id)).isNull();
    }

    // ---------------------------------------------------------------- import

    @Test
    void import_repartitObligatoirementEtResume() throws Exception {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        String csv = "Créé,Nom,Adresse e-mail,Source,Formulaire,Canal,Étape,Propriétaire,Étiquettes,Téléphone,"
                + "Numéro de téléphone secondaire,Numéro WhatsApp\n"
                + "09/23/2026 1:45am,Imp Un " + suffixe + ",,Payé,F,E-mail,À évaluer,Faycal,," + telephone() + ",,\n"
                + "09/23/2026 2:45am,Imp Deux " + suffixe + ",,Payé,F,E-mail,À évaluer,Faycal,," + telephone() + ",,\n";

        mockMvc.perform(multipart("/api/pre-prospections/import")
                        .file(new MockMultipartFile("fichier", "leads.csv", "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8)))
                        .param("trancheId", trancheA.getId().toString())
                        .with(EquipeOutils.connecte(admin, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creees").value(2))
                .andExpect(jsonPath("$.repartition.length()").value(2))
                .andExpect(jsonPath("$.repartition[0].nombre").value(1))
                .andExpect(jsonPath("$.nonAttribuees").value(0));
    }

    // ---------------------------------------------------------------- outils

    private ComProspection lead(String code, CfgTranche tranche) {
        ComProspection p = outils.preProspection(code, tranche, hier);
        p.setCfgProspect(null);
        p.setTempDescription("Lead " + code);
        return p;
    }

    private static UUID employe(AdmUser user) {
        return user.getCfgEmployee().getId();
    }

    /** Neuf chiffres distincts d'un test à l'autre : la règle « lead connu » porte sur toute la table. */
    private static String telephone() {
        return "06" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }

    private ResultActions repartir(AdmUser appelant, String role, List<UUID> ids, boolean simulation) throws Exception {
        em.flush();
        String liste = ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
        return mockMvc.perform(post("/api/equipe/repartir")
                .with(EquipeOutils.connecte(appelant, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preProspectionIds\":" + liste + ",\"simulation\":" + simulation + "}"));
    }

    private String creer(AdmUser appelant, String role, CfgTranche tranche) throws Exception {
        em.flush();
        String reponse = mockMvc.perform(post("/api/pre-prospections")
                        .with(EquipeOutils.connecte(appelant, role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Lead " + UUID.randomUUID() + "\",\"telephone\":\"" + telephone()
                                + "\",\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reponse, "$.preProspection.id");
    }

    private UUID commercialDe(String id) {
        em.flush();
        em.clear();
        ComProspection p = prospectionRepository.findById(UUID.fromString(id)).orElseThrow();
        return p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null;
    }
}
