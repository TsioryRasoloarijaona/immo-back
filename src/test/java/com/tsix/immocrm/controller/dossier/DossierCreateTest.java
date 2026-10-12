package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutDossier;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.EntityManager;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierCreateTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private FixtureStatutDossier fixtureStatutDossier;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private com.tsix.immocrm.repositories.adm.AdmAlertDataRepository alerteRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private com.tsix.immocrm.repositories.str.StrHistoryRepository strHistoryRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private ComPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private com.tsix.immocrm.repositories.str.StrStatusRepository strStatusRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        fixtureStatutDossier.reservation();
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-CREATE-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-CREATE-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CREATE-TEST");
        stkVocationRepository.save(vocation);
    }

    private StkItem newAvailableItem(String code) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(item);
    }

    private ComProspection newProspectionWithProspect() {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-CREATE-TEST");
        tierRepository.save(prospect);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-CREATE-TEST");
        prospection.setCfgProspect(prospect);
        return prospectionRepository.save(prospection);
    }

    @Test
    void create_happyPath_linksMultipleArticles() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem main = newAvailableItem("LOT-CREATE-MAIN");
        StkItem annexe = newAvailableItem("LOT-CREATE-ANNEXE");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"101112131415"}
                """.formatted(prospection.getId(), main.getId(), annexe.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.articles.length()").value(2))
                .andExpect(jsonPath("$.clientId").value(prospection.getCfgProspect().getId().toString()));

        String prospectJson = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/prospects/{id}", prospection.getCfgProspect().getId()))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(
                        com.jayway.jsonpath.JsonPath.<String>read(prospectJson, "$.pieceIdentite.numero"))
                .isEqualTo("101112131415");
    }

    /**
     * La vente ferme la prospection et désarme ses relances.
     *
     * <p>Sans ça, la prospection reste <strong>active dans le pipeline</strong> après la vente :
     * elle continue d'apparaître dans {@code GET /api/prospections}, de compter dans les tableaux
     * de bord, et ses relances restent armées — un commercial est relancé sur une affaire déjà
     * signée. Mesuré sur les données réelles : <b>354 des 355</b> dossiers liés à une prospection
     * l'ont fermée, avec {@code ClosingDate} sur 354/354.
     *
     * <p>Le désarmement reprend le prédicat exact du logiciel de bureau : alerte présente ET non
     * rattachée à une échéance. L'interaction liée à une {@code ComDocument} doit rester armée —
     * c'est un rappel de paiement du dossier qui vient de naître.
     */
    @Test
    void create_fermeLaProspectionEtDesarmeSesRelances() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-CLOTURE-TEST");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-CLOTURE-TEST");
        paymentMethodRepository.save(modePaie);

        // Une relance en cours sur la prospection : elle doit être désarmée.
        ComActionMessage relance = new ComActionMessage();
        relance.setId(UUID.randomUUID());
        relance.setComProspection(prospection);
        relance.setDocDate(LocalDateTime.now());
        relance.setAlertValidityDate(LocalDateTime.now().plusDays(7));
        relance.setWithoutRevival(false);
        actionMessageRepository.save(relance);

        // Une interaction SANS alerte : hors du prédicat, elle ne doit pas être touchée.
        ComActionMessage sansAlerte = new ComActionMessage();
        sansAlerte.setId(UUID.randomUUID());
        sansAlerte.setComProspection(prospection);
        sansAlerte.setDocDate(LocalDateTime.now());
        sansAlerte.setWithoutRevival(false);
        actionMessageRepository.save(sansAlerte);

        // L'alerte de tableau de bord liée à la relance : la vente l'éteint, comme l'archivage.
        com.tsix.immocrm.entities.adm.AdmAlertData alerte = new com.tsix.immocrm.entities.adm.AdmAlertData();
        alerte.setId(UUID.randomUUID());
        alerte.setCode(alerte.getId().toString());
        alerte.setDescription(com.tsix.immocrm.entities.adm.AdmAlertData.DESCRIPTION_ALERTE);
        alerte.setGroupping(com.tsix.immocrm.entities.adm.AdmAlertData.GROUPE_PROSPECTION);
        alerte.setMessage("relance avant vente");
        alerte.setAlertDate(LocalDateTime.now().plusDays(7));
        alerte.setRelatedKey(relance.getId());
        alerte.setRightKey(com.tsix.immocrm.entities.adm.AdmAlertData.DROIT_COMMUNICATION);
        alerte.setActive(true);
        // saveAndFlush : le désarmement passe par une requête native, sans auto-flush.
        alerteRepository.saveAndFlush(alerte);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-CLOTURE"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();

        ComProspection fermee = prospectionRepository.findById(prospection.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(fermee.getIsClosed())
                .as("la prospection transformée doit sortir du pipeline")
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(fermee.getClosingDate()).isNotNull();
        // Aucun motif codé (le légataire n'en pose pas), mais son texte de clôture « vente ».
        org.assertj.core.api.Assertions.assertThat(fermee.getComProspectionClosingReason()).isNull();
        org.assertj.core.api.Assertions.assertThat(fermee.getClosingReasonText())
                .isEqualTo(com.tsix.immocrm.services.dossier.DossierService.TEXTE_CLOTURE_VENTE);
        org.assertj.core.api.Assertions.assertThat(alerteRepository.findById(alerte.getId()).orElseThrow().getActive())
                .as("l'alerte de la prospection vendue doit être éteinte")
                .isFalse();

        org.assertj.core.api.Assertions.assertThat(
                        actionMessageRepository.findById(relance.getId()).orElseThrow().getWithoutRevival())
                .as("la relance armée de la prospection doit être désarmée")
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        actionMessageRepository.findById(sansAlerte.getId()).orElseThrow().getWithoutRevival())
                .as("une interaction sans alerte est hors du prédicat et ne doit pas bouger")
                .isFalse();
    }

    /**
     * L'historique de statut doit dire <em>qui</em>, et le dossier doit apparaître dans les deux
     * tables d'historique du légataire.
     *
     * <p>{@code StrFolderStatusHistory} était écrite mais amputée de l'auteur et du client —
     * renseignés sur 1 282 des 1 308 lignes réelles (98 %). {@code StrHistory}, que nous
     * <em>lisions</em> déjà pour l'onglet « Historique », n'était jamais écrite : nos dossiers n'y
     * figuraient pas du tout.
     */
    @Test
    void create_historiseLeStatutDansLesDeuxTables() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-HISTO-TEST");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-HISTO-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-HISTO"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        String json = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID dossierId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));

        entityManager.flush();
        entityManager.clear();

        var statuts = statusHistoryRepository.findAll().stream()
                .filter(h -> h.getComFolder() != null && dossierId.equals(h.getComFolder().getId()))
                .toList();
        org.assertj.core.api.Assertions.assertThat(statuts).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(statuts.get(0).getCfgTier())
                .as("le client doit figurer sur la ligne d'historique, comme chez le légataire")
                .isNotNull();

        var generiques = strHistoryRepository.findAll().stream()
                .filter(h -> dossierId.equals(h.getObjectId()))
                .toList();
        org.assertj.core.api.Assertions.assertThat(generiques)
                .as("le dossier doit aussi apparaître dans StrHistory, que l'onglet Historique lit")
                .hasSize(1);
        org.assertj.core.api.Assertions.assertThat(generiques.get(0).getStrConfigEntityId())
                .isEqualTo(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
    }

    @Test
    void create_setsInitialStatusToReservation() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-STATUT-INIT-TEST");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-STATUT-INIT-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-STATUT-INIT"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        String json = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("RESERVATION"))
                .andReturn().getResponse().getContentAsString();

        // Also traceable in the history (see DossierHistoriqueTest for the endpoint itself).
        String dossierId = com.jayway.jsonpath.JsonPath.read(json, "$.id");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/dossiers/{id}/historique", dossierId))
                .andExpect(jsonPath("$.content[0].type").value("STATUT"))
                .andExpect(jsonPath("$.content[0].description").value("Statut changé vers RESERVATION"));
    }

    @Test
    void create_setsDocDateAndGeneratesCodeFromProjectAndSequence() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-CODE-TEST");
        // Manually-assigned id => Spring Data's save() sees a non-null id and calls merge(),
        // which returns a NEW managed instance rather than attaching this one — must reassign
        // and use the returned reference for any further association, or a later flush treats it
        // as a transient/unsaved instance (Hibernate TransientPropertyValueException).
        project = projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-CODE-TEST");
        tranche.setCfgProject(project);
        tranche = trancheRepository.save(tranche);

        StkItem item = newAvailableItem("LOT-CODE-TEST");
        item.setStkHierarchy(tranche);
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-CODE-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-CODE-TEST"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateDocument").exists())
                // "PRJ-CODE-TEST 2026/00001" — project code, current year, zero-padded sequence.
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(
                        "PRJ-CODE-TEST \\d{4}/\\d{5}")));

        // ComFolder.StkItemId (main article back-link) must be set — was missing entirely before
        // this fix, leaving folder.getStkItem() always null (broke FicheReservationService etc.).
        com.tsix.immocrm.entities.com.ComFolder saved = folderRepository.findAll().stream()
                .filter(f -> "LOT-CODE-TEST".equals(f.getStkItem() != null ? f.getStkItem().getCode() : null))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getStkItem().getCode()).isEqualTo("LOT-CODE-TEST");
    }

    @Test
    void create_missingModePaie_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-CREATE-NOMODEPAIE");

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"cin":"999"}
                """.formatted(prospection.getId(), item.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingCin_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-CREATE-NOCIN");

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("ESPECES-TEST");
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s"}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownProspection_returns400() throws Exception {
        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_prospectionWithoutProspect_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-NOPROSPECT");
        prospectionRepository.save(prospection);

        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(prospection.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_emptyArticleIds_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        String body = """
                {"prospectionId":"%s","articleIds":[]}
                """.formatted(prospection.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_avecPrixEtTranche_calculeMontantsHTTTCEtTotalAPayerAvecSyndic() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-MONTANTS-TEST");
        project = projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-MONTANTS-TEST");
        tranche.setCfgProject(project);
        tranche.setSyndicAmount(new java.math.BigDecimal("700.00"));
        tranche.setSyndicPeriod(24); // 700 x 24 = 16 800.00 DH de frais syndic
        tranche = trancheRepository.save(tranche);

        StkItem item = newAvailableItem("LOT-MONTANTS-TEST");
        item.setStkHierarchy(tranche);
        item.setVat(new java.math.BigDecimal("0.20"));
        item.setComSalePrice(new java.math.BigDecimal("1000000.00")); // prix commercial (liste) TTC
        item.setComSalePriceHT(new java.math.BigDecimal("833333.333333"));
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-MONTANTS-TEST");
        paymentMethodRepository.save(modePaie);

        // Prix négocié 900 000 (< 1 000 000 commercial) => remise de 100 000 (10 %).
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-MONTANTS-TEST","prix":900000.00}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(1000000.00))
                .andExpect(jsonPath("$.remise").value(10.0))
                // HT : 833 333,333333 − 900 000 / 1,2.
                .andExpect(jsonPath("$.remiseMontant").value(83333.333333))
                .andExpect(jsonPath("$.prixApresRemise").value(900000.00))
                .andExpect(jsonPath("$.prix").value(900000.00))
                // Total à payer = prix négocié (900 000, TTC) + Frais Syndic (16 800) — contrairement
                // à une Offre, dont le total n'inclut jamais le syndic.
                .andExpect(jsonPath("$.totalAPayer").value(916800.00));
    }

    @Test
    void create_avecRemisePercent_appliqueLaRemiseSaisieParLeCommercial() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-REMISE-TEST");
        project = projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REMISE-TEST");
        tranche.setCfgProject(project);
        tranche.setSyndicAmount(new java.math.BigDecimal("700.00"));
        tranche.setSyndicPeriod(24); // 700 x 24 = 16 800.00 DH de frais syndic
        tranche = trancheRepository.save(tranche);

        StkItem item = newAvailableItem("LOT-REMISE-TEST");
        item.setStkHierarchy(tranche);
        item.setVat(new java.math.BigDecimal("0.20"));
        item.setComSalePrice(new java.math.BigDecimal("1000000.00"));
        item.setComSalePriceHT(new java.math.BigDecimal("833333.333333"));
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-REMISE-TEST");
        paymentMethodRepository.save(modePaie);

        // Le commercial saisit 10 % de remise sur un prix avant remise de 1 000 000
        // => remise 100 000, prix dû 900 000, total à payer 900 000 + 16 800 de syndic.
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-REMISE-TEST","prix":1000000.00,"remisePercent":10}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        String creation = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(1000000.00))
                .andExpect(jsonPath("$.remise").value(10.0))
                .andExpect(jsonPath("$.remiseMontant").value(83333.333333))
                .andExpect(jsonPath("$.prixApresRemise").value(900000.00))
                .andExpect(jsonPath("$.prix").value(900000.00))
                .andExpect(jsonPath("$.totalAPayer").value(916800.00))
                .andReturn().getResponse().getContentAsString();

        // Relu en base : les colonnes sont dans la convention du logiciel de bureau (ratio, HT).
        com.tsix.immocrm.entities.com.ComFolder folder = recharger(creation);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(folder.getDiscount()).isEqualByComparingTo("0.1"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getDiscountAmount()).isEqualByComparingTo("83333.333333"));
    }

    @Test
    void create_avecRemisePercentEtAnnexe_prixEstLeTotal_annexeNonRecomptee() throws Exception {
        String creation = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAppartementEtParking("ANNEXE", "1200000.00", "5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(1200000.00))
                .andExpect(jsonPath("$.remise").value(5.0))
                .andExpect(jsonPath("$.remiseMontant").value(50000.00))
                .andExpect(jsonPath("$.prixApresRemise").value(1140000.00))
                .andExpect(jsonPath("$.totalAPayer").value(1140000.00))
                .andReturn().getResponse().getContentAsString();

        com.tsix.immocrm.entities.com.ComFolder folder = recharger(creation);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(folder.getItemPrice()).isEqualByComparingTo("1000000"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getUnderItemsPrice()).isEqualByComparingTo("200000"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getPriceBeforeDiscount()).isEqualByComparingTo("1200000"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getDiscount()).isEqualByComparingTo("0.05"),
                () -> org.assertj.core.api.Assertions.assertThat(folder.getPriceAfterDiscount()).isEqualByComparingTo("1140000"));
    }

    @Test
    void create_remiseZeroAvecAnnexe_rienNEstAjouteAuPrix() throws Exception {
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAppartementEtParking("ANNEXE0", "1200000.00", "0")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(1200000.00))
                .andExpect(jsonPath("$.remiseMontant").value(0))
                .andExpect(jsonPath("$.prixApresRemise").value(1200000.00));
    }

    @Test
    void create_remiseAvecPrixInferieurAuxAnnexes_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAppartementEtParking("ANNEXEKO", "150000.00", "5")))
                .andExpect(status().isBadRequest());
    }

    /** Appartement 1 000 000 + parking 200 000, sans tranche (pas de syndic). */
    private String corpsAppartementEtParking(String suffixe, String prix, String remisePercent) {
        ComProspection prospection = newProspectionWithProspect();
        StkItem principal = newAvailableItem("LOT-" + suffixe + "-PRINC");
        principal.setVat(new java.math.BigDecimal("0.20"));
        principal.setComSalePrice(new java.math.BigDecimal("1000000.00"));
        principal.setComSalePriceHT(new java.math.BigDecimal("833333.333333"));
        stkItemRepository.save(principal);
        StkItem annexe = newAvailableItem("LOT-" + suffixe + "-PARK");
        annexe.setComSalePrice(new java.math.BigDecimal("200000.00"));
        annexe.setComSalePriceHT(new java.math.BigDecimal("166666.666667"));
        stkItemRepository.save(annexe);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-" + suffixe);
        paymentMethodRepository.save(modePaie);
        return """
                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"CIN-%s","prix":%s,"remisePercent":%s}
                """.formatted(prospection.getId(), principal.getId(), annexe.getId(), modePaie.getId(),
                suffixe, prix, remisePercent);
    }

    /** ComFolder relu en base (flush + clear) : on compare ce qui est enregistré, pas l'entité en cache. */
    private com.tsix.immocrm.entities.com.ComFolder recharger(String reponseCreation) {
        entityManager.flush();
        entityManager.clear();
        return folderRepository
                .findById(UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponseCreation, "$.id"))).orElseThrow();
    }

    @Test
    void simulationPuisCreation_avecRemiseEtAnnexe_memesMontantsDeRemise() throws Exception {
        assertSimulationEgaleCreation("EGAL-EXPL", "1200000.00", "5");
    }

    @Test
    void simulationPuisCreation_sansRemise_memesMontantsDeRemiseDeduits() throws Exception {
        assertSimulationEgaleCreation("EGAL-DED", "1080000.00", null);
    }

    /** Mêmes entrées → la simulation annonce exactement ce que la création enregistre sur ComFolder. */
    private void assertSimulationEgaleCreation(String suffixe, String prix, String remisePercent) throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem principal = newAvailableItem("LOT-" + suffixe + "-PRINC");
        principal.setVat(new java.math.BigDecimal("0.20"));
        principal.setComSalePrice(new java.math.BigDecimal("1000000.00"));
        stkItemRepository.save(principal);
        StkItem annexe = newAvailableItem("LOT-" + suffixe + "-PARK");
        annexe.setComSalePrice(new java.math.BigDecimal("200000.00"));
        stkItemRepository.save(annexe);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-" + suffixe);
        paymentMethodRepository.save(modePaie);
        String remise = remisePercent != null ? ",\"remisePercent\":" + remisePercent : "";

        String simulation = mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"articleIds":["%s","%s"],"prix":%s%s}"""
                                .formatted(principal.getId(), annexe.getId(), prix, remise)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String creation = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"CIN-%s","prix":%s%s}"""
                                .formatted(prospection.getId(), principal.getId(), annexe.getId(),
                                        modePaie.getId(), suffixe, prix, remise)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        com.tsix.immocrm.entities.com.ComFolder folder = recharger(creation);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(montant(simulation, "$.prixAvantRemise"))
                        .isEqualByComparingTo(folder.getPriceBeforeDiscount()),
                () -> org.assertj.core.api.Assertions.assertThat(montant(simulation, "$.montantRemise"))
                        .isPositive()
                        .isEqualByComparingTo(folder.getPriceBeforeDiscount().subtract(folder.getPriceAfterDiscount())),
                // Le taux annoncé est celui que le dossier créé expose, et la base en garde le ratio.
                () -> org.assertj.core.api.Assertions.assertThat(montant(simulation, "$.remisePercent"))
                        .isEqualByComparingTo(montant(creation, "$.remise"))
                        .isEqualByComparingTo(folder.getDiscount().movePointRight(2)),
                () -> org.assertj.core.api.Assertions.assertThat(montant(simulation, "$.prixApresRemise"))
                        .isEqualByComparingTo(folder.getPriceAfterDiscount()));
    }

    private static java.math.BigDecimal montant(String json, String chemin) {
        Object valeur = com.jayway.jsonpath.JsonPath.read(json, chemin);
        return new java.math.BigDecimal(String.valueOf(valeur));
    }

    @Test
    void create_remisePercentZero_prixDuEgalPrixSaisi() throws Exception {
        ComProspection prospection = newProspectionWithProspect();

        StkItem item = newAvailableItem("LOT-REMISE-ZERO");
        item.setVat(new java.math.BigDecimal("0.20"));
        item.setComSalePrice(new java.math.BigDecimal("1000000.00"));
        item.setComSalePriceHT(new java.math.BigDecimal("833333.333333"));
        stkItemRepository.save(item);

        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-REMISE-ZERO");
        paymentMethodRepository.save(modePaie);

        // Remise explicite à 0 : le prix saisi est le prix avant ET après remise — à ne pas
        // confondre avec `remisePercent` absent, qui déduirait une remise de l'écart avec le
        // prix commercial de l'article.
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-REMISE-ZERO","prix":950000.00,"remisePercent":0}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixAvantRemise").value(950000.00))
                .andExpect(jsonPath("$.remise").value(0.0))
                .andExpect(jsonPath("$.remiseMontant").value(0))
                .andExpect(jsonPath("$.prixApresRemise").value(950000.00));
    }

    @Test
    void create_remisePercentNegative_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAvecRemise("NEG", "-5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_remisePercentSuperieureA100_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsAvecRemise("SUP", "110")))
                .andExpect(status().isBadRequest());
    }

    /** Requête de création valide en tout point sauf la remise — isole le motif du 400. */
    private String corpsAvecRemise(String suffixe, String remisePercent) {
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-REMISE-" + suffixe);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIREMENT-REMISE-" + suffixe);
        paymentMethodRepository.save(modePaie);

        return """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-REMISE-%s","prix":1000000.00,"remisePercent":%s}
                """.formatted(prospection.getId(), item.getId(), modePaie.getId(), suffixe, remisePercent);
    }

    // ============ FEATURE-interop-omran-ecritures.md §3 : ce qu'OMRAN écrit à l'insertion d'un dossier

    @Autowired
    private com.tsix.immocrm.fixtures.FixturePatrimoine fixturePatrimoine;
    @Autowired
    private com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository categoryRepository;
    @Autowired
    private com.tsix.immocrm.repositories.com.ComDocumentRepository documentRepository;
    @Autowired
    private com.tsix.immocrm.repositories.com.ComFolderItemRepository folderItemRepository;
    @Autowired
    private com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private com.tsix.immocrm.repositories.adm.AdmUserRepository userRepository;
    @Autowired
    private java.time.Clock clock;

    /** Prix de référence TTC du lot chiffré : AVANCE 245 700 + AUTO-FINANCEMENT 573 300. */
    private static final String PRIX_LOT = "819000";

    /** Colonnes de §3 qu'OMRAN remplit toujours : aucune ne doit rester NULL. */
    private static final java.util.List<String> COLONNES_OMRAN = java.util.List.of(
            "StrEntityId", "StrStatusId", "PartPercent", "IsOldCustomer", "Description", "DocDate", "Area",
            "GlobalArea", "UnitCostHT", "UnitCost", "VATRatio", "ItemPrice", "ItemPriceHT", "UnderItemsPrice",
            "UnderItemsPriceHT", "PriceBeforeDiscount", "PriceBeforeDiscountHT", "Discount", "DiscountAmount",
            "PriceAfterDiscount", "PriceAfterDiscountHT", "ItemPriceAfter", "ItemPriceAfterHT", "UnderItemsPriceAfter",
            "UnderItemsPriceAfterHT", "Price", "PriceHT", "TotalToPay", "AgreementPrice", "AgreementAmount",
            "ComDocumentDetail", "DeliveryRatio", "DeliveryMaxNumber", "ProbabilityOfFinalisation",
            "ModificationsAmount", "ModificationsAmountHT", "DeliveryNumber", "DeliveryAmount", "RomInterest",
            "CommissionRate", "IsAccounted", "IsPenaltyDisabled", "PriceWithModification", "IsConfirmedAgreementDate",
            "IsWithGiftChoiceLater", "Notes", "CancelDescription", "PenalityDescription", "CreditAgency",
            "FinalTitleNumber", "UserCreate", "UserCreateDateTime", "UserUpdate", "UserUpdateDateTime");

    /** Lot de 63 m², 10 833,333333 HT/m², TVA 0,20, tranche à 30 % d'avance et 700 × 24 de syndic. */
    private StkItem lotChiffre(com.tsix.immocrm.fixtures.FixturePatrimoine.Chaine chaine, String code) {
        chaine.tranche().setAdvancePercent(new java.math.BigDecimal("0.300000"));
        chaine.tranche().setSyndicAmount(new java.math.BigDecimal("700.000000"));
        chaine.tranche().setSyndicPeriod(24);
        trancheRepository.save(chaine.tranche());
        categorie(chaine, "AVANCE", true, false);
        categorie(chaine, "AUTO-FINANCEMENT", false, false);
        categorie(chaine, "FRAIS SYNDIC", false, true);
        StkItem lot = fixturePatrimoine.article(chaine, code);
        lot.setArea(new java.math.BigDecimal("63"));
        lot.setComSaleUnitCostHT(new java.math.BigDecimal("10833.333333"));
        lot.setVat(new java.math.BigDecimal("0.20"));
        lot.setComSalePrice(new java.math.BigDecimal(PRIX_LOT));
        return stkItemRepository.save(lot);
    }

    private void categorie(com.tsix.immocrm.fixtures.FixturePatrimoine.Chaine chaine, String code,
                           boolean avance, boolean exclue) {
        com.tsix.immocrm.entities.com.ComSettlementCategory categorie = new com.tsix.immocrm.entities.com.ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkVocation(chaine.vocation());
        categorie.setIsCredit(true);
        categorie.setIsSaleAdvance(avance);
        categorie.setExcludeFromPrice(exclue);
        categorie.setVatRatio(new java.math.BigDecimal("0.2"));
        categoryRepository.save(categorie);
    }

    private ComPaymentMethod modePaie(String code) {
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode(code);
        return paymentMethodRepository.save(modePaie);
    }

    private UUID creer(ComProspection prospection, StkItem lot, String prix,
                       org.springframework.test.web.servlet.request.RequestPostProcessor auteur) throws Exception {
        String champPrix = prix != null ? ",\"prix\":" + prix : "";
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-OMR"%s}
                """.formatted(prospection.getId(), lot.getId(), modePaie("VIR-" + lot.getCode()).getId(), champPrix);
        var requete = post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body);
        if (auteur != null) {
            requete = requete.with(auteur);
        }
        String json = mockMvc.perform(requete)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    /** Compte réel : sous {@code @WithMockUser}, l'auteur affiché est vide. */
    private com.tsix.immocrm.entities.adm.AdmUser nouvelUtilisateur(String code) {
        com.tsix.immocrm.entities.adm.AdmUser user = new com.tsix.immocrm.entities.adm.AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(code + "." + UUID.randomUUID());
        user.setDescription("Auteur OMRAN");
        user.setDisabled(false);
        return userRepository.save(user);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor connecte(
            com.tsix.immocrm.entities.adm.AdmUser user) {
        var principal = new com.tsix.immocrm.security.AuthenticatedUser(
                user.getId(), user.getCode(), null, false, true, 0);
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_Administrateur"))));
    }

    @Test
    void create_ecritLesColonnesComFolderCommeOmran_casChiffre() throws Exception {
        com.tsix.immocrm.entities.str.StrStatus reservation = fixtureStatutDossier.reservation();
        reservation.setDelay(15);
        strStatusRepository.save(reservation);
        var chaine = fixturePatrimoine.chaine("OMR-CHIFFRE");
        StkItem lot = lotChiffre(chaine, "LOT-OMR-CHIFFRE");

        UUID dossierId = creer(newProspectionWithProspect(), lot, PRIX_LOT, null);

        com.tsix.immocrm.entities.com.ComFolder f = folderRepository.findById(dossierId).orElseThrow();
        LocalDateTime minuit = java.time.LocalDate.now(clock).atStartOfDay();
        String aujourdhui = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(minuit);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(f.getStrEntityId())
                        .isEqualTo(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID),
                () -> org.assertj.core.api.Assertions.assertThat(f.getStrStatus().getCode()).isEqualTo("RESERVATION"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getDocDate()).isEqualTo(minuit),
                () -> org.assertj.core.api.Assertions.assertThat(f.getStatusValidityDate()).isEqualTo(minuit.plusDays(15)),
                () -> org.assertj.core.api.Assertions.assertThat(f.getDescription()).isEqualTo(f.getCode() + " du " + aujourdhui),
                () -> org.assertj.core.api.Assertions.assertThat(f.getPartPercent()).isEqualByComparingTo("1"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getIsOldCustomer()).isFalse(),
                () -> org.assertj.core.api.Assertions.assertThat(f.getArea()).isEqualByComparingTo("63"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getGlobalArea()).isEqualByComparingTo("63"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getUnitCostHT()).isEqualByComparingTo("10833.333333"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getUnitCost()).isEqualByComparingTo("13000"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getPrice()).isEqualByComparingTo(PRIX_LOT),
                // AVANCE 245 700 + AUTO-FINANCEMENT 573 300 ; FRAIS SYNDIC exclu du prix.
                () -> org.assertj.core.api.Assertions.assertThat(f.getAgreementPrice()).isEqualByComparingTo(PRIX_LOT),
                () -> org.assertj.core.api.Assertions.assertThat(f.getAgreementAmount()).isEqualByComparingTo("573300"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getDeliveryRatio()).isEqualByComparingTo("0.001"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getDeliveryMaxNumber()).isEqualTo(180),
                () -> org.assertj.core.api.Assertions.assertThat(f.getProbabilityOfFinalisation()).isEqualByComparingTo("1"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getUserUpdate()).isEqualTo(f.getUserCreate()),
                () -> org.assertj.core.api.Assertions.assertThat(f.getUserUpdateDateTime()).isEqualTo(f.getUserCreateDateTime()),
                () -> org.assertj.core.api.Assertions.assertThat(f.getComDocumentDetail())
                        .startsWith("Schèma de financement :\r\nDate :" + aujourdhui + "   Montant :")
                        .contains("   Montant :       245 700.00 DH   Réglé :             0.00 DH"
                                + "   Reste :       245 700.00 DH   Exclure du prix contractuel:Non   Catégorie : AVANCE\r\n")
                        .contains("Exclure du prix contractuel:Oui   Catégorie : FRAIS SYNDIC\r\n"));

        var lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        org.assertj.core.api.Assertions.assertThat(lignes).hasSize(3).allSatisfy(ligne -> {
            org.assertj.core.api.Assertions.assertThat(ligne.getCfgCompany().getId()).isEqualTo(chaine.societe().getId());
            org.assertj.core.api.Assertions.assertThat(ligne.getAdmModuleId())
                    .isEqualTo(com.tsix.immocrm.fixtures.ReferentielSeme.ADM_MODULE_COM);
            org.assertj.core.api.Assertions.assertThat(ligne.getDocDate()).isEqualTo(minuit);
            org.assertj.core.api.Assertions.assertThat(ligne.getAmountHT())
                    .isEqualByComparingTo(ligne.getAmount().divide(new java.math.BigDecimal("1.2"), 6,
                            java.math.RoundingMode.HALF_UP));
        });
    }

    @Test
    void create_colonnesNonNullesCommeOmran() throws Exception {
        var chaine = fixturePatrimoine.chaine("OMR-NONNULL");
        UUID dossierId = creer(newProspectionWithProspect(), lotChiffre(chaine, "LOT-OMR-NONNULL"), PRIX_LOT,
                connecte(nouvelUtilisateur("omr.nonnull")));

        Object[] valeurs = (Object[]) entityManager.createNativeQuery(
                        "SELECT " + String.join(", ", COLONNES_OMRAN) + " FROM ComFolder WHERE PKey = :id")
                .setParameter("id", dossierId)
                .getSingleResult();
        java.util.List<String> nulles = java.util.stream.IntStream.range(0, COLONNES_OMRAN.size())
                .filter(i -> valeurs[i] == null)
                .mapToObj(COLONNES_OMRAN::get)
                .toList();
        org.assertj.core.api.Assertions.assertThat(nulles).as("colonnes laissées NULL").isEmpty();
    }

    @Test
    void create_sansPrix_montantsDepuisLePrixDeReference() throws Exception {
        var chaine = fixturePatrimoine.chaine("OMR-SANSPRIX");
        UUID dossierId = creer(newProspectionWithProspect(), lotChiffre(chaine, "LOT-OMR-SANSPRIX"), null, null);

        com.tsix.immocrm.entities.com.ComFolder f = folderRepository.findById(dossierId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(f.getPrice()).isEqualByComparingTo(PRIX_LOT),
                () -> org.assertj.core.api.Assertions.assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo(PRIX_LOT),
                () -> org.assertj.core.api.Assertions.assertThat(f.getDiscountAmount()).isEqualByComparingTo("0"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getTotalToPay()).isEqualByComparingTo("835800"),
                () -> org.assertj.core.api.Assertions.assertThat(f.getComDocumentDetail())
                        .isEqualTo("Schèma de financement :\r\n"));
    }

    @Test
    void create_clientDejaClient_isOldCustomerVrai() throws Exception {
        com.tsix.immocrm.entities.cfg.CfgTierType typeClient = tierTypeRepository.findByCode("CLI").orElseGet(() -> {
            com.tsix.immocrm.entities.cfg.CfgTierType type = new com.tsix.immocrm.entities.cfg.CfgTierType();
            type.setId(UUID.randomUUID());
            type.setCode("CLI");
            type.setDescription("Client");
            return tierTypeRepository.save(type);
        });
        ComProspection prospection = newProspectionWithProspect();
        prospection.getCfgProspect().setCfgTierType(typeClient);
        tierRepository.save(prospection.getCfgProspect());

        UUID dossierId = creer(prospection, newAvailableItem("LOT-OMR-ANCIEN"), null, null);

        org.assertj.core.api.Assertions.assertThat(folderRepository.findById(dossierId).orElseThrow().getIsOldCustomer())
                .isTrue();
    }

    /** OMRAN refuse un dossier sans statut (RessourceWithWorkFlow.cs:411) : 500 explicite et rien d'écrit. */
    @Test
    void create_sansStatutReservation_refuse500EtNEcritRien() throws Exception {
        strStatusRepository.delete(fixtureStatutDossier.reservation());
        ComProspection prospection = newProspectionWithProspect();
        StkItem item = newAvailableItem("LOT-OMR-SANSSTATUT");
        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-REFUS"}
                """.formatted(prospection.getId(), item.getId(), modePaie("VIR-OMR-REFUS").getId());
        // Les fixtures doivent être en base : le clear() qui suit effacerait des insertions en attente.
        entityManager.flush();

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("RESERVATION")));

        entityManager.clear();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(stkItemRepository.findById(item.getId()).orElseThrow()
                        .getComFolderId()).isNull(),
                () -> org.assertj.core.api.Assertions.assertThat(prospectionRepository.findById(prospection.getId())
                        .orElseThrow().getIsClosed()).isNotEqualTo(Boolean.TRUE),
                () -> org.assertj.core.api.Assertions.assertThat(tierRepository.findById(prospection.getCfgProspect().getId())
                        .orElseThrow().getIDNumber()).isNotEqualTo("CIN-REFUS"));
    }

    @Test
    void create_avecSousArticle_ecritComFolderItemComFolderIdEtAffectation() throws Exception {
        com.tsix.immocrm.entities.adm.AdmUser moi = nouvelUtilisateur("omr.sous.article");
        var chaine = fixturePatrimoine.chaine("OMR-SOUS");
        StkItem lot = lotChiffre(chaine, "LOT-OMR-SOUS");
        StkItem terrasse = fixturePatrimoine.article(chaine, "LOT-OMR-SOUS TER");
        terrasse.setParent(lot);
        terrasse.setIsUnderItem(true);
        terrasse.setArea(new java.math.BigDecimal("15"));
        stkItemRepository.save(terrasse);
        ComProspection prospection = newProspectionWithProspect();

        UUID dossierId = creer(prospection, lot, PRIX_LOT, connecte(moi));

        UUID auteurId = moi.getId();
        var items = folderItemRepository.findAll().stream()
                .filter(i -> i.getComFolder() != null && dossierId.equals(i.getComFolder().getId()))
                .toList();
        var historique = statusHistoryRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        var generiques = strHistoryRepository.findAll().stream()
                .filter(h -> dossierId.equals(h.getObjectId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(stkItemRepository.findById(terrasse.getId()).orElseThrow()
                        .getComFolderId()).isEqualTo(dossierId),
                () -> org.assertj.core.api.Assertions.assertThat(folderRepository.findById(dossierId).orElseThrow()
                        .getGlobalArea()).isEqualByComparingTo("78"),
                () -> org.assertj.core.api.Assertions.assertThat(items).singleElement().satisfies(item -> {
                    org.assertj.core.api.Assertions.assertThat(item.getStkItem().getId()).isEqualTo(terrasse.getId());
                    org.assertj.core.api.Assertions.assertThat(item.getCode()).isEqualTo("LOT-OMR-SOUS TER");
                    org.assertj.core.api.Assertions.assertThat(item.getArea()).isEqualByComparingTo("15");
                    org.assertj.core.api.Assertions.assertThat(item.getIsUnderItem()).isTrue();
                    org.assertj.core.api.Assertions.assertThat(item.getParentId()).isEqualTo(lot.getId());
                }),
                () -> org.assertj.core.api.Assertions.assertThat(historique)
                        .extracting(com.tsix.immocrm.entities.str.StrFolderStatusHistory::getDescription,
                                com.tsix.immocrm.entities.str.StrFolderStatusHistory::getStkItemId)
                        .containsExactlyInAnyOrder(
                                org.assertj.core.groups.Tuple.tuple("AFFECTATION", terrasse.getId()),
                                org.assertj.core.groups.Tuple.tuple("", lot.getId())),
                () -> org.assertj.core.api.Assertions.assertThat(historique).isNotEmpty().allSatisfy(h -> {
                    org.assertj.core.api.Assertions.assertThat(h.getAdmUserId()).isEqualTo(auteurId);
                    org.assertj.core.api.Assertions.assertThat(h.getCfgTier().getId())
                            .isEqualTo(prospection.getCfgProspect().getId());
                }),
                () -> org.assertj.core.api.Assertions.assertThat(generiques).singleElement().satisfies(h -> {
                    org.assertj.core.api.Assertions.assertThat(h.getDescription()).isEqualTo("Création");
                    org.assertj.core.api.Assertions.assertThat(h.getStrStatusDescription()).isEqualTo("Réservation");
                }));
    }

    @Test
    void create_articleAlreadyOccupied_returns400() throws Exception {
        ComProspection prospection = newProspectionWithProspect();
        StkItem occupied = newAvailableItem("LOT-OCCUPIED");
        occupied.setComFolderId(UUID.randomUUID());
        stkItemRepository.save(occupied);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"]}
                """.formatted(prospection.getId(), occupied.getId());

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
