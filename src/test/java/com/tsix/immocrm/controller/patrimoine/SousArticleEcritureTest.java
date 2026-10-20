package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_PROJET;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écriture des sous-articles — FEATURE-patrimoine-sous-articles.md §3, recalage R1, R2, R4-2 à R4-4, réponses du
 * manager (20/10) n° 3 : création libre ({@code POST /api/sous-articles}), {@code PATCH /api/sous-articles/{id}}
 * (parent et « Toujours affecté » d'un libre ; rattaché en lecture seule → 409), {@code PATCH /api/articles/{id}}
 * inchangé pour un rattaché, changement de parent = désaffecter puis affecter.
 * Parent créé par l'API : 63 m² × 13 000 TTC = 819 000 ; sous-article 60 000 TTC → parent 879 000.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticleEcritureTest {

    private static final String SOUS_ARTICLES = "/api/sous-articles";
    private static final String SOUS_ARTICLE = "/api/sous-articles/{id}";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String PARENT_ID = "parentId";
    private static final String TOUJOURS_AFFECTE = "toujoursAffecte";
    private static final String DESCRIPTION = "description";
    private static final String COM_SALE_PRICE = "ComSalePrice";
    private static final String ALWAYS_AFFECTED = "AlwaysAffected";
    private static final String PRIX_PARENT = "819000";
    private static final String PRIX_PARENT_AVEC_SOUS = "879000";
    private static final String MESSAGE_SANS_PARENT =
            "Vous ne pouvez pas toujours affecter un sous article sans article parent";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P4E");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    // ------------------------------------------------------------------ outils

    private UUID creerParent(String code) throws Exception {
        String json = mockMvc.perform(post("/api/articles").with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("trancheId", c.tranche().getId()).avec("code", code)
                                .avec(DESCRIPTION, "Parent " + code).avec("vocationId", VOCATION_VENTE)
                                .avec("categorieId", c.categorie().getId()).avec("typeId", c.type().getId())
                                .avec("immeubleId", c.immeuble().getId()).avec("etageId", c.etage().getId())
                                .avec("surfaceProvisoire", new BigDecimal("63"))
                                .avec("surfaceDefinitive", new BigDecimal("63"))
                                .avec("prixM2ProvisoireTTC", new BigDecimal("13000"))
                                .avec("prixM2DefinitifTTC", new BigDecimal("13000"))
                                .avec("tauxTva", new BigDecimal("0.20")).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    /** Sous-article rattaché par la route existante de la fiche parent. */
    private UUID creerRattache(UUID parentId, String code, boolean toujoursAffecte) throws Exception {
        String json = mockMvc.perform(post("/api/articles/{id}/sous-articles", parentId).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsSousArticle(code).sans("trancheId").avec(TOUJOURS_AFFECTE, toujoursAffecte).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    /** Corps d'un sous-article libre complet (tranche, classement « pour les sous-articles », immeuble, étage). */
    private CorpsJson corpsSousArticle(String code) {
        return corps().avec("trancheId", c.tranche().getId()).avec("code", code).avec(DESCRIPTION, "Parking " + code)
                .avec("categorieId", c.categorieSous().getId()).avec("typeId", c.typeSous().getId())
                .avec("immeubleId", c.immeuble().getId()).avec("etageId", c.etage().getId())
                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                .avec("prixM2ProvisoireHT", new BigDecimal("5000")).avec("prixM2DefinitifHT", new BigDecimal("5000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(SOUS_ARTICLES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(String url, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(url, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID seulId(String code) {
        List<String> ids = sousArticles.idsParCode(code, c.tranche().getId());
        assertThat(ids).as("article %s", code).hasSize(1);
        return UUID.fromString(ids.get(0));
    }

    private BigDecimal prixDeVente(UUID articleId) {
        return decimal(fixture.ligneArticle(articleId).get(COM_SALE_PRICE));
    }

    // ------------------------------------------------------------------ §3 POST création libre

    @Test
    void r3_creationLibre_201_defautsUnderItem_vocationVenteParDefaut() throws Exception {
        String code = code("LIB");

        creer(corpsSousArticle(code)).andExpect(status().isCreated());

        Tuple s = fixture.ligneArticle(seulId(code));
        assertAll(
                () -> assertThat(s.get("IsUnderItem")).isEqualTo(true),
                () -> assertThat(s.get("IsUnderItemOrigin")).isEqualTo(true),
                () -> assertThat(s.get("IsTitled")).isEqualTo(false),
                () -> assertThat(s.get("ParentId")).isNull(),
                () -> assertThat(s.get(ALWAYS_AFFECTED)).isEqualTo(false),
                () -> assertThat(guid(s.get("StkHierarchyId"))).isEqualTo(guid(c.tranche().getId())),
                () -> assertThat(guid(s.get("StkVocationId"))).isEqualTo(guid(VOCATION_VENTE)),
                () -> assertThat(guid(s.get("StkKindId"))).isEqualTo(guid(FixturePatrimoine.NATURE_BAT)),
                () -> assertThat(guid(s.get("StkBuildingId"))).isEqualTo(guid(c.immeuble().getId())),
                () -> assertThat(guid(s.get("StkFloorId"))).isEqualTo(guid(c.etage().getId())));
    }

    /** R4-3 : un sous-article libre porte sa propre vocation. */
    @Test
    void r4_3_creationLibre_vocationFournie_retenue() throws Exception {
        String code = code("VOC");
        StkVocation vocation = sousArticles.vocation("VOC-" + c.jeton());

        creer(corpsSousArticle(code).avec("vocationId", vocation.getId())).andExpect(status().isCreated());

        assertThat(guid(fixture.ligneArticle(seulId(code)).get("StkVocationId"))).isEqualTo(guid(vocation.getId()));
    }

    @Test
    void r4_3_creationLibre_vocationProjet_400() throws Exception {
        creer(corpsSousArticle(code("PROJ")).avec("vocationId", VOCATION_PROJET)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_creationLibre_immeubleManquant_400() throws Exception {
        String code = code("SIMM");

        creer(corpsSousArticle(code).sans("immeubleId")).andExpect(status().isBadRequest());

        assertThat(sousArticles.idsParCode(code, c.tranche().getId())).isEmpty();
    }

    @Test
    void r3_creationLibre_etageManquant_400() throws Exception {
        String code = code("SETG");

        creer(corpsSousArticle(code).sans("etageId")).andExpect(status().isBadRequest());

        assertThat(sousArticles.idsParCode(code, c.tranche().getId())).isEmpty();
    }

    @Test
    void r3_creationLibre_trancheManquante_400() throws Exception {
        creer(corpsSousArticle(code("STR")).sans("trancheId")).andExpect(status().isBadRequest());
    }

    @Test
    void r3_creationLibre_categoriePourArticlePrincipal_400() throws Exception {
        creer(corpsSousArticle(code("CATP")).avec("categorieId", c.categorie().getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r3_creationLibre_codeDejaPrisParUnArticleDeLaTranche_409() throws Exception {
        sousArticles.principal(c, code("DBL"), c.tranche());

        creer(corpsSousArticle(code("DBL"))).andExpect(status().isConflict());
    }

    @Test
    void r3_toujoursAffecteSansParent_400_messageOmran() throws Exception {
        creer(corpsSousArticle(code("TASP")).avec(TOUJOURS_AFFECTE, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SANS_PARENT)));
    }

    @Test
    void r3_creationAvecParent_memeTranche_rattacheEtParentRecalcule() throws Exception {
        UUID parentId = creerParent(code("PAR"));
        String code = code("AVP");

        creer(corpsSousArticle(code).avec(PARENT_ID, parentId)).andExpect(status().isCreated());

        assertAll(
                () -> assertThat(guid(fixture.ligneArticle(seulId(code)).get("ParentId"))).isEqualTo(guid(parentId)),
                () -> assertThat(prixDeVente(parentId)).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS));
    }

    @Test
    void r3_creationAvecParentDUneAutreTranche_400() throws Exception {
        StkItem ailleurs = sousArticles.principal(c, code("PAIL"), sousArticles.autreTranche(c, "B"));
        String code = code("AVPA");

        creer(corpsSousArticle(code).avec(PARENT_ID, ailleurs.getId())).andExpect(status().isBadRequest());

        assertThat(sousArticles.idsParCode(code, c.tranche().getId())).isEmpty();
    }

    @Test
    void r4_3_creationAvecParentDUneAutreVocation_400() throws Exception {
        UUID parentId = creerParent(code("PVOC"));
        StkVocation vocation = sousArticles.vocation("VOC-" + c.jeton());

        creer(corpsSousArticle(code("AVPV")).avec("vocationId", vocation.getId()).avec(PARENT_ID, parentId))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §3 PATCH d'un sous-article libre

    @Test
    void r3_patchLibre_description_200() throws Exception {
        StkItem libre = sousArticles.sousArticle(c, code("PDES"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(DESCRIPTION, "Parking modifie")).andExpect(status().isOk());

        assertThat(fixture.ligneArticle(libre.getId()).get("Description")).isEqualTo("Parking modifie");
    }

    @Test
    void r4_3_patchLibre_vocationPermise() throws Exception {
        StkItem libre = sousArticles.sousArticle(c, code("PVOCL"), null);
        StkVocation vocation = sousArticles.vocation("VOC-" + c.jeton());

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec("vocationId", vocation.getId())).andExpect(status().isOk());

        assertThat(guid(fixture.ligneArticle(libre.getId()).get("StkVocationId"))).isEqualTo(guid(vocation.getId()));
    }

    @Test
    void r3_patchLibre_parentId_rattacheEtParentRecalcule() throws Exception {
        UUID parentId = creerParent(code("PPAR"));
        StkItem libre = sousArticles.sousArticle(c, code("PRAT"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(PARENT_ID, parentId)).andExpect(status().isOk());

        Tuple s = fixture.ligneArticle(libre.getId());
        assertAll(
                () -> assertThat(guid(s.get("ParentId"))).isEqualTo(guid(parentId)),
                () -> assertThat(s.get(ALWAYS_AFFECTED)).isEqualTo(false),
                () -> assertThat(prixDeVente(parentId)).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS));
    }

    @Test
    void r3_patchLibre_parentIdEtToujoursAffecte_lesDeuxEcrits() throws Exception {
        UUID parentId = creerParent(code("PTA"));
        StkItem libre = sousArticles.sousArticle(c, code("PTAS"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(PARENT_ID, parentId).avec(TOUJOURS_AFFECTE, true))
                .andExpect(status().isOk());

        Tuple s = fixture.ligneArticle(libre.getId());
        assertAll(
                () -> assertThat(guid(s.get("ParentId"))).isEqualTo(guid(parentId)),
                () -> assertThat(s.get(ALWAYS_AFFECTED)).isEqualTo(true));
    }

    @Test
    void r3_patchLibre_toujoursAffecteSansParent_400() throws Exception {
        StkItem libre = sousArticles.sousArticle(c, code("PTASP"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(TOUJOURS_AFFECTE, true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_SANS_PARENT)));

        assertThat(fixture.ligneArticle(libre.getId()).get(ALWAYS_AFFECTED)).isEqualTo(false);
    }

    @Test
    void r4_3_patchLibre_parentDUneAutreTranche_400() throws Exception {
        StkItem ailleurs = sousArticles.principal(c, code("PPAIL"), sousArticles.autreTranche(c, "C"));
        StkItem libre = sousArticles.sousArticle(c, code("PLAIL"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(PARENT_ID, ailleurs.getId()))
                .andExpect(status().isBadRequest());

        assertThat(fixture.ligneArticle(libre.getId()).get("ParentId")).isNull();
    }

    @Test
    void r4_3_patchLibre_parentDUneAutreVocation_400() throws Exception {
        UUID parentId = creerParent(code("PPVOC"));
        StkItem libre = sousArticles.sousArticle(c, code("PLVOC"), null);
        libre.setStkVocation(sousArticles.vocation("VOC-" + c.jeton()));
        sousArticles.enregistrer(libre);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(PARENT_ID, parentId)).andExpect(status().isBadRequest());

        assertThat(fixture.ligneArticle(libre.getId()).get("ParentId")).isNull();
    }

    @Test
    void r2_patchLibre_parentOccupe_409() throws Exception {
        UUID parentId = creerParent(code("PPOCC"));
        StkItem parent = sousArticles.recharger(parentId);
        parent.setComFolderId(fixture.dossier("DOS-" + code("PPOCC")).getId());
        sousArticles.enregistrer(parent);
        StkItem libre = sousArticles.sousArticle(c, code("PLOCC"), null);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(PARENT_ID, parentId)).andExpect(status().isConflict());

        assertThat(fixture.ligneArticle(libre.getId()).get("ParentId")).isNull();
    }

    @Test
    void r3_patch_sousArticleInconnu_404() throws Exception {
        modifier(SOUS_ARTICLE, UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void visibiliteR4_patch_entiteInactive_404() throws Exception {
        StkItem libre = sousArticles.sousArticle(c, code("PR4"), null);
        entites.actif(c.entite().getId(), false);

        modifier(SOUS_ARTICLE, libre.getId(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ R4-2 / réponse 3 : rattaché

    @Test
    void reponse3_patchRattacheSurSousArticles_409_ligneInchangee() throws Exception {
        UUID parentId = creerParent(code("RPAR"));
        UUID rattache = creerRattache(parentId, code("RRAT"), false);

        modifier(SOUS_ARTICLE, rattache, corps().avec(DESCRIPTION, "Modifie hors fiche parent"))
                .andExpect(status().isConflict());

        assertThat(fixture.ligneArticle(rattache).get("Description")).isEqualTo("Parking " + code("RRAT"));
    }

    @Test
    void reponse3_patchRattacheSurArticles_inchange_200() throws Exception {
        UUID parentId = creerParent(code("APAR"));
        UUID rattache = creerRattache(parentId, code("ARAT"), false);

        modifier(ARTICLE, rattache, corps().avec(DESCRIPTION, "Modifie depuis la fiche parent"))
                .andExpect(status().isOk());

        assertThat(fixture.ligneArticle(rattache).get("Description")).isEqualTo("Modifie depuis la fiche parent");
    }

    /** R1 : toujoursAffecte est lu en PATCH (fiche parent, PATCH /api/articles/{id}). */
    @Test
    void r1_patchRattacheSurArticles_toujoursAffecteEcrit() throws Exception {
        UUID parentId = creerParent(code("TPAR"));
        UUID rattache = creerRattache(parentId, code("TRAT"), false);

        modifier(ARTICLE, rattache, corps().avec(TOUJOURS_AFFECTE, true)).andExpect(status().isOk());

        assertThat(fixture.ligneArticle(rattache).get(ALWAYS_AFFECTED)).isEqualTo(true);
    }

    /** Réponse 3 : changer de parent = désaffecter (route existante) puis rattacher le libre à son nouveau parent. */
    @Test
    void reponse3_desaffecterPuisAffecter_lesDeuxParentsRecalcules() throws Exception {
        UUID ancien = creerParent(code("ANC"));
        UUID nouveau = creerParent(code("NOU"));
        UUID sous = creerRattache(ancien, code("CHG"), false);
        assertThat(prixDeVente(ancien)).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS);

        mockMvc.perform(post("/api/articles/{id}/sous-articles/{sousArticleId}/desaffecter", ancien, sous)
                        .with(connecte(admin)))
                .andExpect(status().isOk());
        modifier(SOUS_ARTICLE, sous, corps().avec(PARENT_ID, nouveau)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(guid(fixture.ligneArticle(sous).get("ParentId"))).isEqualTo(guid(nouveau)),
                () -> assertThat(prixDeVente(ancien)).isEqualByComparingTo(PRIX_PARENT),
                () -> assertThat(prixDeVente(nouveau)).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS));
    }
}
