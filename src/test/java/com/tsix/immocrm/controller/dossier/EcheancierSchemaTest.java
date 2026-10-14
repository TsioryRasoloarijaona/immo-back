package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppression, rééquilibrage, défauts de catégorie, quittance, périodicité (FEATURE-vente-echeancier.md §1). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EcheancierSchemaTest {

    private static final String ECHEANCIER = "/api/dossiers/{id}/echeancier";
    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";
    private static final String SCHEMA = "/api/dossiers/{id}/echeancier/schema";
    private static final String PERIODIQUE = "/api/dossiers/{id}/echeancier/periodique";
    private static final BigDecimal PRIX = new BigDecimal("1000000");
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private java.time.Clock clock;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComSettlementCategoryRepository categorieRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;
    @Autowired private ComActionMessageRepository actionRepository;
    @Autowired private AdmAlertDataRepository alerteRepository;

    private CfgTranche tranche;
    private ComFolder dossier;
    private ComSettlementCategory avance;
    private ComSettlementCategory versement;
    private ComSettlementCategory autofinancement;
    private ComSettlementCategory credit;
    private ComDocument ligneAvance;
    private ComDocument autofinancementAncien;
    private ComDocument autofinancementRecent;

    /** Prix 1 000 000 = avance 300 000 + autofinancement 400 000 (janvier) + 300 000 (mars). */
    @BeforeEach
    void dossierEquilibre() {
        tranche = fixture.tranche("TR-ECH-SCH", true);
        dossier = fixture.dossier("DOS-ECH-SCH", tranche, StatutDossier.RESERVATION);
        dossier.setPriceAfterDiscount(PRIX);
        dossier.setPrice(PRIX);
        folderRepository.save(dossier);
        avance = categorie("AVANCE-SCH", c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        versement = categorie("VERSEMENT-SCH", c -> {
            c.setIsSaleAdvance(true);
            c.setCanUpdateDocuments(true);
        });
        autofinancement = categorie("AUTOFIN-SCH", c -> c.setIsSaleAutoFinancment(true));
        credit = categorie("CREDIT-SCH", c -> c.setIsSaleCredit(true));
        ligneAvance = fixture.echeance(dossier, avance, new BigDecimal("300000"), date(2026, 10, 1));
        autofinancementAncien = fixture.echeance(dossier, autofinancement, new BigDecimal("400000"), date(2027, 1, 1));
        autofinancementRecent = fixture.echeance(dossier, autofinancement, new BigDecimal("300000"), date(2027, 3, 1));
    }

    // ---- Rééquilibrage -------------------------------------------------------------------------------

    @Test
    void ajoutDUneLigneQuiMetAJourLeSchema_retrancheDuDernierAutofinancement() throws Exception {
        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(versement, "50000", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ecartSchema").value(0));

        assertAll(
                () -> assertThat(montant(autofinancementRecent)).isEqualByComparingTo("250000"),
                () -> assertThat(montant(autofinancementAncien)).isEqualByComparingTo("400000"),
                () -> assertThat(montant(ligneAvance)).isEqualByComparingTo("300000"));
        schemaEquilibre();
    }

    @Test
    void ligneQuiNeMetPasAJourLeSchema_aucunReport() throws Exception {
        ComSettlementCategory frais = categorie("FRAIS-SCH", c -> c.setIsSaleExpense(true));

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(frais, "16800", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated());

        assertThat(montant(autofinancementRecent)).isEqualByComparingTo("300000");
    }

    @Test
    void sansAutofinancement_lEcartVaAuDernierCredit() throws Exception {
        documentRepository.deleteAll(List.of(autofinancementAncien, autofinancementRecent));
        ComDocument ligneCredit = fixture.echeance(dossier, credit, new BigDecimal("700000"), date(2027, 6, 1));

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(versement, "100000", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated());

        assertThat(montant(ligneCredit)).isEqualByComparingTo("600000");
        schemaEquilibre();
    }

    @Test
    void hausseDUneAvance_retrancheLaDifference() throws Exception {
        mockMvc.perform(patch(ECHEANCE, dossier.getId(), ligneAvance.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":350000}"))
                .andExpect(status().isOk());

        assertThat(montant(autofinancementRecent)).isEqualByComparingTo("250000");
        schemaEquilibre();
    }

    // ---- Suppression ---------------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_rendLeMontantALAutofinancement_etSupprimeLesAlertes() throws Exception {
        ComDocument versementLigne = fixture.echeance(dossier, versement, new BigDecimal("50000"), date(2026, 11, 1));
        autofinancementRecent.setAmount(new BigDecimal("250000"));
        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setComDocument(versementLigne);
        actionRepository.save(action);
        AdmAlertData alerte = new AdmAlertData();
        alerte.setId(UUID.randomUUID());
        alerte.setRelatedKey(action.getId());
        alerteRepository.save(alerte);
        entityManager.flush();

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), versementLigne.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecart").value(0));

        entityManager.flush();
        assertAll(
                () -> assertThat(documentRepository.existsById(versementLigne.getId())).isFalse(),
                () -> assertThat(actionRepository.existsById(action.getId())).isFalse(),
                () -> assertThat(alerteRepository.existsById(alerte.getId())).isFalse(),
                () -> assertThat(montant(autofinancementRecent)).isEqualByComparingTo("300000"));
        schemaEquilibre();
    }

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_echeanceReglee_409EtRienNEstSupprime() throws Exception {
        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setAmount(new BigDecimal("1000"));
        settlementRepository.save(reglement);
        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(ligneAvance);
        ligne.setAmount(new BigDecimal("1000"));
        settlementLineRepository.save(ligne);

        refusee(ligneAvance);
    }

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_echeanceFacturee_409() throws Exception {
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO StlDocumentLine (Pkey, ComDocumentId) VALUES (:id, :doc)")
                .setParameter("id", UUID.randomUUID()).setParameter("doc", ligneAvance.getId()).executeUpdate();

        refusee(ligneAvance);
    }

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_echeanceEnDecompte_409() throws Exception {
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO StlPaymentAuthorizationLine (PKey, ComDocumentId) VALUES (:id, :doc)")
                .setParameter("id", UUID.randomUUID()).setParameter("doc", ligneAvance.getId()).executeUpdate();

        refusee(ligneAvance);
    }

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_echeanceEnRecouvrement_409() throws Exception {
        UUID recouvrement = UUID.randomUUID();
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO StlRecoveryFolder (PKey, Code) VALUES (:id, 'TST-REC')")
                .setParameter("id", recouvrement).executeUpdate();
        ligneAvance.setStlRecoveryFolderId(recouvrement);
        documentRepository.save(ligneAvance);

        refusee(ligneAvance);
    }

    @Test
    @WithMockUser(roles = ADMINISTRATEUR)
    void suppression_echeanceInconnueOuDUnAutreDossier_404() throws Exception {
        ComFolder autre = fixture.dossier("DOS-ECH-AUTRE", tranche, StatutDossier.RESERVATION);

        mockMvc.perform(delete(ECHEANCE, dossier.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(ECHEANCE, autre.getId(), ligneAvance.getId()))
                .andExpect(status().isNotFound());
    }

    // ---- Défauts de catégorie et quittance -----------------------------------------------------------

    @Test
    void fraisDeDossier_montantDeLaTranche_remiseAncienClient_montantDuInchange() throws Exception {
        tranche.setDefaultExpenseAmount(new BigDecimal("7500"));
        tranche.setDefaultExpenseAmountDiscount(new BigDecimal("0.1"));
        trancheRepository.save(tranche);
        dossier.setIsOldCustomer(true);
        folderRepository.save(dossier);
        ComSettlementCategory frais = categorie("FRAIS DOSSIER", c -> c.setIsSaleExpense(true));

        String json = mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"%s\"}".formatted(frais.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ComDocument ligne = relire(UUID.fromString(JsonPath.read(json, "$.id")));
        assertAll(
                () -> assertThat(ligne.getAmount()).isEqualByComparingTo("7500"),
                () -> assertThat(ligne.getDiscountRatio()).isEqualByComparingTo("0.1"),
                () -> assertThat(ligne.getAmountHT()).isEqualByComparingTo("6944.444444"),
                () -> assertThat(ligne.getDocDate()).isEqualTo(LocalDate.now(clock).atStartOfDay()));
    }

    @Test
    void ligneDeFrais_numeroDeQuittanceDeLaSequenceDesistement() throws Exception {
        ComSettlementCategory frais = categorie("FRAIS-QUIT", c -> c.setIsSaleExpense(true));

        String premiere = mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(frais, "1200", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String seconde = mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(frais, "1500", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String code1 = JsonPath.read(premiere, "$.decompte");
        String code2 = JsonPath.read(seconde, "$.decompte");
        assertAll(
                () -> assertThat(code1).matches("\\d{2}/\\d{5}"),
                () -> assertThat(Integer.parseInt(code2.substring(3))).isEqualTo(Integer.parseInt(code1.substring(3)) + 1));
    }

    @Test
    void ligneHorsFrais_pasDeQuittance() throws Exception {
        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(autofinancement, "1000", "2026-11-15T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decompte").doesNotExist())
                // Catégorie sans « mise à jour du schéma » : OMRAN laisse l'écart, la réponse le montre.
                .andExpect(jsonPath("$.ecartSchema").value(-1000));
    }

    @Test
    void categorieBaseeSurLesFinalisations_dateAbsente_prendLeDebutDesFinalisations() throws Exception {
        LocalDateTime debut = LocalDate.now(clock).plusMonths(6).atStartOfDay();
        tranche.setStartDateFinalisation(debut);
        trancheRepository.save(tranche);
        ComSettlementCategory finale = categorie("AUTOFIN-FINAL", c -> {
            c.setIsSaleAutoFinancment(true);
            c.setIsConfermentBasedDate(true);
        });

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"%s\",\"montant\":1000}".formatted(finale.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateEcheance").value(debut.toString().substring(0, 16) + ":00"));
    }

    @Test
    void categorieBaseeSurLesFinalisations_dateAnterieure_400() throws Exception {
        tranche.setStartDateFinalisation(LocalDateTime.of(2027, 6, 1, 0, 0));
        trancheRepository.save(tranche);
        ComSettlementCategory finale = categorie("AUTOFIN-FINAL2", c -> {
            c.setIsSaleAutoFinancment(true);
            c.setIsConfermentBasedDate(true);
        });

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(finale, "1000", "2027-05-31T00:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("début des finalisations")));
    }

    @Test
    void sansMontantNiDefaut_400() throws Exception {
        ComSettlementCategory libre = categorie("LIBRE-SCH", c -> c.setIsSaleExpense(true));

        mockMvc.perform(post(ECHEANCIER, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categorieId\":\"%s\"}".formatted(libre.getId())))
                .andExpect(status().isBadRequest());
    }

    // ---- Périodicité ---------------------------------------------------------------------------------

    @Test
    void periodique_nLignesSommeExacte_ecartReporteEtAutofinancementSoldeRetire() throws Exception {
        String json = mockMvc.perform(post(PERIODIQUE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId":"%s","montantTotal":300000,"nombre":7,"periodicite":"TRIMESTRIELLE",
                                 "premiereDate":"2026-12-01T00:00:00","notes":"Plan trimestriel"}"""
                                .formatted(versement.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        List<Number> montants = JsonPath.read(json, "$[*].montant");
        BigDecimal somme = montants.stream().map(n -> new BigDecimal(n.toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertAll(
                () -> assertThat(montants).hasSize(7),
                () -> assertThat(somme).isEqualByComparingTo("300000"),
                () -> assertThat(JsonPath.<List<String>>read(json, "$[*].dateEcheance"))
                        .startsWith("2026-12-01T00:00:00", "2027-03-01T00:00:00"),
                () -> assertThat(documentRepository.existsById(autofinancementRecent.getId())).isFalse(),
                () -> assertThat(montant(autofinancementAncien)).isEqualByComparingTo("400000"));
        schemaEquilibre();
    }

    @Test
    void periodique_montantAuDelaDuMontantInitial_400() throws Exception {
        mockMvc.perform(post(PERIODIQUE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categorieId":"%s","montantTotal":700000.01,"nombre":2,"periodicite":"MENSUELLE",
                                 "premiereDate":"2026-12-01T00:00:00"}""".formatted(versement.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void schema_etatEtMontantInitial() throws Exception {
        mockMvc.perform(get(SCHEMA, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avance").value(300000))
                .andExpect(jsonPath("$.autofinancement").value(700000))
                .andExpect(jsonPath("$.ecart").value(0))
                .andExpect(jsonPath("$.montantInitialPeriodicite").value(700000));
    }

    @Test
    void schema_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(SCHEMA, UUID.randomUUID())).andExpect(status().isNotFound());
    }

    // ---- Outils --------------------------------------------------------------------------------------

    private void schemaEquilibre() throws Exception {
        entityManager.flush();
        mockMvc.perform(get(SCHEMA, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecart").value(0));
    }

    private void refusee(ComDocument ligne) throws Exception {
        mockMvc.perform(delete(ECHEANCE, dossier.getId(), ligne.getId()))
                .andExpect(status().isConflict());
        entityManager.flush();
        assertThat(documentRepository.existsById(ligne.getId())).isTrue();
    }

    private BigDecimal montant(ComDocument ligne) {
        entityManager.flush();
        entityManager.refresh(ligne);
        return ligne.getAmount();
    }

    private ComDocument relire(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return documentRepository.findById(id).orElseThrow();
    }

    private ComSettlementCategory categorie(String code, java.util.function.Consumer<ComSettlementCategory> drapeaux) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setIsCredit(true);
        c.setVatRatio(new BigDecimal("0.2"));
        drapeaux.accept(c);
        return categorieRepository.save(c);
    }

    private static String corps(ComSettlementCategory categorie, String montant, String date) {
        return "{\"categorieId\":\"%s\",\"montant\":%s,\"dateEcheance\":\"%s\"}".formatted(categorie.getId(), montant, date);
    }

    private static LocalDateTime date(int annee, int mois, int jour) {
        return LocalDateTime.of(annee, mois, jour, 0, 0);
    }
}
