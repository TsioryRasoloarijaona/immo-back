package com.tsix.immocrm.controller.perimetre;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat de réservation d'un dossier d'agence (R-C1) et règlements fermés à l'agence (R-D6, liste blanche A1d) —
 * FEATURE-agences-vente.md §5, §8, réponses du manager (27/11). Dossier d'agence = commercial vendeur de l'agence A.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesContratReservationTest {

    private static final String ACTE = "/api/dossiers/{id}/documents/acte-reservation";
    private static final String ACTE_LOT = "/api/dossiers/documents/acte-reservation";
    private static final String STATUT_REGLEMENT = "/api/dossiers/{id}/reglements/{reglementId}/statut";
    private static final String REGLEMENTS = "/api/dossiers/{id}/reglements";
    private static final String POINTE = "POINTED";
    private static final String EN_COURS = "IN_WORK";
    private static final String CORPS_POINTAGE = "{\"statut\":\"POINTED\",\"dateReglement\":\"2026-11-05T09:00:00\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureStatutsDossier statuts;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche tranche;
    private UUID vendeurA;
    private UUID vendeurSiege;
    private RequestPostProcessor compteA;

    private record Dossier(UUID id, String code) {
    }

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        siege = connecte(patrimoine.administrateur());
        tranche = plans.tranche("CRT-" + jeton, true);
        UUID agenceA = agences.agence("CRA-" + jeton);
        vendeurA = agences.vendeur("CRVA-" + jeton);
        vendeurSiege = agences.vendeur("CRVS-" + jeton);
        agences.lier(agenceA, vendeurA);
        compteA = compteAgence("CRC");
    }

    // ------------------------------------------------------------------ fixtures

    private RequestPostProcessor compteAgence(String prefixe, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeurA, autresRoles);
        plans.habiliter(compte, tranche.getCfgProject());
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = FixtureAgences.ROLE_AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    /** Dossier en Réservation sur un lot neuf de la tranche ; {@code commercial} = vendeur du dossier. */
    private Dossier dossier(String suffixe, UUID commercial) {
        StkItem article = plans.article(tranche, "CRL-" + suffixe + "-" + jeton, false, null);
        article.setArea(new BigDecimal("60.00"));
        ComFolder folder = plans.dossier(article, StatutDossier.RESERVATION, null, "Réservation");
        folder.setPrice(new BigDecimal("1000000.00"));
        folder.setDocDate(LocalDateTime.of(2026, 11, 1, 10, 0));
        Dossier dossier = new Dossier(folder.getId(), folder.getCode());
        agences.commercialDuDossier(dossier.id(), commercial);
        return dossier;
    }

    private Dossier dossierAgence(String suffixe) {
        return dossier(suffixe, vendeurA);
    }

    /** Échéance + règlement de statut {@code statut} affecté à cette échéance. */
    private UUID reglement(Dossier dossier, String statut) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(em.getReference(ComFolder.class, dossier.id()));
        echeance.setAmount(new BigDecimal("100000.00"));
        em.persist(echeance);

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode("CRR-" + UUID.randomUUID());
        reglement.setDocDate(LocalDateTime.of(2026, 11, 2, 10, 0));
        reglement.setAmount(new BigDecimal("50000.00"));
        reglement.setStatus(statut);
        reglement.setIsCredit(true);
        em.persist(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("50000.00"));
        em.persist(ligne);
        em.flush();
        return reglement.getId();
    }

    private ResultActions acte(Dossier dossier, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(ACTE, dossier.id()).with(qui));
    }

    private ResultActions acteEnLot(RequestPostProcessor qui, Dossier... dossiers) throws Exception {
        String ids = Arrays.stream(dossiers).map(d -> "\"" + d.id() + "\"").collect(Collectors.joining(","));
        return mockMvc.perform(post(ACTE_LOT).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dossierIds\":[" + ids + "]}"));
    }

    private ResultActions pointer(Dossier dossier, UUID reglement, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(patch(STATUT_REGLEMENT, dossier.id(), reglement).with(qui)
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_POINTAGE));
    }

    private ResultActions saisirReglement(Dossier dossier, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(REGLEMENTS, dossier.id()).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content("{\"montant\":1000,\"dateEcheance\":\"2026-11-10T00:00:00\",\"affectations\":[]}"));
    }

    // ------------------------------------------------------------------ R-C1 : contrat après pointage

    @Test
    void rC1_unitaire_dossierAgenceSansReglement_409() throws Exception {
        acte(dossierAgence("U0"), compteA).andExpect(status().isConflict());
    }

    @Test
    void rC1_lot_dossierAgenceSansReglement_409() throws Exception {
        acteEnLot(compteA, dossierAgence("L0")).andExpect(status().isConflict());
    }

    @Test
    void rC1_unitaire_reglementCanceledSeul_409() throws Exception {
        Dossier dossier = dossierAgence("UC");
        reglement(dossier, "CANCELED");

        acte(dossier, compteA).andExpect(status().isConflict());
    }

    @Test
    void rC1_unitaire_reglementEnCoursSeul_409() throws Exception {
        Dossier dossier = dossierAgence("UE");
        reglement(dossier, EN_COURS);

        acte(dossier, compteA).andExpect(status().isConflict());
    }

    @Test
    void rC1_unitaire_apresPointage_200() throws Exception {
        Dossier dossier = dossierAgence("UP");
        reglement(dossier, POINTE);

        acte(dossier, compteA).andExpect(status().isOk());
    }

    @Test
    void rC1_lot_apresPointage_200() throws Exception {
        Dossier dossier = dossierAgence("LP");
        reglement(dossier, POINTE);

        acteEnLot(compteA, dossier).andExpect(status().isOk());
    }

    // Réponse du manager (27/11) n°2 : le contrôle porte sur le dossier, quel que soit l'appelant.
    @Test
    void rC1_siegeSurDossierAgenceNonPointe_409() throws Exception {
        acte(dossierAgence("US"), siege).andExpect(status().isConflict());
    }

    // Réponse du manager (27/11) n°2 : un seul dossier d'agence non pointé refuse tout le lot ; le detail le nomme.
    @Test
    void rC1_lot_unDossierAgenceNonPointe_409PourToutLeLot_detailNommeLeDossier() throws Exception {
        Dossier pointe = dossierAgence("LM1");
        reglement(pointe, POINTE);
        Dossier duSiege = dossier("LM2", vendeurSiege);
        Dossier nonPointe = dossierAgence("LM3");

        acteEnLot(siege, pointe, duSiege, nonPointe)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(nonPointe.code())));
    }

    @Test
    void rC1_temoinSiege_dossierDuSiegeSansPointage_200() throws Exception {
        acte(dossier("TS", vendeurSiege), siege).andExpect(status().isOk());
    }

    @Test
    void rC1_temoinSiege_lotDossierDuSiegeSansPointage_200() throws Exception {
        acteEnLot(siege, dossier("TSL", vendeurSiege)).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ R-D6 : ni pointage ni saisie de règlement

    @Test
    void rD6_pointageParAgence_403() throws Exception {
        Dossier dossier = dossierAgence("P1");
        UUID reglement = reglement(dossier, EN_COURS);

        pointer(dossier, reglement, compteA).andExpect(status().isForbidden());

        assertThat(em.find(StlSettlement.class, reglement).getStatus()).isEqualTo(EN_COURS);
    }

    // Réponse du manager (27/11) n°3 : Agence + Administrateur est traité comme agence.
    @Test
    void rD6_pointageParAgenceAdministrateur_403() throws Exception {
        Dossier dossier = dossierAgence("P2");
        UUID reglement = reglement(dossier, EN_COURS);

        pointer(dossier, reglement, compteAgence("CRADM", "Administrateur")).andExpect(status().isForbidden());
    }

    @Test
    void rD6_temoinSiege_pointage_pas403() throws Exception {
        Dossier dossier = dossierAgence("P3");
        UUID reglement = reglement(dossier, EN_COURS);

        int statut = pointer(dossier, reglement, siege).andReturn().getResponse().getStatus();

        assertThat(statut).isNotEqualTo(403);
    }

    @Test
    void rD6_saisieReglementParAgence_403() throws Exception {
        saisirReglement(dossierAgence("S1"), compteA).andExpect(status().isForbidden());
    }

    @Test
    void rD6_temoinSiege_saisieReglement_pas403() throws Exception {
        int statut = saisirReglement(dossierAgence("S2"), siege).andReturn().getResponse().getStatus();

        assertThat(statut).isNotEqualTo(403);
    }
}
