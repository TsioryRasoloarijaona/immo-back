package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureChargementTranche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écritures vers un objet masqué — FEATURE-patrimoine-reliquat-v2.md §B (règle 6) : une tranche ou un article d'une entité
 * inactive désigné dans le corps → 400 sur le champ, Administrateur compris, sans écriture ni fuite (le message ne cite ni
 * le code de l'objet ni « inactive »). Un test par site du tableau §B.2 (numéro dans le nom).
 *
 * <p>Objet masqué = rattaché à une tranche d'entité inactive : {@code StkItemRepository.findVisible} ignore la société
 * directe d'un article qui a une tranche, une tranche active rend donc tous ses articles visibles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcrituresVersObjetMasqueTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String ARTICLE_ID = "articleId";
    private static final String ARTICLE_IDS = "articleIds";
    private static final String COMMERCIAL_ID = "commercialId";
    private static final String PRIX = "prix";
    private static final String DEVIS = "/api/prospections/{id}/devis";
    private static final String PROSPECTIONS_DE_LA_TRANCHE = "SELECT COUNT(*) FROM ComProspection WHERE StkHierarchyId = :id";
    private static final String TRANCHE_DE_LA_PROSPECTION = "SELECT StkHierarchyId FROM ComProspection WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private FixtureReferentielStk referentielStk;
    @Autowired private FixtureChargementTranche lecture;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;
    @Autowired private ComProspectionOriginRepository origineRepository;
    @Autowired private ComActionMessageTypeRepository typeContactRepository;
    @Autowired private ComProspectionKindRepository natureRepository;
    @Autowired private ComPaymentMethodRepository modePaieRepository;

    private String jeton;
    private AdmUser admin;
    private StkItemType type;
    private StkItemCategory categorie;
    private StkVocation vocation;
    private CfgTranche trancheActive;
    private CfgTranche trancheMasquee;
    private StkItem articleActif;
    private StkItem articleMasque;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        type = patrimoine.type("TYP-MSQ-" + jeton, referentielStk.natureParDefaut(), false);
        categorie = patrimoine.categorie("CAT-MSQ-" + jeton, referentielStk.natureParDefaut(), false);
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-MSQ-" + jeton);
        vocation = vocationRepository.save(vocation);
        trancheActive = vente.tranche("MSQ-A-" + jeton, true);
        trancheMasquee = vente.tranche("MSQ-M-" + jeton, false);
        articleActif = article("LOT-ACT-" + jeton, trancheActive, null);
        articleMasque = article("LOT-MSQ-" + jeton, trancheMasquee, null);
    }

    // ------------------------------------------------------------------ outils

    private StkItem article(String code, CfgTranche tranche, StkItem parent) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Article " + code);
        item.setStkItemType(type);
        item.setStkItemCategory(categorie);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setParent(parent);
        item.setComSalePrice(new BigDecimal("500000"));
        item.setVat(new BigDecimal("0.2"));
        item.setStkKind(referentielStk.natureParDefaut());
        return stkItemRepository.save(item);
    }

    private CfgTier tiers(String prefixe, CfgTierType typeTiers) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(prefixe + "-" + jeton);
        tier.setFirstName("Prenom");
        tier.setLastName("Nom " + prefixe + " " + jeton);
        tier.setCfgTierType(typeTiers);
        return tierRepository.save(tier);
    }

    private CfgTier prospect() {
        CfgTierType pro = tierTypeRepository.findByCode("PRO").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("PRO");
            t.setDescription("PROSPECT");
            return tierTypeRepository.save(t);
        });
        return tiers("PRO-MSQ", pro);
    }

    private ComProspection prospection(CfgTier prospect, CfgTranche tranche, boolean preProspection) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-MSQ-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        p.setCfgProspect(prospect);
        p.setStkHierarchy(tranche);
        if (preProspection) {
            p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        }
        return prospectionRepository.save(p);
    }

    private String corpsCreation(UUID trancheId, UUID articleId) {
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-MSQ-" + jeton);
        origine.setDescription("Facebook");
        origineRepository.save(origine);
        ComActionMessageType typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TYP-MSQ-" + jeton);
        typeContact.setDescription("Message entrant");
        typeContactRepository.save(typeContact);
        ComProspectionKind nature = new ComProspectionKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("NAT-MSQ-" + jeton);
        nature.setDescription("Qualifie");
        natureRepository.save(nature);
        return corps().avec("prospectId", prospect().getId()).avec(TRANCHE_ID, trancheId).avec(ARTICLE_ID, articleId)
                .avec("origineId", origine.getId()).avec("typeContactId", typeContact.getId())
                .avec("etapeId", nature.getId()).avec("notes", "Interesse par un T3").json();
    }

    private ResultActions envoyer(MockHttpServletRequestBuilder requete, CorpsJson corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions envoyer(MockHttpServletRequestBuilder requete, String corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    /** §B.1 : 400, {@code detail} nomme le champ, ne cite ni l'objet masqué ni son entité inactive. */
    private static ResultActions refuseSurLeChamp(ResultActions resultat, String champ, String codeMasque)
            throws Exception {
        return resultat.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(allOf(containsString(champ), not(containsString(codeMasque)),
                        not(containsStringIgnoringCase("inactive")))));
    }

    private int compter(String sql, UUID id) {
        return lecture.compter(sql, id);
    }

    private String guid(String sql, UUID id) {
        return FixturePatrimoine.guid(lecture.texte(sql, id));
    }

    // ------------------------------------------------------------------ prospections (sites 1 à 7)

    @Test
    void site1_creationProspection_trancheMasquee_400TrancheId_aucuneProspection() throws Exception {
        refuseSurLeChamp(envoyer(post("/api/prospections"), corpsCreation(trancheMasquee.getId(), null)),
                TRANCHE_ID, trancheMasquee.getCode());

        assertThat(compter(PROSPECTIONS_DE_LA_TRANCHE, trancheMasquee.getId())).isZero();
    }

    @Test
    void site2_creationProspection_articleMasque_400ArticleId_aucuneProspection() throws Exception {
        refuseSurLeChamp(envoyer(post("/api/prospections"), corpsCreation(trancheActive.getId(), articleMasque.getId())),
                ARTICLE_ID, articleMasque.getCode());

        assertThat(compter("SELECT COUNT(*) FROM ComProspection WHERE StkItemId = :id", articleMasque.getId())).isZero();
    }

    @Test
    void site3_creationPreProspection_trancheMasquee_400TrancheId_aucuneLigne() throws Exception {
        refuseSurLeChamp(envoyer(post("/api/pre-prospections"),
                        corps().avec("nom", "Lead MSQ " + UUID.randomUUID()).avec(TRANCHE_ID, trancheMasquee.getId())),
                TRANCHE_ID, trancheMasquee.getCode());

        assertThat(compter(PROSPECTIONS_DE_LA_TRANCHE, trancheMasquee.getId())).isZero();
    }

    @Test
    void site4_transformation_trancheMasquee_400TrancheId_resteUnePreProspection() throws Exception {
        ComProspection pre = prospection(prospect(), null, true);
        CfgTier commercial = tiers("COM-MSQ", null);

        refuseSurLeChamp(envoyer(post("/api/prospections/{id}/transformer", pre.getId()),
                        corps().avec(COMMERCIAL_ID, commercial.getId()).avec(TRANCHE_ID, trancheMasquee.getId())),
                TRANCHE_ID, trancheMasquee.getCode());

        assertAll(
                () -> assertThat(guid("SELECT StrEntityId FROM ComProspection WHERE PKey = :id", pre.getId()))
                        .isEqualTo(FixturePatrimoine.guid(DiscriminantPreProspection.ID_PRE_PROSPECTION)),
                () -> assertThat(lecture.texte("SELECT CfgCommercialId FROM ComProspection WHERE PKey = :id", pre.getId()))
                        .isNull(),
                () -> assertThat(lecture.texte(TRANCHE_DE_LA_PROSPECTION, pre.getId())).isNull());
    }

    @Test
    void site5_tranchePrincipale_trancheMasquee_400TrancheId_trancheDOrigineGardee() throws Exception {
        ComProspection p = prospection(null, trancheActive, false);

        refuseSurLeChamp(envoyer(patch("/api/prospections/{id}/tranche-principale", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId())), TRANCHE_ID, trancheMasquee.getCode());

        assertThat(guid(TRANCHE_DE_LA_PROSPECTION, p.getId())).isEqualTo(FixturePatrimoine.guid(trancheActive.getId()));
    }

    @Test
    void site5_tranchePrincipale_articleMasque_400ArticleId_articleNonRattache() throws Exception {
        ComProspection p = prospection(null, trancheActive, false);

        refuseSurLeChamp(envoyer(patch("/api/prospections/{id}/tranche-principale", p.getId()),
                        corps().avec(TRANCHE_ID, trancheActive.getId()).avec(ARTICLE_ID, articleMasque.getId())),
                ARTICLE_ID, articleMasque.getCode());

        assertThat(lecture.texte("SELECT StkItemId FROM ComProspection WHERE PKey = :id", p.getId())).isNull();
    }

    @Test
    void site6_autreTranche_trancheMasquee_400TrancheId_aucunComProspectionProject() throws Exception {
        ComProspection p = prospection(null, trancheActive, false);

        refuseSurLeChamp(envoyer(post("/api/prospections/{id}/autres-tranches", p.getId()),
                corps().avec(TRANCHE_ID, trancheMasquee.getId())), TRANCHE_ID, trancheMasquee.getCode());

        assertThat(compter("SELECT COUNT(*) FROM ComProspectionProject WHERE ComProspectionId = :id", p.getId())).isZero();
    }

    @Test
    void site6_autreTranche_articleMasque_400ArticleId_aucunComProspectionProject() throws Exception {
        ComProspection p = prospection(null, null, false);

        refuseSurLeChamp(envoyer(post("/api/prospections/{id}/autres-tranches", p.getId()),
                        corps().avec(TRANCHE_ID, trancheActive.getId()).avec(ARTICLE_ID, articleMasque.getId())),
                ARTICLE_ID, articleMasque.getCode());

        assertThat(compter("SELECT COUNT(*) FROM ComProspectionProject WHERE ComProspectionId = :id", p.getId())).isZero();
    }

    @Test
    void site7_importPreProspections_trancheMasquee_400TrancheId_aucuneLigneImportee() throws Exception {
        String fichier = "Créé,Nom,Adresse e-mail,Source,Formulaire,Canal,Étape,Propriétaire,Étiquettes,Téléphone,"
                + "Numéro de téléphone secondaire,Numéro WhatsApp\n"
                + "09/23/2026 1:45am,Imane Msq " + jeton + ",imane.msq@test.local,Payé," + trancheMasquee.getCode()
                + " FORM,E-mail,À évaluer,Unassigned,,+212663434434,,\n";

        refuseSurLeChamp(mockMvc.perform(multipart("/api/pre-prospections/import")
                        .file(new MockMultipartFile("fichier", "leads.csv", "text/csv",
                                fichier.getBytes(StandardCharsets.UTF_8)))
                        .param(TRANCHE_ID, trancheMasquee.getId().toString())
                        .with(connecte(admin))),
                TRANCHE_ID, trancheMasquee.getCode());

        assertThat(compter(PROSPECTIONS_DE_LA_TRANCHE, trancheMasquee.getId())).isZero();
    }

    // ------------------------------------------------------------------ offres (sites 8 à 11)

    @Test
    void site8_creationOffre_trancheMasquee_400TrancheId_aucunStlQuote() throws Exception {
        ComProspection p = prospection(prospect(), null, false);

        refuseSurLeChamp(envoyer(post(DEVIS, p.getId()),
                        corps().avec(TRANCHE_ID, trancheMasquee.getId()).avec(PRIX, new BigDecimal("300000"))),
                TRANCHE_ID, trancheMasquee.getCode());

        assertThat(compter("SELECT COUNT(*) FROM StlQuote WHERE CfgTrancheId = :id", trancheMasquee.getId())).isZero();
    }

    /** Route du site 9 trouvée par Grep : {@code DevisController} {@code @PatchMapping("/{id}")} → {@code modifierDevis}. */
    @Test
    void site9_modificationOffre_trancheMasquee_400TrancheId_offreInchangee() throws Exception {
        ComProspection p = prospection(prospect(), null, false);
        String creee = envoyer(post(DEVIS, p.getId()),
                        corps().avec(TRANCHE_ID, trancheActive.getId()).avec(PRIX, new BigDecimal("300000")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID offre = UUID.fromString(JsonPath.read(creee, "$.id"));

        refuseSurLeChamp(envoyer(patch("/api/devis/{id}", offre), corps().avec(TRANCHE_ID, trancheMasquee.getId())),
                TRANCHE_ID, trancheMasquee.getCode());

        assertThat(guid("SELECT CfgTrancheId FROM StlQuote WHERE PKey = :id", offre))
                .isEqualTo(FixturePatrimoine.guid(trancheActive.getId()));
    }

    @Test
    void site10_creationOffre_articleMasque_400ArticleId_aucunStlQuote() throws Exception {
        ComProspection p = prospection(prospect(), null, false);

        refuseSurLeChamp(envoyer(post(DEVIS, p.getId()),
                        corps().avec(TRANCHE_ID, trancheActive.getId()).avec(ARTICLE_ID, articleMasque.getId())),
                ARTICLE_ID, articleMasque.getCode());

        assertThat(compter("SELECT COUNT(*) FROM StlQuote WHERE StkItemId = :id", articleMasque.getId())).isZero();
    }

    /** Sous-article enfant d'un article visible mais rangé sous une tranche d'entité inactive : masqué par findVisible. */
    @Test
    void site11_creationOffre_sousArticleMasque_400SousArticleIds_aucunStlQuote() throws Exception {
        ComProspection p = prospection(prospect(), null, false);
        StkItem sousArticle = article("SA-MSQ-" + jeton, trancheMasquee, articleActif);

        refuseSurLeChamp(envoyer(post(DEVIS, p.getId()), corps().avec(TRANCHE_ID, trancheActive.getId())
                        .avec(ARTICLE_ID, articleActif.getId()).avec("sousArticleIds", List.of(sousArticle.getId()))),
                "sousArticleIds", sousArticle.getCode());

        assertThat(compter("SELECT COUNT(*) FROM StlQuote WHERE StkItemId = :id", articleActif.getId())).isZero();
    }

    // ------------------------------------------------------------------ dossiers (sites 12 à 14)

    @Test
    void site12_creationDossier_articleMasque_400ArticleIds_aucunDossierArticleLibre() throws Exception {
        statuts.reservation();
        ComProspection p = prospection(prospect(), null, false);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-MSQ-" + jeton);
        modePaie = modePaieRepository.save(modePaie);

        refuseSurLeChamp(envoyer(post("/api/dossiers"), corps().avec("prospectionId", p.getId())
                        .avec(ARTICLE_IDS, List.of(articleMasque.getId())).avec("modePaieId", modePaie.getId())
                        .avec("cin", "CIN-MSQ")),
                ARTICLE_IDS, articleMasque.getCode());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM ComFolder WHERE StkItemId = :id", articleMasque.getId()))
                        .isZero(),
                () -> assertThat(lecture.texte("SELECT ComFolderId FROM StkItem WHERE PKey = :id", articleMasque.getId()))
                        .isNull());
    }

    @Test
    void site13_simulationFinancement_articleMasque_400ArticleIds_sansMontant() throws Exception {
        refuseSurLeChamp(envoyer(post("/api/dossiers/simulation-financement"),
                        corps().avec(ARTICLE_IDS, List.of(articleMasque.getId())).avec(PRIX, new BigDecimal("1548000"))),
                ARTICLE_IDS, articleMasque.getCode())
                .andExpect(jsonPath("$.totalGeneral").doesNotExist())
                .andExpect(jsonPath("$.lignes").doesNotExist());
    }

    @Test
    void site14_ajoutSousArticleAuDossier_articleMasque_400ArticleId_aucunSousArticleAffecte() throws Exception {
        ComFolder dossier = vente.dossier("DOS-MSQ-" + jeton, trancheActive, StatutDossier.RESERVATION);

        refuseSurLeChamp(envoyer(post("/api/dossiers/{id}/sous-articles", dossier.getId()),
                corps().avec(ARTICLE_ID, articleMasque.getId())), ARTICLE_ID, articleMasque.getCode());

        assertThat(lecture.texte("SELECT ComFolderId FROM StkItem WHERE PKey = :id", articleMasque.getId())).isNull();
    }
}
