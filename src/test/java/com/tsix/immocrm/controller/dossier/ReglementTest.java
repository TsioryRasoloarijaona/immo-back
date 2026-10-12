package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StlSettlementRepository settlementRepository;

    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-REG-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private ComDocument newEcheance(ComFolder folder) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new java.math.BigDecimal("100000"));
        return documentRepository.save(doc);
    }

    private StlSettlementType newType(String code) {
        return newType(code, false, false);
    }

    private StlSettlementType newType(String code, boolean referenceObligatoire, boolean banqueObligatoire) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(code);
        type.setIsReferenceMandatory(referenceObligatoire);
        type.setIsBankMandatory(banqueObligatoire);
        return settlementTypeRepository.save(type);
    }

    private CfgTier newPayeur(String code, String nom) {
        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode(code);
        payeur.setDescription(nom);
        return tierRepository.save(payeur);
    }

    /**
     * Corps de création complet. Depuis FEATURE-reglement-statut-preuve, {@code montant},
     * {@code dateEcheance}, {@code typeId} et {@code payeurId} sont tous obligatoires : un test
     * qui n'en omet qu'un seul teste bien ce champ-là, alors qu'un corps minimal renverrait 400
     * pour la première raison venue. {@code extra} ajoute les champs propres au test.
     */
    private String corpsComplet(String montant, StlSettlementType type, CfgTier payeur, String extra) {
        return "{\"montant\":" + montant + ",\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + payeur.getId() + "\""
                + (extra != null && !extra.isEmpty() ? "," + extra : "") + "}";
    }

    @Test
    void addReglement_happyPath_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-REG-1");
        ComDocument echeance = newEcheance(folder);
        StlSettlementType type = newType("VIREMENT-REG-1");
        CfgTier payeur = newPayeur("PAYEUR-REG-1", "Karim ALAMI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsComplet("100000.00", type, payeur, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(100000.00))
                .andExpect(jsonPath("$.echeanceId").value(echeance.getId().toString()))
                .andExpect(jsonPath("$.id").exists())
                // Status n'était jamais écrit par l'API avant cette feature : la colonne restait
                // NULL alors que les 3 218 règlements legacy saisis portent IN_WORK.
                .andExpect(jsonPath("$.statut").value("IN_WORK"))
                // Invariant : pas de SettlementDate tant que le règlement n'est pas pointé.
                .andExpect(jsonPath("$.dateReglement").doesNotExist());
    }

    @Test
    void addReglement_unknownDossier_returns404() throws Exception {
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-REG-2");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_echeanceBelongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-REG-3A");
        ComFolder folder2 = newFolder("DOS-REG-3B");
        ComDocument echeance = newEcheance(folder1);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder2.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_missingMontant_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-4");
        ComDocument echeance = newEcheance(folder);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_setsReferenceEtDateEcheance() throws Exception {
        // from_front.md gap: `reference` (StlSettlement.Code) was exposed on ReglementDto but
        // never settable on creation; `dateEcheance` (StlSettlement.EcheanceDate) wasn't exposed
        // at all despite being populated on >99% of real règlements — see CreerReglementRequest.
        // Le défaut silencieux de dateEcheance à now() a été retiré : le champ est désormais
        // requis, et c'est bien la valeur envoyée qui est relue (voir aussi
        // addReglement_dateEcheanceManquante_returns400).
        ComFolder folder = newFolder("DOS-REG-10");
        ComDocument echeance = newEcheance(folder);
        StlSettlementType type = newType("VIREMENT-REG-10");
        CfgTier payeur = newPayeur("PAYEUR-REG-10", "Samir TAZI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsComplet("100000.00", type, payeur, "\"reference\":\"CHQ-REG-10\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("CHQ-REG-10"))
                .andExpect(jsonPath("$.dateEcheance").value("2026-09-01T10:00:00"));
    }

    @Test
    void addReglement_typeRequiresReference_missingReference_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-11");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = newType("CHEQUE-TEST", true, false);
        CfgTier payeur = newPayeur("PAYEUR-REG-11", "Yassine BERRADA");

        // Corps complet SAUF la référence : sans cela le 400 tomberait pour un champ obligatoire
        // manquant et ne prouverait rien sur referenceObligatoire.
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsComplet("100000.00", type, payeur, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_setsPayeur() throws Exception {
        // payeurId was already readable (ReglementDto.payeurId/payeurNom, via StlSettlement.TierId,
        // "réglé par" — confirmed populated on >99% of real règlements) but never settable on
        // creation until now.
        ComFolder folder = newFolder("DOS-REG-13");
        ComDocument echeance = newEcheance(folder);

        CfgTier payeur = newPayeur("PAYEUR-REG-13", "Nadia BENJELLOUN");
        StlSettlementType type = newType("VIREMENT-REG-13");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsComplet("100000.00", type, payeur, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payeurId").value(payeur.getId().toString()))
                .andExpect(jsonPath("$.payeurNom").value("Nadia BENJELLOUN"));
    }

    @Test
    void addReglement_unknownPayeur_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-14");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = newType("VIREMENT-REG-14");

        String body = "{\"montant\":100000.00,\"dateEcheance\":\"2026-09-01T10:00:00\""
                + ",\"typeId\":\"" + type.getId() + "\",\"payeurId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_typeRequiresReference_referenceProvided_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-REG-12");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = newType("CHEQUE-TEST-2", true, false);
        CfgTier payeur = newPayeur("PAYEUR-REG-12", "Hicham OUAZZANI");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsComplet("100000.00", type, payeur, "\"reference\":\"CHQ-REG-12\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("CHQ-REG-12"));
    }

    @Test
    void getReglements_returnsFullHistoryWithBankAndTypeAndPayeur() throws Exception {
        ComFolder folder = newFolder("DOS-REG-5");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-TEST");
        type.setDescription("VIREMENT TEST");
        type.setIsBankMandatory(true);
        settlementTypeRepository.save(type);

        CfgTierType typeBanque = new CfgTierType();
        typeBanque.setId(UUID.randomUUID());
        typeBanque.setCode("BAN-REG-TEST");
        typeBanque.setDescription("BANQUE");
        tierTypeRepository.save(typeBanque);

        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode("BANQUE-REG-1");
        banque.setDescription("Banque Populaire");
        banque.setCfgTierType(typeBanque);
        tierRepository.save(banque);

        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-REG-1");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("CHQ-000123");
        settlement.setDocDate(LocalDateTime.now());
        settlement.setEcheanceDate(LocalDateTime.now());
        settlement.setSettlementDate(LocalDateTime.now());
        settlement.setAmount(new java.math.BigDecimal("50000"));
        settlement.setStlSettlementType(type);
        settlement.setCfgBank(banque);
        settlement.setTier(payeur);
        settlement.setNotes("Versement partiel");
        settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new java.math.BigDecimal("50000"));
        settlementLineRepository.save(line);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].echeanceId").value(echeance.getId().toString()))
                .andExpect(jsonPath("$[0].reference").value("CHQ-000123"))
                .andExpect(jsonPath("$[0].dateEcheance").exists())
                .andExpect(jsonPath("$[0].dateReglement").exists())
                .andExpect(jsonPath("$[0].montant").value(50000))
                .andExpect(jsonPath("$[0].typeCode").value("VIREMENT-TEST"))
                .andExpect(jsonPath("$[0].typeLibelle").value("VIREMENT TEST"))
                .andExpect(jsonPath("$[0].banqueNom").value("Banque Populaire"))
                .andExpect(jsonPath("$[0].payeurNom").value("Karim ALAMI"))
                .andExpect(jsonPath("$[0].notes").value("Versement partiel"));
    }

    @Test
    void getReglements_noneYet_returnsEmptyArray() throws Exception {
        ComFolder folder = newFolder("DOS-REG-6");
        ComDocument echeance = newEcheance(folder);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getReglements_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-REG-7");

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReglements_settlementSplitAcrossEcheances_idIsSettlementPKeyAndMontantIsLineShare() throws Exception {
        // Repro of instructions/from_front.md #8: one StlSettlement split across two échéances
        // (e.g. FRAIS DOSSIER + FRAIS SYNDIC) — each ReglementDto must carry its own line's amount
        // (not the settlement's total) and an `id` that IS the StlSettlement.PKey, since that id is
        // sent as-is to GET .../documents/recu.
        ComFolder folder = newFolder("DOS-REG-9");
        ComDocument echeanceFrais = newEcheance(folder);
        ComDocument echeanceSyndic = newEcheance(folder);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("VIR-000456");
        settlement.setDocDate(LocalDateTime.now());
        settlement.setSettlementDate(LocalDateTime.now());
        settlement.setAmount(new java.math.BigDecimal("16800"));
        settlementRepository.save(settlement);

        StlSettlementLine ligneFrais = new StlSettlementLine();
        ligneFrais.setId(UUID.randomUUID());
        ligneFrais.setStlSettlement(settlement);
        ligneFrais.setComDocument(echeanceFrais);
        ligneFrais.setAmount(new java.math.BigDecimal("8400"));
        settlementLineRepository.save(ligneFrais);

        StlSettlementLine ligneSyndic = new StlSettlementLine();
        ligneSyndic.setId(UUID.randomUUID());
        ligneSyndic.setStlSettlement(settlement);
        ligneSyndic.setComDocument(echeanceSyndic);
        ligneSyndic.setAmount(new java.math.BigDecimal("8400"));
        settlementLineRepository.save(ligneSyndic);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeanceFrais.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(settlement.getId().toString()))
                .andExpect(jsonPath("$[0].montant").value(8400));

        // The `id` returned above (StlSettlement.PKey) must resolve directly against the reçu
        // endpoint — previously it was the StlSettlementLine PKey instead, causing a 404
        // "Règlement introuvable" on an id copied straight out of this DTO (from_front.md #8).
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        folder.getId(), settlement.getId()))
                .andExpect(status().isOk());
    }

    // ============ FEATURE-interop-omran-ecritures.md §5 : ce qu'OMRAN écrit sur un encaissement

    @Autowired
    private com.tsix.immocrm.fixtures.FixturePatrimoine fixturePatrimoine;
    @Autowired
    private com.tsix.immocrm.services.dossier.RecuReglementService recuReglementService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    @Autowired
    private java.time.Clock clock;

    /** Dossier vendu à {@code client}, sur un lot dont la tranche porte une société. */
    private ComFolder dossierAvecClient(String code, CfgTier client, com.tsix.immocrm.fixtures.FixturePatrimoine.Chaine chaine) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(chaine.vocation());
        folder.setStkItem(fixturePatrimoine.article(chaine, "LOT-" + code));
        folder.setCfgCustomer(client);
        return folderRepository.save(folder);
    }

    private CfgTier banque(String code) {
        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode(code);
        banque.setDescription("Banque " + code);
        return tierRepository.save(banque);
    }

    private StlSettlement creerEtRelire(ComFolder folder, ComDocument echeance, String corps) throws Exception {
        String json = mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                                folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return settlementRepository.findById(UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id")))
                .orElseThrow();
    }

    private String aujourdhui() {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(java.time.LocalDate.now(clock));
    }

    @Test
    void addReglement_ecritLesColonnesOmran_tierClientEtPayeurSaisi() throws Exception {
        var chaine = fixturePatrimoine.chaine("REG-OMR");
        CfgTier client = newPayeur("CLIENT-REG-OMR", "Client du dossier");
        CfgTier payeur = newPayeur("PAYEUR-REG-OMR", "Payeur SAISI");
        ComFolder folder = dossierAvecClient("DOS-REG-OMR", client, chaine);
        ComDocument echeance = newEcheance(folder);
        StlSettlementType type = newType("CHEQUE-OMR");
        CfgTier bmce = banque("BMCE");

        StlSettlement s = creerEtRelire(folder, echeance, corpsComplet("100000.00", type, payeur,
                "\"banqueId\":\"" + bmce.getId() + "\",\"reference\":\"CHQ-1\""));

        java.time.LocalDateTime minuit = java.time.LocalDate.now(clock).atStartOfDay();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(s.getTier().getId()).isEqualTo(client.getId()),
                () -> org.assertj.core.api.Assertions.assertThat(s.getTierPayer().getId()).isEqualTo(payeur.getId()),
                () -> org.assertj.core.api.Assertions.assertThat(s.getAdmModuleId())
                        .isEqualTo(com.tsix.immocrm.fixtures.ReferentielSeme.ADM_MODULE_COM),
                () -> org.assertj.core.api.Assertions.assertThat(s.getStrEntityId())
                        .isEqualTo(com.tsix.immocrm.fixtures.ReferentielSeme.STR_ENTITY_ENCAISSEMENT),
                () -> org.assertj.core.api.Assertions.assertThat(s.getStlCurrencyId())
                        .isEqualTo(com.tsix.immocrm.fixtures.ReferentielSeme.STL_CURRENCY_MAD),
                () -> org.assertj.core.api.Assertions.assertThat(s.getStlCurrencyAffectationId())
                        .isEqualTo(com.tsix.immocrm.fixtures.ReferentielSeme.STL_CURRENCY_MAD),
                () -> org.assertj.core.api.Assertions.assertThat(s.getCfgCompany().getId()).isEqualTo(chaine.societe().getId()),
                () -> org.assertj.core.api.Assertions.assertThat(s.getConvertValue()).isEqualByComparingTo("1"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getConvertUnit()).isEqualTo(1),
                () -> org.assertj.core.api.Assertions.assertThat(s.getAmountBase()).isEqualByComparingTo("100000"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getAmountBaseConverted()).isEqualByComparingTo("100000"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getAmountRS()).isEqualByComparingTo("0"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getRsRatio()).isEqualByComparingTo("0"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getAmountStamp()).isEqualByComparingTo("0"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getRSAmountSettled()).isFalse(),
                () -> org.assertj.core.api.Assertions.assertThat(s.getIsAccounted()).isFalse(),
                () -> org.assertj.core.api.Assertions.assertThat(s.getIsDcoumentDeliveredToCenter()).isFalse(),
                () -> org.assertj.core.api.Assertions.assertThat(s.getCategory()).isEqualTo("None"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getItemNumber()).isEqualTo("Non définie"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getItemDescription()).isEqualTo("Non définie"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getIsCredit()).isTrue(),
                () -> org.assertj.core.api.Assertions.assertThat(s.getStatus()).isEqualTo("IN_WORK"),
                () -> org.assertj.core.api.Assertions.assertThat(s.getDocDate()).isEqualTo(minuit),
                () -> org.assertj.core.api.Assertions.assertThat(s.getEcheanceDate())
                        .isEqualTo(java.time.LocalDateTime.of(2026, 9, 1, 10, 0)),
                () -> org.assertj.core.api.Assertions.assertThat(s.getDescription())
                        .isEqualTo("CHEQUE-OMR BMCE N° CHQ-1 du " + aujourdhui()),
                () -> org.assertj.core.api.Assertions.assertThat(s.getUserCreateDateTime())
                        .isNotNull().isEqualTo(s.getUserUpdateDateTime()));

        // Le payeur affiché est TierPayerId, pas le client (TierId).
        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(jsonPath("$[0].payeurId").value(payeur.getId().toString()))
                .andExpect(jsonPath("$[0].payeurNom").value("Payeur SAISI"));
    }

    @Test
    void addReglement_sansBanqueNiReference_descriptionCourte() throws Exception {
        var chaine = fixturePatrimoine.chaine("REG-DESC");
        CfgTier client = newPayeur("CLIENT-REG-DESC", "Client");
        ComFolder folder = dossierAvecClient("DOS-REG-DESC", client, chaine);
        ComDocument echeance = newEcheance(folder);

        StlSettlement s = creerEtRelire(folder, echeance,
                corpsComplet("50000.00", newType("ESPECES-OMR"), client, null));

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(s.getDescription()).isEqualTo("ESPECES-OMR du " + aujourdhui()),
                () -> org.assertj.core.api.Assertions.assertThat(s.getCode()).isEmpty(),
                () -> org.assertj.core.api.Assertions.assertThat(s.getTierPayer().getId()).isEqualTo(s.getTier().getId()));
    }

    @Test
    void recu_imprimeLePayeurSaisi_pasLeClient() throws Exception {
        var chaine = fixturePatrimoine.chaine("REG-RECU");
        CfgTier client = newPayeur("CLIENT-REG-RECU", "Client du dossier");
        CfgTier payeur = newPayeur("PAYEUR-REG-RECU", "Payeur du recu");
        ComFolder folder = dossierAvecClient("DOS-REG-RECU", client, chaine);
        ComDocument echeance = newEcheance(folder);

        StlSettlement s = creerEtRelire(folder, echeance,
                corpsComplet("100000.00", newType("VIREMENT-RECU"), payeur, null));

        org.assertj.core.api.Assertions.assertThat(recuReglementService.collect(folder.getId(), s.getId()).beneficiaire().nom())
                .isEqualTo("Payeur du recu");
    }

    /** GEN0016 : le passage de statut signe la mise à jour du règlement. */
    @Test
    void changerStatutReglement_signeUserUpdate() throws Exception {
        ComFolder folder = newFolder("DOS-REG-MAJ");
        ComDocument echeance = newEcheance(folder);
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.now());
        settlement.setAmount(new java.math.BigDecimal("1000"));
        settlement.setStatus("IN_WORK");
        settlementRepository.save(settlement);
        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new java.math.BigDecimal("1000"));
        settlementLineRepository.save(line);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/dossiers/{id}/reglements/{reglementId}/statut", folder.getId(), settlement.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        org.assertj.core.api.Assertions.assertThat(settlementRepository.findById(settlement.getId()).orElseThrow()
                .getUserUpdateDateTime()).isNotNull();
    }

    @Test
    void getReglements_echeanceBelongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-REG-8A");
        ComFolder folder2 = newFolder("DOS-REG-8B");
        ComDocument echeance = newEcheance(folder1);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder2.getId(), echeance.getId()))
                .andExpect(status().isNotFound());
    }
}
