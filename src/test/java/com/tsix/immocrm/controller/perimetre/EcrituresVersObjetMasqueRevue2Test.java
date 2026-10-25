package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue (règle 6), tour 2 : inconnu = masqué sur les sites non couverts, et une fuite hors liste B.2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcrituresVersObjetMasqueRevue2Test {

    private static final String TRANCHE_ID = "trancheId";
    private static final String ARTICLE_ID = "articleId";
    private static final String DETAIL = "$.detail";
    private static final String DEVIS = "/api/prospections/{id}/devis";
    private static final String PRIX = "prix";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureReferentielStk referentielStk;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTierTypeRepository tierTypeRepository;

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
        type = patrimoine.type("TYP-MSQ3-" + jeton, referentielStk.natureParDefaut(), false);
        categorie = patrimoine.categorie("CAT-MSQ3-" + jeton, referentielStk.natureParDefaut(), false);
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-MSQ3-" + jeton);
        vocation = vocationRepository.save(vocation);
        trancheActive = vente.tranche("MSQ3A" + jeton, true);
        trancheMasquee = vente.tranche("MSQ3M" + jeton, false);
        articleActif = article("LOT-ACT3-" + jeton, trancheActive, null);
        articleMasque = article("LOT-MSQ3-" + jeton, trancheMasquee, null);
    }

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

    private ComProspection prospection() {
        CfgTierType pro = tierTypeRepository.findByCode("PRO").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("PRO");
            t.setDescription("PROSPECT");
            return tierTypeRepository.save(t);
        });
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("PRO-MSQ3-" + UUID.randomUUID().toString().substring(0, 8));
        tier.setFirstName("Prenom");
        tier.setLastName("Nom MSQ3 " + jeton);
        tier.setCfgTierType(pro);
        tier = tierRepository.save(tier);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-MSQ3-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        p.setCfgProspect(tier);
        p.setStkHierarchy(trancheActive);
        return prospectionRepository.save(p);
    }

    private ResultActions json(MockHttpServletRequestBuilder requete, String corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private static String detail400(ResultActions r) throws Exception {
        return JsonPath.read(r.andExpect(status().isBadRequest()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), DETAIL);
    }

    // ------------------------------------------------------------------ inconnu = masqué (sites non couverts au tour 1)

    @Test
    void site7_import_trancheInconnueEtMasquee_memeDetail() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "leads.csv", "text/csv",
                "Créé,Nom\n".getBytes(StandardCharsets.UTF_8));
        String masquee = detail400(mockMvc.perform(multipart("/api/pre-prospections/import").file(fichier)
                .param(TRANCHE_ID, trancheMasquee.getId().toString()).with(connecte(admin))));
        String inconnue = detail400(mockMvc.perform(multipart("/api/pre-prospections/import").file(fichier)
                .param(TRANCHE_ID, UUID.randomUUID().toString()).with(connecte(admin))));
        assertThat(masquee).isEqualTo(inconnue).contains(TRANCHE_ID).doesNotContain(trancheMasquee.getCode());
    }

    @Test
    void site9_modificationOffre_trancheInconnueEtMasquee_memeDetail() throws Exception {
        String creee = json(post(DEVIS, prospection().getId()),
                corps().avec(TRANCHE_ID, trancheActive.getId()).avec(PRIX, new BigDecimal("300000")).json())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID offre = UUID.fromString(JsonPath.read(creee, "$.id"));

        String masquee = detail400(json(patch("/api/devis/{id}", offre), corps().avec(TRANCHE_ID, trancheMasquee.getId()).json()));
        String inconnue = detail400(json(patch("/api/devis/{id}", offre), corps().avec(TRANCHE_ID, UUID.randomUUID()).json()));
        assertThat(masquee).isEqualTo(inconnue).contains(TRANCHE_ID).doesNotContain(trancheMasquee.getCode());
    }

    @Test
    void site11_sousArticleInconnuEtMasque_memeDetail() throws Exception {
        StkItem sousArticle = article("SA-MSQ3-" + jeton, trancheMasquee, articleActif);
        UUID prospectionId = prospection().getId();
        String masque = detail400(json(post(DEVIS, prospectionId), corps().avec(TRANCHE_ID, trancheActive.getId())
                .avec(ARTICLE_ID, articleActif.getId()).avec("sousArticleIds", List.of(sousArticle.getId())).json()));
        String inconnu = detail400(json(post(DEVIS, prospectionId), corps().avec(TRANCHE_ID, trancheActive.getId())
                .avec(ARTICLE_ID, articleActif.getId()).avec("sousArticleIds", List.of(UUID.randomUUID())).json()));
        assertThat(masque).isEqualTo(inconnu).contains("sousArticleIds").doesNotContain(sousArticle.getCode());
    }

    @Test
    void site14_sousArticleDuDossier_articleInconnuEtMasque_memeDetail() throws Exception {
        ComFolder dossier = vente.dossier("DOS-MSQ3-" + jeton, trancheActive, StatutDossier.RESERVATION);
        String masque = detail400(json(post("/api/dossiers/{id}/sous-articles", dossier.getId()),
                corps().avec(ARTICLE_ID, articleMasque.getId()).json()));
        String inconnu = detail400(json(post("/api/dossiers/{id}/sous-articles", dossier.getId()),
                corps().avec(ARTICLE_ID, UUID.randomUUID()).json()));
        assertThat(masque).isEqualTo(inconnu).contains(ARTICLE_ID).doesNotContain(articleMasque.getCode());
    }

    // ------------------------------------------------------------------ fuite hors liste B.2 (TrancheService.codeLibre)

    /**
     * B.1 « aucune écriture ne doit … refléter (message d'erreur) une tranche masquée » : renommer une tranche active
     * avec le code d'une tranche d'entité inactive → le 409 cite le libellé de la tranche masquée.
     */
    @Test
    void modificationTranche_codeDUneTrancheMasquee_409SansLeLibelleDeLaTrancheMasquee() throws Exception {
        json(patch("/api/tranches/{id}", trancheActive.getId()), corps().avec("code", trancheMasquee.getCode()).json())
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(not(containsString(trancheMasquee.getDescription()))));
    }
}
