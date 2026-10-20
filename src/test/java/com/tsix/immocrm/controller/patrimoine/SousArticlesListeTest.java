package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste {@code GET /api/sous-articles} — FEATURE-patrimoine-sous-articles.md §2, recalage R4-6 à R4-8, réponses du
 * manager (20/10) n° 4 : {@code IsUnderItem = 1 AND IsUnderItemOrigin = 1}, filtres OMRAN dont code parent (espaces
 * retirés des deux côtés), état LIBRE / VENDU / LOUE sur le dossier du sous-article lui-même, indicateur « parent
 * occupé », visibilité R4 des articles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticlesListeTest {

    private static final String LISTE = "/api/sous-articles";
    private static final String TRANCHE_ID = "trancheId";
    private static final String ETAT = "etat";
    private static final String CODE_PARENT = "codeParent";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";
    private static final String COLONNES = "colonnes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P4L");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(LISTE).param(TRANCHE_ID, c.tranche().getId().toString()).param(SIZE, TOUS).with(connecte(admin));
    }

    private ResultActions lister(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk());
    }

    private static String ligne(StkItem item) {
        return "$.content[?(@.id=='" + item.getId() + "')]";
    }

    // ------------------------------------------------------------------ §2 périmètre de la liste

    @Test
    void r2_perimetre_sousArticlesDOrigineSeuls_libresEtRattaches() throws Exception {
        StkItem parent = sousArticles.principal(c, code("PAR"), c.tranche());
        StkItem libre = sousArticles.sousArticle(c, code("LIB"), null);
        StkItem rattache = sousArticles.sousArticle(c, code("RAT"), parent);
        StkItem pasDOrigine = sousArticles.sousArticle(c, code("NOR"), null);
        pasDOrigine.setIsUnderItemOrigin(false);
        sousArticles.enregistrer(pasDOrigine);

        lister(liste())
                .andExpect(jsonPath(ligne(libre)).isNotEmpty())
                .andExpect(jsonPath(ligne(rattache)).isNotEmpty())
                .andExpect(jsonPath(ligne(parent)).isEmpty())
                .andExpect(jsonPath(ligne(pasDOrigine)).isEmpty());
    }

    // ------------------------------------------------------------------ §2 code parent

    @Test
    void r2_codeParent_espacesRetiresDesDeuxCotes() throws Exception {
        StkItem parent = sousArticles.principal(c, "A B" + c.jeton() + " 7", c.tranche());
        StkItem enfant = sousArticles.sousArticle(c, code("ENF"), parent);
        StkItem autreParent = sousArticles.principal(c, code("AUTRE"), c.tranche());
        StkItem autreEnfant = sousArticles.sousArticle(c, code("AENF"), autreParent);
        StkItem libre = sousArticles.sousArticle(c, code("LIBCP"), null);

        lister(liste().param(CODE_PARENT, "AB " + c.jeton() + "7"))
                .andExpect(jsonPath(ligne(enfant)).isNotEmpty())
                .andExpect(jsonPath(ligne(autreEnfant)).isEmpty())
                .andExpect(jsonPath(ligne(libre)).isEmpty());
    }

    @Test
    void r2_codeParent_sousChaine() throws Exception {
        StkItem parent = sousArticles.principal(c, "VILLA-" + c.jeton() + "-12", c.tranche());
        StkItem enfant = sousArticles.sousArticle(c, code("SCH"), parent);

        lister(liste().param(CODE_PARENT, c.jeton() + "-1"))
                .andExpect(jsonPath(ligne(enfant)).isNotEmpty());
    }

    // ------------------------------------------------------------------ R4-6 état du sous-article lui-même

    @Test
    void r4_6_etatLibre_sansDossierPropre_memeSiLeParentEstVendu() throws Exception {
        StkItem parent = sousArticles.principal(c, code("PVENDU"), c.tranche());
        parent.setComFolderId(fixture.dossier("DOS-" + code("PVENDU")).getId());
        sousArticles.enregistrer(parent);
        StkItem enfant = sousArticles.sousArticle(c, code("ENFL"), parent);
        StkItem vendu = sousArticles.sousArticle(c, code("VENDU"), null);
        vendu.setComFolderId(fixture.dossier("DOS-" + code("VENDU")).getId());
        sousArticles.enregistrer(vendu);

        lister(liste().param(ETAT, "LIBRE"))
                .andExpect(jsonPath(ligne(enfant)).isNotEmpty())
                .andExpect(jsonPath(ligne(vendu)).isEmpty());
    }

    @Test
    void r4_6_etatVendu_dossierDeVenteDuSousArticle() throws Exception {
        StkItem vendu = sousArticles.sousArticle(c, code("VEN"), null);
        vendu.setComFolderId(fixture.dossier("DOS-" + code("VEN")).getId());
        sousArticles.enregistrer(vendu);
        StkItem libre = sousArticles.sousArticle(c, code("VLIB"), null);

        lister(liste().param(ETAT, "VENDU"))
                .andExpect(jsonPath(ligne(vendu)).isNotEmpty())
                .andExpect(jsonPath(ligne(libre)).isEmpty());
    }

    @Test
    void r4_6_etatLoue_dossierDeLocationDuSousArticle() throws Exception {
        StkItem loue = sousArticles.sousArticle(c, code("LOU"), null);
        StkItem libre = sousArticles.sousArticle(c, code("LLIB"), null);
        fiche.occuper(loue.getId(), "RntFolderId", fiche.location("LOC-" + code("LOU")));

        lister(liste().param(ETAT, "LOUE"))
                .andExpect(jsonPath(ligne(loue)).isNotEmpty())
                .andExpect(jsonPath(ligne(libre)).isEmpty());
    }

    /** Réponse 4 : indicateur « parent occupé » dans la ligne (nom de colonne choisi : parentOccupe). */
    @Test
    void reponse4_parentOccupe_vraiQuandLeParentEstVendu_fauxSinon() throws Exception {
        StkItem parentVendu = sousArticles.principal(c, code("POCC"), c.tranche());
        parentVendu.setComFolderId(fixture.dossier("DOS-" + code("POCC")).getId());
        sousArticles.enregistrer(parentVendu);
        StkItem enfantVerrouille = sousArticles.sousArticle(c, code("EVER"), parentVendu);
        StkItem parentLibre = sousArticles.principal(c, code("PLIB"), c.tranche());
        StkItem enfantLibre = sousArticles.sousArticle(c, code("ELIB"), parentLibre);

        lister(liste().param(COLONNES, "code,parentOccupe"))
                .andExpect(jsonPath(ligne(enfantVerrouille) + ".parentOccupe").value(true))
                .andExpect(jsonPath(ligne(enfantLibre) + ".parentOccupe").value(false));
    }

    // ------------------------------------------------------------------ §2 autres filtres

    @Test
    void r2_filtreEntite() throws Exception {
        StkItem dedans = sousArticles.sousArticle(c, code("ENT"), null);
        Contexte ailleurs = sousArticles.contexte("P4LE");
        StkItem dehors = sousArticles.sousArticle(ailleurs, "ENT-" + ailleurs.jeton(), null);

        lister(get(LISTE).param("entiteId", c.entite().getId().toString()).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(jsonPath(ligne(dedans)).isNotEmpty())
                .andExpect(jsonPath(ligne(dehors)).isEmpty());
    }

    @Test
    void r2_filtreTranche() throws Exception {
        StkItem dedans = sousArticles.sousArticle(c, code("TRA"), null);
        StkItem dehors = sousArticles.sousArticle(c, code("TRB"), null);
        dehors.setStkHierarchy(sousArticles.autreTranche(c, "B"));
        sousArticles.enregistrer(dehors);

        lister(liste())
                .andExpect(jsonPath(ligne(dedans)).isNotEmpty())
                .andExpect(jsonPath(ligne(dehors)).isEmpty());
    }

    @Test
    void r2_filtreVocation() throws Exception {
        StkItem vente = sousArticles.sousArticle(c, code("VOCV"), null);
        StkItem autre = sousArticles.sousArticle(c, code("VOCA"), null);
        autre.setStkVocation(sousArticles.vocation("VOC-" + c.jeton()));
        sousArticles.enregistrer(autre);

        lister(liste().param("vocationId", autre.getStkVocation().getId().toString()))
                .andExpect(jsonPath(ligne(autre)).isNotEmpty())
                .andExpect(jsonPath(ligne(vente)).isEmpty());
    }

    @Test
    void r2_filtreCategorieEtType() throws Exception {
        StkItem cible = sousArticles.sousArticle(c, code("CATC"), null);
        StkItem autre = sousArticles.sousArticle(c, code("CATA"), null);
        autre.setStkItemCategory(fixture.categorie("CATS2-" + c.jeton(), fixture.natureBat(), true));
        autre.setStkItemType(fixture.type("TYPS2-" + c.jeton(), fixture.natureBat(), true));
        sousArticles.enregistrer(autre);

        lister(liste().param("categorieId", c.categorieSous().getId().toString())
                .param("typeId", c.typeSous().getId().toString()))
                .andExpect(jsonPath(ligne(cible)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isEmpty());
    }

    @Test
    void r2_filtrePrix_deA() throws Exception {
        StkItem dansLaFourchette = sousArticles.sousArticle(c, code("PRIX"), null);
        StkItem tropCher = sousArticles.sousArticle(c, code("CHER"), null);
        tropCher.setSalePrice(new BigDecimal("900000"));
        tropCher.setComSalePrice(new BigDecimal("900000"));
        sousArticles.enregistrer(tropCher);

        lister(liste().param("prixDe", "50000").param("prixA", "70000"))
                .andExpect(jsonPath(ligne(dansLaFourchette)).isNotEmpty())
                .andExpect(jsonPath(ligne(tropCher)).isEmpty());
    }

    @Test
    void r2_filtreSurface_deA() throws Exception {
        StkItem petit = sousArticles.sousArticle(c, code("PETIT"), null);
        StkItem grand = sousArticles.sousArticle(c, code("GRAND"), null);
        grand.setArea(new BigDecimal("40"));
        sousArticles.enregistrer(grand);

        lister(liste().param("surfaceDe", "5").param("surfaceA", "15"))
                .andExpect(jsonPath(ligne(petit)).isNotEmpty())
                .andExpect(jsonPath(ligne(grand)).isEmpty());
    }

    // ------------------------------------------------------------------ R4-7 recherche

    @Test
    void r4_7_recherche_codeDuSousArticle_espacesIgnores() throws Exception {
        StkItem cible = sousArticles.sousArticle(c, "PK " + c.jeton(), null);
        StkItem autre = sousArticles.sousArticle(c, code("ZZ"), null);

        lister(liste().param(SEARCH, "PK" + c.jeton()))
                .andExpect(jsonPath(ligne(cible)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isEmpty());
    }

    @Test
    void r4_7_recherche_surLeCodeParent() throws Exception {
        StkItem parent = sousArticles.principal(c, "MAISON-" + c.jeton(), c.tranche());
        StkItem enfant = sousArticles.sousArticle(c, code("RCP"), parent);
        StkItem autre = sousArticles.sousArticle(c, code("RCA"), null);

        lister(liste().param(SEARCH, "MAISON-" + c.jeton()))
                .andExpect(jsonPath(ligne(enfant)).isNotEmpty())
                .andExpect(jsonPath(ligne(autre)).isEmpty());
    }

    // ------------------------------------------------------------------ visibilité R4

    @Test
    void visibiliteR4_entiteInactive_sousArticleAbsent() throws Exception {
        StkItem cache = sousArticles.sousArticle(c, code("R4"), null);
        lister(liste()).andExpect(jsonPath(ligne(cache)).isNotEmpty());

        entites.actif(c.entite().getId(), false);

        lister(liste()).andExpect(jsonPath(ligne(cache)).isEmpty());
    }
}
