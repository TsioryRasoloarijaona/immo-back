package com.tsix.immocrm.controller.hotline;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FIX-tracabilite-historique-hotline.md §1 à §5 : auteurs, commercial du tiers, historique et interactions. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TracabiliteHistoriqueHotlineTest {

    private static final String AUTEUR = "Agent Traçabilité";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComSettlementCategoryRepository settlementCategoryRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private StrStatusRepository strStatusRepository;
    @Autowired private StrFolderStatusHistoryRepository statusHistoryRepository;

    private AdmUser moi;
    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;

    @BeforeEach
    void setUp() {
        moi = newUser("trace.moi");
        moi.setDescription(AUTEUR);
        userRepository.save(moi);

        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-TRACE-" + suffixe());
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-TRACE-" + suffixe());
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TRACE-" + suffixe());
        stkVocationRepository.save(vocation);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ================================================================= fixtures

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String telephoneUnique() {
        return "06" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private AdmUser newUser(String prefixe) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(prefixe + "." + suffixe());
        user.setDisabled(false);
        return userRepository.save(user);
    }

    private void affecter(AdmUser user, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(user);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    private RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private CfgProject newProjet(String code) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code + "-" + suffixe());
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-" + projet.getCode());
        tranche.setCfgProject(projet);
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700.000000"));
        tranche.setSyndicPeriod(24);
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String code, String telephone) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code + "-" + suffixe());
        tier.setPhone(telephone);
        return tierRepository.save(tier);
    }

    private StkItem newArticle(String code, CfgTranche tranche) {
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code + "-" + suffixe());
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(article);
    }

    private ComProspection newProspection(String code, CfgTranche tranche, CfgTier prospect) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code + "-" + suffixe());
        prospection.setCfgProspect(prospect);
        prospection.setStkHierarchy(tranche);
        prospection.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(prospection);
    }

    private ComFolder newDossier(String code, CfgTranche tranche, CfgTier client, StrStatus statut) {
        StkItem article = newArticle("LOT-" + code, tranche);
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code + "-" + suffixe());
        folder.setStkVocation(vocation);
        folder.setDocDate(LocalDateTime.now());
        folder.setCfgCustomer(client);
        folder.setStkItem(article);
        folder.setStrStatus(statut);
        folder = folderRepository.save(folder);
        article.setComFolderId(folder.getId());
        stkItemRepository.save(article);
        return folder;
    }

    private StrStatus statut(StatutDossier statut) {
        StrStatus s = strStatusRepository.findByStrConfigEntityIdAndCode(StatutDossier.CONFIG_ENTITY_ID, statut.getCode());
        if (s != null) {
            return s;
        }
        s = new StrStatus();
        s.setId(UUID.randomUUID());
        s.setCode(statut.getCode());
        s.setDescription(statut.getCode());
        s.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        return strStatusRepository.save(s);
    }

    private StrFolderStatusHistory newLigneStatut(ComFolder dossier, CfgTier tier, StrStatus statut,
                                                  UUID admUserId, LocalDateTime date, String note) {
        StrFolderStatusHistory h = new StrFolderStatusHistory();
        h.setId(UUID.randomUUID());
        h.setComFolder(dossier);
        h.setCfgTier(tier);
        h.setStrFolderStatus(statut);
        h.setAdmUserId(admUserId);
        h.setDocDate(date);
        h.setDescription(note);
        return statusHistoryRepository.save(h);
    }

    private ComActionMessage newInteraction(CfgTier tier, ComProspection prospection, String description) {
        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setCfgTier(tier);
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription(description);
        return actionMessageRepository.save(message);
    }

    private void newCategorie(String code, Boolean excludeFromPrice) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkVocation(vocation);
        categorie.setExcludeFromPrice(excludeFromPrice);
        settlementCategoryRepository.save(categorie);
    }

    // ================================================================= §1 et §2 : auteurs du dossier

    @Test
    void creationDeDossier_signeLeDossierLesEcheancesEtLeStatutInitial() throws Exception {
        statut(StatutDossier.RESERVATION);
        newCategorie("AUTO-FINANCEMENT", false);
        newCategorie("AVANCE", false);
        newCategorie("FRAIS SYNDIC", true);
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-CREA"));
        StkItem article = newArticle("LOT-TRACE-CREA", tranche);
        ComProspection prospection = newProspection("PSN-TRACE-CREA", tranche, newTier("CLI-TRACE-CREA", null));
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-TRACE-" + suffixe());
        paymentMethodRepository.save(modePaie);

        String body = """
                {"prospectionId":"%s","articleIds":["%s"],"prix":1548000,"modePaieId":"%s","cin":"CIN-TRACE"}
                """.formatted(prospection.getId(), article.getId(), modePaie.getId());
        String cree = mockMvc.perform(post("/api/dossiers").with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andExpect(jsonPath("$.creeLe").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        UUID dossierId = UUID.fromString(JsonPath.read(cree, "$.id"));

        entityManager.flush();
        entityManager.clear();
        ComFolder dossier = folderRepository.findById(dossierId).orElseThrow();
        assertThat(dossier.getUserCreate()).isEqualTo(AUTEUR);
        assertThat(dossier.getUserCreateDateTime()).isNotNull();
        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        assertThat(echeances).hasSize(3).allSatisfy(d -> {
            assertThat(d.getUserCreate()).isEqualTo(AUTEUR);
            assertThat(d.getUserCreateDateTime()).isNotNull();
        });

        String historique = mockMvc.perform(get("/api/dossiers/{id}/historique", dossierId).with(connecte(moi)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JSONArray statuts = JsonPath.read(historique, "$.content[?(@.type=='STATUT')].utilisateur");
        JSONArray echeancesHisto = JsonPath.read(historique, "$.content[?(@.type=='ECHEANCE')].utilisateur");
        assertThat(statuts).containsExactly(AUTEUR);
        assertThat(echeancesHisto).hasSize(3).containsOnly(AUTEUR);
    }

    @Test
    void changementDeStatut_signeLaModificationEtLEvenement() throws Exception {
        StrStatus reservation = statut(StatutDossier.RESERVATION);
        statut(StatutDossier.CONCRETISATION);
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-STAT"));
        ComFolder dossier = newDossier("DOS-TRACE-STAT", tranche, newTier("CLI-TRACE-STAT", null), reservation);
        assertThat(dossier.getUserUpdate()).isNull();

        mockMvc.perform(patch("/api/dossiers/{id}/statut", dossier.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CONCRETISATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR));

        entityManager.flush();
        entityManager.clear();
        ComFolder relu = folderRepository.findById(dossier.getId()).orElseThrow();
        assertThat(relu.getUserUpdate()).isEqualTo(AUTEUR);
        assertThat(relu.getUserUpdateDateTime()).isNotNull();

        mockMvc.perform(get("/api/dossiers/{id}/historique", dossier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("STATUT"))
                .andExpect(jsonPath("$.content[0].utilisateur").value(AUTEUR));
    }

    @Test
    void echeanceAjoutee_porteSonAuteurDansLHistorique() throws Exception {
        ComFolder dossier = newDossier("DOS-TRACE-ECH", newTranche(newProjet("PRJ-TRACE-ECH")),
                newTier("CLI-TRACE-ECH", null), null);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier", dossier.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dossiers/{id}/historique", dossier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='ECHEANCE')].utilisateur").value(AUTEUR));
    }

    @Test
    void ligneDeStatutLegacySansAuteur_resteSansAuteur() throws Exception {
        ComFolder dossier = newDossier("DOS-TRACE-LEG", newTranche(newProjet("PRJ-TRACE-LEG")),
                newTier("CLI-TRACE-LEG", null), null);
        newLigneStatut(dossier, dossier.getCfgCustomer(), statut(StatutDossier.RESERVATION), null,
                LocalDateTime.of(2020, 1, 1, 10, 0), null);

        mockMvc.perform(get("/api/dossiers/{id}/historique", dossier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("STATUT"))
                .andExpect(jsonPath("$.content[0].utilisateur").doesNotExist());
    }

    @Test
    void creationDeProspect_signeLaFiche() throws Exception {
        String cree = mockMvc.perform(post("/api/prospects").with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Trace" + suffixe() + "\",\"ignorerDoublons\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();
        CfgTier tier = tierRepository.findById(UUID.fromString(JsonPath.read(cree, "$.id"))).orElseThrow();
        assertThat(tier.getUserCreate()).isEqualTo(AUTEUR);
        assertThat(tier.getUserCreateDateTime()).isNotNull();
    }

    // ================================================================= §3 : commercial du tiers

    @Test
    void transformer_prospectSansCommercial_recoitCeluiDeLaProspection() throws Exception {
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-TRF"));
        CfgTier prospect = newTier("CLI-TRACE-TRF", null);
        ComProspection pre = newProspection("PSN-TRACE-TRF", null, prospect);
        CfgTier commercial = newTier("COMM-TRACE-TRF", null);

        mockMvc.perform(post("/api/prospections/{id}/transformer", pre.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(tierRepository.findById(prospect.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(commercial.getId());
    }

    @Test
    void transformer_prospectAvecCommercial_nEstPasEcrase() throws Exception {
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-TRF2"));
        CfgTier referent = newTier("COMM-TRACE-REF", null);
        CfgTier prospect = newTier("CLI-TRACE-TRF2", null);
        prospect.setCfgCommercial(referent);
        tierRepository.save(prospect);
        ComProspection pre = newProspection("PSN-TRACE-TRF2", null, prospect);
        CfgTier autre = newTier("COMM-TRACE-AUTRE", null);

        mockMvc.perform(post("/api/prospections/{id}/transformer", pre.getId()).with(connecte(moi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialId\":\"" + autre.getId() + "\",\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(tierRepository.findById(prospect.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(referent.getId());
    }

    // ================================================================= §4 : historique du tiers

    @Test
    void historiqueDuTiers_listeLesChangementsDeStatutDeSesDossiers() throws Exception {
        CfgTier client = newTier("CLI-TRACE-HIS", null);
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-HIS"));
        ComFolder dossier = newDossier("DOS-TRACE-HIS", tranche, client, null);
        StrFolderStatusHistory ancien = newLigneStatut(dossier, client, statut(StatutDossier.RESERVATION),
                null, LocalDateTime.of(2025, 1, 1, 9, 0), null);
        StrFolderStatusHistory recent = newLigneStatut(dossier, client, statut(StatutDossier.CONCRETISATION),
                moi.getId(), LocalDateTime.of(2025, 2, 1, 9, 0), "Avance reçue");

        mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(recent.getId().toString()))
                .andExpect(jsonPath("$.content[0].utilisateur").value(AUTEUR))
                .andExpect(jsonPath("$.content[0].statutDescription").value("CONCRETISATION"))
                .andExpect(jsonPath("$.content[0].dossierId").value(dossier.getId().toString()))
                .andExpect(jsonPath("$.content[0].dossierCode").value(dossier.getCode()))
                .andExpect(jsonPath("$.content[0].articleCode").value(dossier.getStkItem().getCode()))
                .andExpect(jsonPath("$.content[0].description").value("Avance reçue"))
                .andExpect(jsonPath("$.content[1].id").value(ancien.getId().toString()))
                .andExpect(jsonPath("$.content[1].utilisateur").doesNotExist());
    }

    @Test
    void historiqueDuTiers_sansDossier_estVide() throws Exception {
        CfgTier tier = newTier("CLI-TRACE-VIDE", null);

        mockMvc.perform(get("/api/prospects/{id}/historique", tier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void historiqueDuTiers_inconnu_renvoie404() throws Exception {
        mockMvc.perform(get("/api/prospects/{id}/historique", UUID.randomUUID()).with(connecte(moi)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historiqueDuTiers_dossierHorsPerimetre_estMasque() throws Exception {
        CfgProject projetA = newProjet("PRJ-TRACE-PA");
        CfgProject projetB = newProjet("PRJ-TRACE-PB");
        AdmUser restreint = newUser("trace.restreint");
        affecter(restreint, projetA);
        CfgTier client = newTier("CLI-TRACE-PERIM", null);
        ComFolder surA = newDossier("DOS-TRACE-PA", newTranche(projetA), client, null);
        ComFolder surB = newDossier("DOS-TRACE-PB", newTranche(projetB), client, null);
        StrFolderStatusHistory visible = newLigneStatut(surA, client, statut(StatutDossier.RESERVATION),
                null, LocalDateTime.of(2025, 3, 1, 9, 0), null);
        StrFolderStatusHistory cachee = newLigneStatut(surB, client, statut(StatutDossier.RESERVATION),
                null, LocalDateTime.of(2025, 3, 2, 9, 0), null);

        mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).with(connecte(restreint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + visible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + cachee.getId() + "')]").doesNotExist());
    }

    // ================================================================= §5 : interactions de la Hotline

    @Test
    void hotline_interactionSaisieAvantCreerProspect_apparaitUneSeuleFois() throws Exception {
        String telephone = telephoneUnique();
        CfgTier tier = newTier("CLI-TRACE-HOT", telephone);
        ComProspection prospection = newProspection("PSN-TRACE-HOT", null, tier);
        ComActionMessage avantFiche = newInteraction(null, prospection, "appel pré-prospection");
        ComActionMessage surLesDeux = newInteraction(tier, prospection, "appel après création");
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-HOT"));
        ComFolder dossier = newDossier("DOS-TRACE-HOT", tranche, tier, null);
        newLigneStatut(dossier, tier, statut(StatutDossier.RESERVATION), moi.getId(), LocalDateTime.now(), null);

        String json = mockMvc.perform(get("/api/hotline/recherche").param("telephone", telephone).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches.length()").value(1))
                .andExpect(jsonPath("$.fiches[0].historique[0].utilisateur").value(AUTEUR))
                .andReturn().getResponse().getContentAsString();

        JSONArray ids = JsonPath.read(json, "$.fiches[0].interactions[*].id");
        assertThat(ids).containsExactlyInAnyOrder(avantFiche.getId().toString(), surLesDeux.getId().toString());
    }

    @Test
    void hotline_interactionDeProspectionHorsPerimetre_resteInvisible() throws Exception {
        CfgProject projetA = newProjet("PRJ-TRACE-HA");
        CfgProject projetB = newProjet("PRJ-TRACE-HB");
        AdmUser restreint = newUser("trace.hotline");
        affecter(restreint, projetA);
        String telephone = telephoneUnique();
        CfgTier tier = newTier("CLI-TRACE-HB", telephone);
        ComActionMessage surB = newInteraction(null, newProspection("PSN-TRACE-HB", newTranche(projetB), tier), "B");
        ComActionMessage surA = newInteraction(null, newProspection("PSN-TRACE-HA", newTranche(projetA), tier), "A");

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", telephone).with(connecte(restreint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surA.getId() + "')]").exists())
                .andExpect(jsonPath("$.fiches[0].interactions[?(@.id=='" + surB.getId() + "')]").doesNotExist());
    }

    // ================================================================= revue : cas limites

    private void newStrHistory(UUID objectId, LocalDateTime date, String auteur) {
        com.tsix.immocrm.entities.str.StrHistory s = new com.tsix.immocrm.entities.str.StrHistory();
        s.setId(UUID.randomUUID());
        s.setObjectId(objectId);
        s.setDocDate(date);
        s.setAdmUserDescription(auteur);
        entityManager.persist(s);
    }

    @Test
    void revue_auteurStatut_repliStrHistory_neLitQueLeMemeDossier() throws Exception {
        LocalDateTime date = LocalDateTime.of(2021, 5, 4, 10, 0);
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-REPLI"));
        CfgTier client = newTier("CLI-TRACE-REPLI", null);
        ComFolder sansJumelle = newDossier("DOS-TRACE-R1", tranche, client, null);
        ComFolder avecJumelle = newDossier("DOS-TRACE-R2", tranche, client, null);
        newLigneStatut(sansJumelle, client, statut(StatutDossier.RESERVATION), null, date, null);
        // AdmUserId null (FK vers AdmUser en CI) : force le repli StrHistory.
        newLigneStatut(avecJumelle, client, statut(StatutDossier.RESERVATION), null, date, null);
        // Même date, mais la ligne StrHistory vise l'autre dossier : elle ne doit pas signer sansJumelle.
        newStrHistory(avecJumelle.getId(), date, "Compte Legacy");
        entityManager.flush();

        mockMvc.perform(get("/api/dossiers/{id}/historique", avecJumelle.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='STATUT')].utilisateur").value("Compte Legacy"));
        mockMvc.perform(get("/api/dossiers/{id}/historique", sansJumelle.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("STATUT"))
                .andExpect(jsonPath("$.content[0].utilisateur").doesNotExist());
    }

    // ================================================================= FIX-historique-tiers-auteur.md

    @Test
    void historiqueDuTiers_repliStrHistory_signeCommeLeDossierEtPasLAutreDossier() throws Exception {
        LocalDateTime date = LocalDateTime.of(2022, 6, 7, 11, 30);
        String telephone = telephoneUnique();
        CfgTier client = newTier("CLI-TRACE-TAUT", telephone);
        CfgTranche tranche = newTranche(newProjet("PRJ-TRACE-TAUT"));
        ComFolder avecJumelle = newDossier("DOS-TRACE-TA1", tranche, client, null);
        ComFolder sansJumelle = newDossier("DOS-TRACE-TA2", tranche, client, null);
        newLigneStatut(avecJumelle, client, statut(StatutDossier.RESERVATION), null, date, null);
        newLigneStatut(sansJumelle, client, statut(StatutDossier.RESERVATION), null, date, null);
        newStrHistory(avecJumelle.getId(), date, "Compte Legacy");
        entityManager.flush();

        String jsonPathAvec = "$.content[?(@.dossierId=='" + avecJumelle.getId() + "')].utilisateur";
        String jsonPathSans = "$.content[?(@.dossierId=='" + sansJumelle.getId() + "')]";
        String tiers = mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();
        JSONArray auteurTiers = JsonPath.read(tiers, jsonPathAvec);
        assertThat(auteurTiers).containsExactly("Compte Legacy");
        JSONArray ligneSans = JsonPath.read(tiers, jsonPathSans);
        assertThat(ligneSans).hasSize(1);
        JSONArray auteurSans = JsonPath.read(tiers, jsonPathSans + ".utilisateur");
        assertThat(auteurSans).hasSize(1).containsOnlyNulls();

        String dossier = mockMvc.perform(get("/api/dossiers/{id}/historique", avecJumelle.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JSONArray auteurDossier = JsonPath.read(dossier, "$.content[?(@.type=='STATUT')].utilisateur");
        assertThat(auteurDossier).containsExactlyElementsOf(auteurTiers);

        String hotline = mockMvc.perform(get("/api/hotline/recherche").param("telephone", telephone).with(connecte(moi)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JSONArray auteurHotline = JsonPath.read(hotline,
                "$.fiches[0].historique[?(@.dossierId=='" + avecJumelle.getId() + "')].utilisateur");
        assertThat(auteurHotline).containsExactly("Compte Legacy");
    }

    /** FEATURE-lot4 §3 : la ligne « Prospect transformé en client » est un événement, jamais une jumelle. */
    @Test
    void evenementSansStatut_neSigneJamaisLaLigneDeStatut_etApparaitSeul() throws Exception {
        LocalDateTime date = LocalDateTime.of(2022, 6, 9, 11, 30);
        CfgTier client = newTier("CLI-TRACE-EVT", null);
        ComFolder dossier = newDossier("DOS-TRACE-EVT", newTranche(newProjet("PRJ-TRACE-EVT")), client, null);
        newLigneStatut(dossier, client, statut(StatutDossier.CONCRETISATION), null, date, null);
        com.tsix.immocrm.entities.str.StrHistory evenement = new com.tsix.immocrm.entities.str.StrHistory();
        evenement.setId(UUID.randomUUID());
        evenement.setObjectId(dossier.getId());
        evenement.setDocDate(date);
        evenement.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        evenement.setDescription("Prospect transformé en client");
        evenement.setAdmUserDescription("Auteur Evenement");
        entityManager.persist(evenement);
        entityManager.flush();

        String json = mockMvc.perform(get("/api/dossiers/{id}/historique", dossier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();
        JSONArray auteurStatut = JsonPath.read(json, "$.content[?(@.description=='Statut changé vers CONCRETISATION')].utilisateur");
        assertThat(auteurStatut).hasSize(1).containsOnlyNulls();
        JSONArray auteurEvenement = JsonPath.read(json, "$.content[?(@.description=='Prospect transformé en client')].utilisateur");
        assertThat(auteurEvenement).containsExactly("Auteur Evenement");
    }

    @Test
    void historiqueDuTiers_compteAdmUser_primeSurLaJumelle() throws Exception {
        LocalDateTime date = LocalDateTime.of(2022, 6, 8, 11, 30);
        CfgTier client = newTier("CLI-TRACE-TAU2", null);
        ComFolder dossier = newDossier("DOS-TRACE-TAU2", newTranche(newProjet("PRJ-TRACE-TAU2")), client, null);
        newLigneStatut(dossier, client, statut(StatutDossier.RESERVATION), moi.getId(), date, null);
        newStrHistory(dossier.getId(), date, "Compte Legacy");
        entityManager.flush();

        mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].utilisateur").value(AUTEUR));
        mockMvc.perform(get("/api/dossiers/{id}/historique", dossier.getId()).with(connecte(moi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='STATUT')].utilisateur").value(AUTEUR));
    }

    @Test
    void revue_historiqueDuTiers_ligneSansDossier_resteVisiblePourUnRestreint() throws Exception {
        CfgProject projetA = newProjet("PRJ-TRACE-NUL");
        AdmUser restreint = newUser("trace.nul");
        affecter(restreint, projetA);
        CfgTier client = newTier("CLI-TRACE-NUL", null);
        StrFolderStatusHistory orpheline = newLigneStatut(null, client, statut(StatutDossier.RESERVATION),
                null, LocalDateTime.of(2019, 1, 1, 9, 0), null);

        mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).with(connecte(restreint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orpheline.getId().toString()))
                .andExpect(jsonPath("$.content[0].dossierId").doesNotExist());
    }

    @Test
    void revue_historiqueDuTiers_paginationRestreinte_compteJuste() throws Exception {
        CfgProject projetA = newProjet("PRJ-TRACE-PGA");
        CfgProject projetB = newProjet("PRJ-TRACE-PGB");
        AdmUser restreint = newUser("trace.page");
        affecter(restreint, projetA);
        CfgTier client = newTier("CLI-TRACE-PAGE", null);
        ComFolder surA = newDossier("DOS-TRACE-PGA", newTranche(projetA), client, null);
        ComFolder surB = newDossier("DOS-TRACE-PGB", newTranche(projetB), client, null);
        for (int i = 0; i < 3; i++) {
            newLigneStatut(surA, client, statut(StatutDossier.RESERVATION), null, LocalDateTime.of(2024, 1, 1 + i, 9, 0), null);
            newLigneStatut(surB, client, statut(StatutDossier.RESERVATION), null, LocalDateTime.of(2024, 2, 1 + i, 9, 0), null);
        }

        mockMvc.perform(get("/api/prospects/{id}/historique", client.getId()).param("size", "2").param("page", "1")
                        .with(connecte(restreint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].dossierId").value(surA.getId().toString()));
    }

    @Test
    void revue_hotline_interactionSurLaFicheMaisProspectionHorsPerimetre_resteInvisible() throws Exception {
        CfgProject projetA = newProjet("PRJ-TRACE-HFA");
        CfgProject projetB = newProjet("PRJ-TRACE-HFB");
        AdmUser restreint = newUser("trace.hotfiche");
        affecter(restreint, projetA);
        String telephone = telephoneUnique();
        CfgTier tier = newTier("CLI-TRACE-HF", telephone);
        ComActionMessage surB = newInteraction(tier, newProspection("PSN-TRACE-HFB", newTranche(projetB), tier), "B");
        ComActionMessage surFiche = newInteraction(tier, null, "fiche seule");

        String json = mockMvc.perform(get("/api/hotline/recherche").param("telephone", telephone).with(connecte(restreint)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JSONArray ids = JsonPath.read(json, "$.fiches[0].interactions[*].id");
        assertThat(ids).containsExactly(surFiche.getId().toString());
        assertThat(ids).doesNotContain(surB.getId().toString());
    }
}
