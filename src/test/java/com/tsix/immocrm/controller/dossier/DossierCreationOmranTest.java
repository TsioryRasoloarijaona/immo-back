package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Création d'un dossier écrite comme OMRAN l'écrit (FEATURE-interop-omran-ecritures.md §3, FEATURE-vente-socle.md A).
 * Cas chiffré : lot 63 m² à 10 833,333333 HT/m² (TVA 0,20), terrasse 12 m² à 50 000, prix 869 000,
 * avance 30 %, frais de syndic 700 × 24.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierCreationOmranTest {

    private static final String AUTEUR = "Agent Creation";
    private static final String DOSSIERS = "/api/dossiers";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private Clock clock;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private FixtureStatutsDossier fixtureStatuts;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComFolderItemRepository folderItemRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private StrFolderStatusHistoryRepository statusHistoryRepository;
    @Autowired private StrHistoryRepository strHistoryRepository;

    private CfgCompany societe;
    private CfgTranche tranche;
    private StkVocation vocation;
    private StkItem lot;
    private StkItem terrasse;
    private ComPaymentMethod modePaie;
    private AdmUser agent;

    @BeforeEach
    void setUp() {
        societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-CRE-OMRAN");
        societe.setDescription("Société création");
        societe.setIsActive(true);
        companyRepository.save(societe);
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PCO");
        projet.setCfgCompany(societe);
        projet = projectRepository.save(projet);
        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-CRE-OMRAN");
        tranche.setCfgProject(projet);
        tranche.setAdvancePercent(new BigDecimal("0.3"));
        tranche.setSyndicAmount(new BigDecimal("700"));
        tranche.setSyndicPeriod(24);
        tranche = trancheRepository.save(tranche);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CRE-OMRAN");
        vocationRepository.save(vocation);
        categorie("AUTO-FINANCEMENT", false, false, false);
        categorie("AVANCE", true, false, false);
        categorie("FRAIS SYNDIC", false, true, true);

        lot = article("LOT-CRE-OMRAN", "63", "819000");
        lot.setComSaleUnitCostHT(new BigDecimal("10833.333333"));
        lot.setComSalePriceHT(new BigDecimal("682500"));
        lot = itemRepository.save(lot);
        terrasse = article("TER-CRE-OMRAN", "12", "50000");
        terrasse.setSalePriceHT(new BigDecimal("41666.666667"));
        terrasse = itemRepository.save(terrasse);

        modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-CRE-OMRAN");
        paymentMethodRepository.save(modePaie);

        agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.cre." + UUID.randomUUID());
        agent.setDescription(AUTEUR);
        agent.setDisabled(false);
        agent = userRepository.save(agent);
    }

    private void categorie(String code, boolean avance, boolean frais, boolean exclue) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setStkVocation(vocation);
        c.setIsCredit(true);
        c.setIsSaleAdvance(avance);
        c.setIsSaleAutoFinancment("AUTO-FINANCEMENT".equals(code));
        c.setIsSaleExpense(frais);
        c.setExcludeFromPrice(exclue);
        c.setVatRatio(new BigDecimal("0.2"));
        categoryRepository.save(c);
    }

    private StkItem article(String code, String surface, String prix) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-" + code);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-" + code);
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemType(type);
        item.setStkItemCategory(categorie);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        item.setStkHierarchy(tranche);
        item.setArea(new BigDecimal(surface));
        item.setVat(new BigDecimal("0.2"));
        item.setComSalePrice(new BigDecimal(prix));
        return itemRepository.save(item);
    }

    private ComProspection prospection(String codeTypeClient) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-CRE-" + UUID.randomUUID());
        prospect.setDescription("Acheteur");
        if (codeTypeClient != null) {
            prospect.setCfgTierType(tierTypeRepository.findByCode(codeTypeClient).orElseGet(() -> {
                CfgTierType t = new CfgTierType();
                t.setId(UUID.randomUUID());
                t.setCode(codeTypeClient);
                return tierTypeRepository.save(t);
            }));
        }
        prospect = tierRepository.save(prospect);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-CRE-" + UUID.randomUUID());
        p.setCfgProspect(prospect);
        return prospectionRepository.save(p);
    }

    private ComFolder creer(ComProspection prospection, String prix, StkItem... articles) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (StkItem a : articles) {
            ids.append(ids.isEmpty() ? "" : ",").append('"').append(a.getId()).append('"');
        }
        String champPrix = prix != null ? ",\"prix\":" + prix : "";
        String json = mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionId\":\"%s\",\"articleIds\":[%s],\"modePaieId\":\"%s\",\"cin\":\"CIN-CRE\"%s}"
                                .formatted(prospection.getId(), ids, modePaie.getId(), champPrix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(UUID.fromString(JsonPath.read(json, "$.id"))).orElseThrow();
    }

    @Test
    void creation_ecritLesColonnesDuDossierCommeOmran() throws Exception {
        StrStatus reservation = fixtureStatuts.reservation();
        reservation.setDescription("Réservation");

        ComFolder f = creer(prospection(null), "869000", lot, terrasse);

        LocalDate jour = LocalDate.now(clock);
        assertAll(
                () -> assertThat(f.getStrEntityId()).isEqualTo(ReferencesOmran.ENTITE_DOSSIER),
                () -> assertThat(f.getStrStatus().getCode()).isEqualTo(StatutDossier.RESERVATION.getCode()),
                () -> assertThat(f.getDocDate()).isEqualTo(jour.atStartOfDay()),
                () -> assertThat(f.getStatusValidityDate())
                        .isEqualTo(jour.atStartOfDay().plusDays(FixtureStatutsDossier.DELAI_RESERVATION)),
                () -> assertThat(f.getPartPercent()).isEqualByComparingTo("1"),
                () -> assertThat(f.getIsOldCustomer()).isFalse(),
                () -> assertThat(f.getDescription())
                        .isEqualTo(f.getCode() + " du " + jour.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                () -> assertThat(f.getArea()).isEqualByComparingTo("63"),
                () -> assertThat(f.getGlobalArea()).isEqualByComparingTo("75"),
                () -> assertThat(f.getUnitCostHT()).isEqualByComparingTo("10833.333333"),
                () -> assertThat(f.getUnitCost()).isEqualByComparingTo("13000"),
                () -> assertThat(f.getPrice()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getAgreementPrice()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getAgreementAmount()).isEqualByComparingTo("608300"),
                () -> assertThat(f.getDeliveryRatio()).isEqualByComparingTo("0.001"),
                () -> assertThat(f.getDeliveryMaxNumber()).isEqualTo(180),
                () -> assertThat(f.getProbabilityOfFinalisation()).isEqualByComparingTo("1"),
                () -> assertThat(f.getModificationsAmount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getModificationsAmountHT()).isEqualByComparingTo("0"),
                () -> assertThat(f.getDeliveryNumber()).isZero(),
                () -> assertThat(f.getDeliveryAmount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getRomInterest()).isEqualByComparingTo("0"),
                () -> assertThat(f.getCommissionRate()).isEqualByComparingTo("0"),
                () -> assertThat(f.getIsAccounted()).isFalse(),
                () -> assertThat(f.getIsPenaltyDisabled()).isFalse(),
                () -> assertThat(f.getPriceWithModification()).isFalse(),
                () -> assertThat(f.getIsConfirmedAgreementDate()).isFalse(),
                () -> assertThat(f.getIsWithGiftChoiceLater()).isFalse(),
                () -> assertThat(f.getNotes()).isEmpty(),
                () -> assertThat(f.getCancelDescription()).isEmpty(),
                () -> assertThat(f.getPenalityDescription()).isEmpty(),
                () -> assertThat(f.getCreditAgency()).isEmpty(),
                () -> assertThat(f.getFinalTitleNumber()).isEmpty(),
                () -> assertThat(f.getUserCreate()).isEqualTo(AUTEUR),
                () -> assertThat(f.getUserUpdate()).isEqualTo(AUTEUR),
                () -> assertThat(f.getUserCreateDateTime()).isNotNull(),
                () -> assertThat(f.getUserUpdateDateTime()).isEqualTo(f.getUserCreateDateTime()));
    }

    @Test
    void creation_schemaTexteAuFormatDOmran() throws Exception {
        fixtureStatuts.reservation();

        ComFolder f = creer(prospection(null), "869000", lot, terrasse);

        String date = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertThat(f.getComDocumentDetail())
                .startsWith("Schèma de financement :\r\nDate :" + date)
                .contains("   Montant :       608 300.00 DH   Réglé :             0.00 DH   Reste :       608 300.00 DH"
                        + "   Exclure du prix contractuel:Non   Catégorie : AUTO-FINANCEMENT\r\n")
                .contains("Exclure du prix contractuel:Oui   Catégorie : FRAIS SYNDIC\r\n");
    }

    /** Aucune colonne qu'OMRAN remplit toujours n'est laissée à NULL (§2.1). */
    @Test
    void colonnesNonNullesCommeOmran() throws Exception {
        fixtureStatuts.reservation();
        ComFolder f = creer(prospection(null), "869000", lot, terrasse);

        Object[] ligne = (Object[]) entityManager.createNativeQuery("""
                        SELECT StrEntityId, StrStatusId, PartPercent, IsOldCustomer, Description, DocDate, Area,
                               GlobalArea, UnitCostHT, UnitCost, VATRatio, ItemPrice, ItemPriceHT, UnderItemsPrice,
                               UnderItemsPriceHT, PriceBeforeDiscount, PriceBeforeDiscountHT, Discount, DiscountAmount,
                               PriceAfterDiscount, PriceAfterDiscountHT, ItemPriceAfter, ItemPriceAfterHT,
                               UnderItemsPriceAfter, UnderItemsPriceAfterHT, Price, PriceHT, TotalToPay,
                               AgreementPrice, AgreementAmount, ComDocumentDetail, DeliveryRatio, DeliveryMaxNumber,
                               ProbabilityOfFinalisation, ModificationsAmount, ModificationsAmountHT, DeliveryNumber,
                               DeliveryAmount, RomInterest, CommissionRate, IsAccounted, IsPenaltyDisabled,
                               PriceWithModification, IsConfirmedAgreementDate, IsWithGiftChoiceLater, Notes,
                               CancelDescription, PenalityDescription, CreditAgency, FinalTitleNumber,
                               UserCreate, UserCreateDateTime, UserUpdate, UserUpdateDateTime
                        FROM ComFolder WHERE PKey = :id""")
                .setParameter("id", f.getId())
                .getSingleResult();

        assertThat(ligne).hasSize(54).doesNotContainNull();
    }

    @Test
    void creation_sousArticle_ligneComFolderItemEtArticleRattache() throws Exception {
        fixtureStatuts.reservation();

        ComFolder f = creer(prospection(null), "869000", lot, terrasse);

        List<ComFolderItem> lignes = folderItemRepository.findByComFolderId(f.getId());
        StkItem terrasseApres = itemRepository.findById(terrasse.getId()).orElseThrow();
        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.getStkItem().getId()).isEqualTo(terrasse.getId()),
                () -> assertThat(l.getCode()).isEqualTo("TER-CRE-OMRAN"),
                () -> assertThat(l.getArea()).isEqualByComparingTo("12"),
                () -> assertThat(l.getPrice()).isEqualByComparingTo("50000"),
                () -> assertThat(l.getIsUnderItem()).isTrue(),
                () -> assertThat(l.getParentId()).isEqualTo(lot.getId())));
        assertAll(
                () -> assertThat(terrasseApres.getComFolderId()).isEqualTo(f.getId()),
                () -> assertThat(terrasseApres.getParent().getId()).isEqualTo(lot.getId()),
                () -> assertThat(terrasseApres.getIsUnderItem()).isTrue(),
                () -> assertThat(itemRepository.findById(lot.getId()).orElseThrow().getComFolderId()).isEqualTo(f.getId()));
    }

    /**
     * Revue : OMRAN {@code SetStkItemId} (Sale.cs:4298) prend tous les sous-articles du lot ({@code ParentId}),
     * même non cochés dans la requête ; spec interop §3 « StkItem.ParentId = l'article ».
     */
    @Test
    void creation_sousArticleEnfantDuLot_rattacheSansEtreDansLaRequete() throws Exception {
        fixtureStatuts.reservation();
        terrasse.setParent(lot);
        terrasse.setIsUnderItem(true);
        terrasse.setAlwaysAffected(true);
        itemRepository.save(terrasse);

        ComFolder f = creer(prospection(null), "869000", lot);

        assertAll(
                () -> assertThat(itemRepository.findById(terrasse.getId()).orElseThrow().getComFolderId())
                        .isEqualTo(f.getId()),
                () -> assertThat(folderItemRepository.findByComFolderId(f.getId())).singleElement()
                        .satisfies(l -> assertThat(l.getStkItem().getId()).isEqualTo(terrasse.getId())),
                () -> assertThat(f.getGlobalArea()).isEqualByComparingTo("75"));
    }

    /** Sale.cs : UnderItemsPrice = somme des sous-articles du lot, saisis ou non. */
    @Test
    void creation_sousArticleEnfantDuLot_entreDansLePrixDesSousArticles() throws Exception {
        fixtureStatuts.reservation();
        terrasseEnfantDuLot();

        ComFolder f = creer(prospection(null), null, lot);

        assertAll(
                () -> assertThat(f.getItemPrice()).isEqualByComparingTo("819000"),
                () -> assertThat(f.getUnderItemsPrice()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("0"));
    }

    /**
     * Revue navigateur (ARS0604 + JRD) : le prix du lot d'OMRAN inclut déjà ses sous-articles ; l'aperçu au prix
     * de référence (lot + enfant) ne doit ni doubler l'enfant ni inventer de remise.
     */
    @Test
    void simulation_lotAvecEnfant_prixDeReference_sansDoubleCompteNiRemise() throws Exception {
        terrasseEnfantDuLot();

        mockMvc.perform(post(DOSSIERS + "/simulation-financement").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"%s\"],\"prix\":869000}".formatted(lot.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixAvantRemise").value(869000.0))
                .andExpect(jsonPath("$.montantRemise").value(0.0))
                .andExpect(jsonPath("$.prixApresRemise").value(869000.0))
                .andExpect(jsonPath("$.annexesAutomatiques[0].prix").value(50000.0));
    }

    /** Forme réelle d'OMRAN : ComSalePrice du lot = son prix seul + celui de ses sous-articles. */
    private void terrasseEnfantDuLot() {
        terrasse.setParent(lot);
        terrasse.setIsUnderItem(true);
        terrasse.setComSalePriceHT(new BigDecimal("41666.666667"));
        itemRepository.save(terrasse);
        lot.setComSalePrice(new BigDecimal("869000"));
        lot.setComSalePriceHT(new BigDecimal("724166.666667"));
        itemRepository.save(lot);
    }

    /** L'aperçu compte le sous-article libre du lot comme la création : mêmes montants. */
    @Test
    void simulation_sousArticleEnfantDuLot_egaleLeDossierCree() throws Exception {
        fixtureStatuts.reservation();
        terrasseEnfantDuLot();
        String corps = "{\"articleIds\":[\"%s\"],\"prix\":869000,\"remisePercent\":5}".formatted(lot.getId());

        String apercu = mockMvc.perform(post(DOSSIERS + "/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annexesAutomatiques[0].code").value("TER-CRE-OMRAN"))
                .andExpect(jsonPath("$.annexesAutomatiques[0].prix").value(50000))
                .andReturn().getResponse().getContentAsString();
        String json = mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionId\":\"%s\",\"articleIds\":[\"%s\"],\"modePaieId\":\"%s\",\"cin\":\"CIN-SIM\","
                                .formatted(prospection(null).getId(), lot.getId(), modePaie.getId())
                                + "\"prix\":869000,\"remisePercent\":5}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        ComFolder f = folderRepository.findById(UUID.fromString(JsonPath.read(json, "$.id"))).orElseThrow();

        assertAll(
                () -> assertThat(new BigDecimal(JsonPath.read(apercu, "$.prixAvantRemise").toString()))
                        .isEqualByComparingTo(f.getPriceBeforeDiscount()),
                () -> assertThat(new BigDecimal(JsonPath.read(apercu, "$.prixApresRemise").toString()))
                        .isEqualByComparingTo(f.getPriceAfterDiscount()),
                () -> assertThat(f.getUnderItemsPrice()).isEqualByComparingTo("50000"));
    }

    @Test
    void creation_historiqueAvecAuteurClientArticleEtAffectation() throws Exception {
        StrStatus reservation = fixtureStatuts.reservation();
        reservation.setDescription("Réservation");
        ComProspection prospection = prospection(null);

        ComFolder f = creer(prospection, "869000", lot, terrasse);

        List<StrFolderStatusHistory> lignes = statusHistoryRepository.findByComFolderIdOrderByDocDateAsc(f.getId());
        assertAll(
                () -> assertThat(lignes).hasSize(2).allSatisfy(l -> assertAll(
                        () -> assertThat(l.getAdmUserId()).isEqualTo(agent.getId()),
                        () -> assertThat(l.getCfgTier().getId()).isEqualTo(prospection.getCfgProspect().getId()),
                        () -> assertThat(l.getStrFolderStatus().getCode()).isEqualTo(StatutDossier.RESERVATION.getCode()))),
                () -> assertThat(lignes).filteredOn(l -> "AFFECTATION".equals(l.getDescription()))
                        .singleElement().satisfies(l -> assertThat(l.getStkItem().getId()).isEqualTo(terrasse.getId())),
                () -> assertThat(lignes).filteredOn(l -> "".equals(l.getDescription()))
                        .singleElement().satisfies(l -> assertThat(l.getStkItem().getId()).isEqualTo(lot.getId())));

        List<StrHistory> generiques = strHistoryRepository.findByObjectIdInOrderByDocDateDesc(List.of(f.getId()));
        assertThat(generiques).filteredOn(h -> h.getStrStatusId() != null).singleElement()
                .satisfies(h -> assertAll(
                        () -> assertThat(h.getDescription()).isEqualTo("Création"),
                        () -> assertThat(h.getStrStatusDescription()).isEqualTo("Réservation")));
    }

    @Test
    void creation_echeancesDuSchemaRempliesCommeOmran() throws Exception {
        fixtureStatuts.reservation();

        ComFolder f = creer(prospection(null), "869000", lot, terrasse);

        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(f.getId());
        assertThat(lignes).hasSize(3).allSatisfy(d -> assertAll(
                () -> assertThat(d.getAmountHT()).isNotNull(),
                () -> assertThat(d.getCfgCompany().getId()).isEqualTo(societe.getId()),
                () -> assertThat(d.getAdmModuleId()).isEqualTo(ReferencesOmran.MODULE_COMMERCIAL),
                () -> assertThat(d.getVATRatio()).isEqualByComparingTo("0.2"),
                () -> assertThat(d.getAmountTTC()).isEqualByComparingTo(d.getAmount()),
                () -> assertThat(d.getDiscountRatio()).isEqualByComparingTo("0"),
                () -> assertThat(d.getDocDate()).isEqualTo(f.getDocDate()),
                () -> assertThat(d.getUserCreate()).isEqualTo(AUTEUR),
                () -> assertThat(d.getUserUpdate()).isEqualTo(AUTEUR)));
        assertThat(lignes).filteredOn(d -> "FRAIS SYNDIC".equals(d.getComSettlementCategory().getCode()))
                .singleElement().satisfies(d -> assertAll(
                        () -> assertThat(d.getAmountHT()).isEqualByComparingTo("14000"),
                        () -> assertThat(d.getExcludeFromPrice()).isTrue()));
    }

    @Test
    void creation_clientDejaClient_estAncienClient() throws Exception {
        fixtureStatuts.reservation();

        ComFolder f = creer(prospection("CLI"), "869000", lot);

        assertThat(f.getIsOldCustomer()).isTrue();
    }

    @Test
    void creation_sansPrix_montantsDepuisLePrixDeReference_sansSchema() throws Exception {
        fixtureStatuts.reservation();

        ComFolder f = creer(prospection(null), null, lot, terrasse);

        assertAll(
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getPrice()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getTotalToPay()).isEqualByComparingTo("869000"),
                () -> assertThat(f.getAgreementPrice()).isEqualByComparingTo("0"),
                () -> assertThat(f.getAgreementAmount()).isEqualByComparingTo("869000"),
                () -> assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(f.getId())).isEmpty());
    }

    /** OMRAN refuse un dossier sans statut : sans RESERVATION configuré, 500 et rien n'est écrit. */
    @Test
    void creation_sansStatutReservation_500_etRienNEstEcrit() throws Exception {
        ComProspection prospection = prospection(null);

        mockMvc.perform(post(DOSSIERS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prospectionId\":\"%s\",\"articleIds\":[\"%s\"],\"modePaieId\":\"%s\",\"cin\":\"CIN-500\"}"
                                .formatted(prospection.getId(), lot.getId(), modePaie.getId())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("RESERVATION")));

        entityManager.flush();
        entityManager.clear();
        assertAll(
                () -> assertThat(itemRepository.findById(lot.getId()).orElseThrow().getComFolderId()).isNull(),
                () -> assertThat(prospectionRepository.findById(prospection.getId()).orElseThrow().getIsClosed())
                        .isNotEqualTo(Boolean.TRUE),
                () -> assertThat(tierRepository.findById(prospection.getCfgProspect().getId()).orElseThrow()
                        .getIDNumber()).isNotEqualTo("CIN-500"));
    }
}
