package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.SOUS_ARTICLES;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ARTICLE_PARENT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ETAGE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.IMMEUBLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.NATURE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.NOTES;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_DEFI_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_PROV_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TOUJOURS_AFFECTE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modèle téléchargé — FEATURE-patrimoine-import-export.md §1.1 (GET {@code …/modele}, nom
 * {@code <tranche>_STOCK_ARTICLES_<horodatage>.xlsx} / {@code …_STOCK_SOUS_ARTICLES_…}, pré-rempli avec les lignes de
 * la tranche), §1.4 / §1.5 (en-têtes OMRAN exacts, feuilles d'aide Code / Libellé), §7 (« modèle relu par POI »).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportModeleTest {

    /** En-têtes du §1.4 (libellés de omran-src/Yes.Rems/Yes.Stk.Front.Import/ImportItems.cs). */
    private static final List<String> ENTETES_ARTICLES = entetesArticles();

    private static final List<String> ENTETES_COMMUNS_SOUS_ARTICLES = List.of(PIVOT, TRANCHE, VOCATION, CODE,
            DESCRIPTION, IMMEUBLE, ETAGE, CATEGORIE, TYPE, "Orientation", "Catégorie de charge", ARTICLE_PARENT,
            TOUJOURS_AFFECTE, "Adresse", NOTES, PRIX_PROV_HT, PRIX_DEFI_HT, SURFACE_PROV, SURFACE_DEFI, TVA);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P9M");
    }

    private static List<String> entetesArticles() {
        List<String> entetes = new ArrayList<>(List.of(PIVOT, TRANCHE, NATURE, VOCATION, CODE, DESCRIPTION, IMMEUBLE,
                ETAGE, CATEGORIE, TYPE, "Type présentation", "Orientation", "Catégorie de charge",
                "Entité de gestion du syndic", "Adresse", NOTES, PRIX_PROV_HT, PRIX_DEFI_HT, SURFACE_PROV, SURFACE_DEFI,
                TVA, "Niveau de remise 1", "Niveau de remise 2", "Avance min.", "Loyer mensuel HT", "Nombre de caution",
                "Titré", "Date enregistrement", "Notes Titre foncier", "Référence électricité",
                "Arrêté du compteur gaz", "Indisponible pour la vente sur le web",
                "Indisponible pour la location sur le web", "Superficie intérieure (m²)", "Superficie murs (m²)",
                "Superficie parties communes (m²)", "Contenance", "Anomalies"));
        for (int n = 1; n <= 3; n++) {
            String p = "Sous article " + n + " ";
            entetes.addAll(List.of(p + "code", p + "catégorie code", p + "type code", p + "prix m² Prov. HT",
                    p + "prix m² Defi. HT", p + "Superficie Prov. (m²)", p + "Superficie Defi. (m²)", p + "TVA",
                    p + "affecté par défaut"));
        }
        return entetes;
    }

    private MvcResult modele(String type) throws Exception {
        return mockMvc.perform(get(MODELE, type).param("trancheId", c.tranche().getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static List<List<String>> premiereFeuille(Map<String, List<List<String>>> classeur) {
        return classeur.values().iterator().next();
    }

    /** Valeurs de la colonne {@code entete} sur les lignes de données de la première feuille. */
    private static List<String> colonne(Map<String, List<List<String>>> classeur, String entete) {
        List<List<String>> lignes = premiereFeuille(classeur);
        int index = lignes.get(0).indexOf(entete);
        assertThat(index).as("colonne " + entete).isNotNegative();
        return lignes.stream().skip(1).filter(l -> l.size() > index).map(l -> l.get(index)).toList();
    }

    @Test
    void r1_4_modeleArticles_enTetesOmranExacts() throws Exception {
        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(modele(ARTICLES).getResponse()
                .getContentAsByteArray());

        assertThat(premiereFeuille(classeur).get(0)).containsAll(ENTETES_ARTICLES);
    }

    @Test
    void r1_1_modeleArticles_nomDeFichierStockArticles() throws Exception {
        String disposition = modele(ARTICLES).getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);

        assertThat(disposition).contains("_STOCK_ARTICLES_").contains(".xlsx");
    }

    @Test
    void r1_1_modeleArticles_preRempliAvecLesArticlesDeLaTranche() throws Exception {
        StkItem article = sousArticles.principal(c, "MOD-" + c.jeton(), c.tranche());

        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(modele(ARTICLES).getResponse()
                .getContentAsByteArray());

        assertThat(colonne(classeur, PIVOT)).contains(article.getCode());
    }

    @Test
    void r1_4_modeleArticles_feuillesDAideDesCodes() throws Exception {
        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(modele(ARTICLES).getResponse()
                .getContentAsByteArray());

        List<String> cellulesDAide = classeur.values().stream().skip(1)
                .flatMap(List::stream).flatMap(List::stream).toList();
        assertAll(
                () -> assertThat(classeur).as("feuille de données + feuilles d'aide").hasSizeGreaterThan(1),
                () -> assertThat(cellulesDAide).contains(c.categorie().getCode(), c.type().getCode()));
    }

    @Test
    void r1_5_modeleSousArticles_enTetes_sansNatureNiPresentationNiContenance() throws Exception {
        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(modele(SOUS_ARTICLES).getResponse()
                .getContentAsByteArray());

        assertThat(premiereFeuille(classeur).get(0)).containsAll(ENTETES_COMMUNS_SOUS_ARTICLES)
                .doesNotContain(NATURE, "Type présentation", "Contenance", "Sous article 1 code");
    }

    @Test
    void r1_5_modeleSousArticles_nomEtPreRemplissage() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, "MODS-" + c.jeton(), null);

        MvcResult resultat = modele(SOUS_ARTICLES);

        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(resultat.getResponse().getContentAsByteArray());
        assertAll(
                () -> assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .contains("_STOCK_SOUS_ARTICLES_").contains(".xlsx"),
                () -> assertThat(colonne(classeur, PIVOT)).contains(sous.getCode()));
    }
}
