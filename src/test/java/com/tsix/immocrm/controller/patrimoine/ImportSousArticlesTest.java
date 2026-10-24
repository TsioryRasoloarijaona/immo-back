package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine.Import;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.SOUS_ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ARTICLE_PARENT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ETAGE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.IMMEUBLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.OBJET_SOUS_ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_DEFI_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TOUJOURS_AFFECTE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des sous-articles — FEATURE-patrimoine-import-export.md §1.5 (format {@code ImportUnderItems}, pivot, article
 * parent, toujours affecté, « Manque le code de… »), §5 I7 (audit {@code Yes.Stk.Building.UnderItem}) et I9.
 * Sans {@code @Transactional} : l'import ouvre sa propre transaction ; tout est effacé en {@code @AfterEach}.
 * La colonne {@code Vocation} est omise (vocation interdite au service pour un sous-article rattaché : Question).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportSousArticlesTest {

    private static final String AJOUT = "Added";
    private static final String PROBLEME_PARENT = "Il y a un problème avec le code article parent";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportPatrimoine fixture;

    private Import i;

    @BeforeEach
    void setUp() {
        i = fixture.preparer("P9S");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(i);
    }

    private String code(String suffixe) {
        return suffixe + "-" + i.jeton();
    }

    /** Sous-article valide : 10 m², 5 000 HT/m², TVA 0,2 ; parent = code d'article ou {@code null} (libre). */
    private Map<String, Object> valide(String code, String parent) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put(PIVOT, code);
        l.put(TRANCHE, i.tranche().getCode());
        l.put(CODE, code);
        l.put(DESCRIPTION, "Parking " + code);
        l.put(IMMEUBLE, i.c().immeuble().getCode());
        l.put(ETAGE, i.c().etage().getCode());
        l.put(CATEGORIE, i.c().categorieSous().getCode());
        l.put(TYPE, i.c().typeSous().getCode());
        l.put(SURFACE_PROV, BigDecimal.TEN);
        l.put(SURFACE_DEFI, BigDecimal.TEN);
        l.put(PRIX_DEFI_HT, new BigDecimal("5000"));
        l.put(TVA, new BigDecimal("0.2"));
        l.put(ARTICLE_PARENT, parent);
        l.put(TOUJOURS_AFFECTE, null);
        return l;
    }

    @SafeVarargs
    private static byte[] fichier(Map<String, Object>... lignes) {
        List<String> entetes = new ArrayList<>(lignes[0].keySet());
        List<List<Object>> valeurs = List.of(lignes).stream()
                .map(l -> entetes.stream().map(l::get).toList())
                .toList();
        return ClasseurDeTest.xlsx(entetes, valeurs);
    }

    private String importer(byte[] fichier, boolean simulation) throws Exception {
        return corps(mockMvc.perform(requete(SOUS_ARTICLES, i.trancheId(), simulation, fichier, connecte(i.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Map<String, Object> seul(String code) {
        List<Map<String, Object>> trouves = fixture.articles(i, code);
        assertThat(trouves).as("sous-article " + code).hasSize(1);
        return trouves.get(0);
    }

    private void erreurAvecMessage(Map<String, Object> ligne, String attendu) throws Exception {
        String bilan = importer(fichier(ligne), true);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(attendu));
    }

    // ------------------------------------------------------------------ création

    @Test
    void r1_5_creationLibre_sansParent_sousArticleLibre_auditSousArticle() throws Exception {
        String code = code("LIB");

        String bilan = importer(fichier(valide(code, null)), false);

        Map<String, Object> s = seul(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(s).containsEntry("IsUnderItem", 1).containsEntry("ParentId", null),
                () -> assertThat(String.valueOf(s.get("StkItemCategoryId"))).isEqualToIgnoringCase(
                        i.c().categorieSous().getId().toString()),
                () -> assertThat(fixture.audit(UUID.fromString((String) s.get("PKey")), AJOUT, OBJET_SOUS_ARTICLE))
                        .isEqualTo(1));
    }

    @Test
    void r1_5_creationAvecArticleParent_rattache() throws Exception {
        StkItem parent = fixture.principal(i, code("PAR"));
        String code = code("RAT");

        String bilan = importer(fichier(valide(code, parent.getCode())), false);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(CREATION),
                () -> assertThat(String.valueOf(seul(code).get("ParentId"))).isEqualToIgnoringCase(
                        parent.getId().toString()));
    }

    @Test
    void r1_5_toujoursAffecteAvecParent_ecrit() throws Exception {
        StkItem parent = fixture.principal(i, code("PARA"));
        String code = code("TAF");
        Map<String, Object> l = valide(code, parent.getCode());
        l.put(TOUJOURS_AFFECTE, "Oui");

        importer(fichier(l), false);

        assertThat(seul(code)).containsEntry("AlwaysAffected", 1);
    }

    // ------------------------------------------------------------------ mise à jour

    @Test
    void r1_5_pivotDUnSousArticleExistant_miseAJour() throws Exception {
        StkItem existant = fixture.sousArticle(i, code("EX"), null);
        Map<String, Object> l = new LinkedHashMap<>();
        l.put(PIVOT, existant.getCode());
        l.put(DESCRIPTION, "Parking renomme");

        String bilan = importer(fichier(l), false);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(MISE_A_JOUR),
                () -> assertThat(seul(existant.getCode())).containsEntry(DESCRIPTION, "Parking renomme"));
    }

    // ------------------------------------------------------------------ I9 et messages OMRAN

    @Test
    void i9_pivotDUnArticlePrincipal_messageOmran() throws Exception {
        StkItem article = fixture.principal(i, code("ART"));

        erreurAvecMessage(valide(article.getCode(), null), "Il existe un article avec le même pivot");
    }

    @Test
    void r1_5_articleParentIntrouvable_messageOmran() throws Exception {
        erreurAvecMessage(valide(code("SPI"), "INCONNU-" + i.jeton()), PROBLEME_PARENT);
    }

    @Test
    void r1_5_articleParentEstUnSousArticle_messageOmran() throws Exception {
        StkItem autreSous = fixture.sousArticle(i, code("SPS"), null);

        erreurAvecMessage(valide(code("SPX"), autreSous.getCode()), PROBLEME_PARENT);
    }

    @Test
    void r1_5_toujoursAffecteSansParent_messageDuService() throws Exception {
        Map<String, Object> l = valide(code("TSP"), null);
        l.put(TOUJOURS_AFFECTE, "Oui");

        erreurAvecMessage(l, "Vous ne pouvez pas toujours affecter un sous article sans article parent");
    }

    @Test
    void r1_5_categorieVide_manqueLeCodeDeLaCategorie() throws Exception {
        Map<String, Object> l = valide(code("SCAT"), null);
        l.put(CATEGORIE, null);

        erreurAvecMessage(l, "Manque le code de la catégorie");
    }

    @Test
    void r1_5_typeVide_manqueLeCodeDuType() throws Exception {
        Map<String, Object> l = valide(code("STYP"), null);
        l.put(TYPE, null);

        erreurAvecMessage(l, "Manque le code du type");
    }

    @Test
    void r1_5_categorieDArticlePrincipal_refusee() throws Exception {
        Map<String, Object> l = valide(code("SCP"), null);
        l.put(CATEGORIE, i.c().categorie().getCode());

        String bilan = importer(fichier(l), true);

        assertThat(resultat(bilan, 2)).isEqualTo(ERREUR);
    }

    // ------------------------------------------------------------------ tout ou rien

    @Test
    void r1_3_secondeLigneEnErreur_rienEcrit() throws Exception {
        Map<String, Object> bonne = valide(code("OK"), null);
        Map<String, Object> mauvaise = valide(code("KO"), "INCONNU-" + i.jeton());

        String bilan = importer(fichier(bonne, mauvaise), false);

        assertAll(
                () -> assertThat(resultat(bilan, 3)).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.nombreArticles(i)).isZero());
    }
}
