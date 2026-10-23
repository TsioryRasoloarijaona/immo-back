package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.entier;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.lignes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ETAGE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.IMMEUBLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.NATURE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.NOTES;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.OBJET_ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.OBJET_SOUS_ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_DEFI_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_PROV_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des articles — FEATURE-patrimoine-import-export.md §1.3 (une transaction, bilan, simulation), §1.4 (format
 * {@code ImportItems}, pivot, rapprochements, messages OMRAN), §1.2 (cellules), §5 I5–I14, §7 (durée 400 lignes),
 * décision Q4 (TVA &gt; 1 ÷ 100). Lignes Excel : 1 = en-têtes, 2 = première donnée.
 * Sans {@code @Transactional} : l'import ouvre sa propre transaction (rollback réel, audit écrit au commit).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportArticlesTest {

    private static final String AJOUT = "Added";
    private static final String INTROUVABLE = "introuvable dans notre référenciel";
    private static final String DESCRIPTION_IMPORT = "Article importe ";
    private static final String TITRE_DATE = "TitleDate";
    private static final String CREATIONS = "creations";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportPatrimoine fixture;

    private Import i;
    private String nature;
    private String vocation;

    @BeforeEach
    void setUp() {
        i = fixture.preparer("P9A");
        nature = fixture.codeNatureBatiment(i);
        vocation = fixture.codeVocationVente(i);
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(i);
    }

    // ------------------------------------------------------------------ outils

    private String code(String suffixe) {
        return suffixe + "-" + i.jeton();
    }

    /** Ligne de création complète et valide : 63 m², 10 000 HT/m², TVA 0,2. */
    private Map<String, Object> valide(String code) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put(PIVOT, code);
        l.put(TRANCHE, i.tranche().getCode());
        l.put(NATURE, nature);
        l.put(VOCATION, vocation);
        l.put(CODE, code);
        l.put(DESCRIPTION, DESCRIPTION_IMPORT + code);
        l.put(IMMEUBLE, i.c().immeuble().getCode());
        l.put(ETAGE, i.c().etage().getCode());
        l.put(CATEGORIE, i.c().categorie().getCode());
        l.put(TYPE, i.c().type().getCode());
        l.put(PRIX_PROV_HT, new BigDecimal("10000"));
        l.put(PRIX_DEFI_HT, new BigDecimal("10000"));
        l.put(TVA, new BigDecimal("0.2"));
        l.put(SURFACE_PROV, new BigDecimal("63"));
        l.put(SURFACE_DEFI, new BigDecimal("63"));
        return l;
    }

    /** Classeur : en-têtes = clés de la première ligne ; valeur absente ou {@code null} = cellule vide. */
    @SafeVarargs
    private static byte[] fichier(Map<String, Object>... lignes) {
        return fichier(List.of(lignes));
    }

    private static byte[] fichier(List<Map<String, Object>> lignes) {
        List<String> entetes = new ArrayList<>(lignes.get(0).keySet());
        List<List<Object>> valeurs = lignes.stream()
                .map(l -> entetes.stream().map(l::get).toList())
                .toList();
        return ClasseurDeTest.xlsx(entetes, valeurs);
    }

    private static Map<String, Object> colonnes(Object... clesValeurs) {
        Map<String, Object> l = new LinkedHashMap<>();
        for (int k = 0; k < clesValeurs.length; k += 2) {
            l.put((String) clesValeurs[k], clesValeurs[k + 1]);
        }
        return l;
    }

    private String importer(byte[] fichier, Boolean simulation) throws Exception {
        return corps(mockMvc.perform(requete(ARTICLES, i.trancheId(), simulation, fichier, connecte(i.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String simuler(byte[] fichier) throws Exception {
        return importer(fichier, true);
    }

    private String ecrire(byte[] fichier) throws Exception {
        return importer(fichier, false);
    }

    private Map<String, Object> seulArticle(String code) {
        List<Map<String, Object>> trouves = fixture.articles(i, code);
        assertThat(trouves).as("article " + code).hasSize(1);
        return trouves.get(0);
    }

    private List<Map<String, Object>> cinqLignesValides() {
        return IntStream.rangeClosed(1, 5).mapToObj(n -> valide(code("ART" + n))).toList();
    }

    // ------------------------------------------------------------------ I6 / I7 simulation et écriture

    @Test
    void i6_simulation_cinqLignesJustes_cinqCreations_rienEnBase() throws Exception {
        int auditAvant = fixture.auditDuTest(i, AJOUT);

        String bilan = simuler(fichier(cinqLignesValides()));

        assertAll(
                () -> assertThat(lignes(bilan)).hasSize(5).extracting(l -> l.get("resultat"))
                        .containsOnly(CREATION),
                () -> assertThat(entier(bilan, "lignesLues")).isEqualTo(5),
                () -> assertThat(entier(bilan, CREATIONS)).isEqualTo(5),
                () -> assertThat(entier(bilan, "erreurs")).isZero(),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(message(bilan, 2)).contains("Création de l'article : " + code("ART1")),
                () -> assertThat(fixture.nombreArticles(i)).as("articles en base").isZero(),
                () -> assertThat(fixture.auditDuTest(i, AJOUT)).as("audit").isEqualTo(auditAvant));
    }

    @Test
    void r1_1_simulationParDefaut_rienEcrit() throws Exception {
        String bilan = importer(fichier(valide(code("DEF"))), null);

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(bilan, "$.simulation")).isTrue(),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.nombreArticles(i)).isZero());
    }

    @Test
    void i7_ecriture_cinqArticles_cinqLignesAuditAdded() throws Exception {
        String bilan = ecrire(fichier(cinqLignesValides()));

        List<String> ids = IntStream.rangeClosed(1, 5).mapToObj(n -> (String) seulArticle(code("ART" + n)).get("PKey"))
                .toList();
        assertAll(
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(entier(bilan, CREATIONS)).isEqualTo(5),
                () -> assertThat(ids).hasSize(5).allSatisfy(id -> assertThat(
                        fixture.audit(UUID.fromString(id), AJOUT, OBJET_ARTICLE)).as("audit " + id).isEqualTo(1)));
    }

    @Test
    void i7_ecriture_articleCreeAvecLesValeursDuFichier() throws Exception {
        String code = code("VAL");

        ecrire(fichier(valide(code)));

        Map<String, Object> a = seulArticle(code);
        assertAll(
                () -> assertThat(a).containsEntry(DESCRIPTION, DESCRIPTION_IMPORT + code),
                () -> assertThat(String.valueOf(a.get("StkBuildingId"))).isEqualToIgnoringCase(
                        i.c().immeuble().getId().toString()),
                () -> assertThat(String.valueOf(a.get("StkFloorId"))).isEqualToIgnoringCase(
                        i.c().etage().getId().toString()),
                () -> assertThat(String.valueOf(a.get("StkItemCategoryId"))).isEqualToIgnoringCase(
                        i.c().categorie().getId().toString()),
                () -> assertThat(String.valueOf(a.get("StkItemTypeId"))).isEqualToIgnoringCase(
                        i.c().type().getId().toString()),
                () -> assertThat((BigDecimal) a.get("ComSaleUnitCostHT")).isEqualByComparingTo("10000"),
                () -> assertThat((BigDecimal) a.get("Area")).isEqualByComparingTo("63"),
                () -> assertThat((BigDecimal) a.get("Vat")).isEqualByComparingTo("0.2"));
    }

    // ------------------------------------------------------------------ I5 / I8 tout ou rien

    /** I5 : la 3e ligne de données (ligne Excel 4) est refusée par le service (surface 0) → tout est annulé. */
    @Test
    void i5_ligne3EnErreur_bilanCinqLignes_rienEnBase_aucunAudit() throws Exception {
        List<Map<String, Object>> lignes = new ArrayList<>(cinqLignesValides());
        lignes.get(2).put(SURFACE_PROV, BigDecimal.ZERO);
        int auditAvant = fixture.auditDuTest(i, AJOUT);

        String bilan = ecrire(fichier(lignes));

        assertAll(
                () -> assertThat(lignes(bilan)).hasSize(5),
                () -> assertThat(resultat(bilan, 4)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 4)).contains("Problèmes d'import de l'article : " + code("ART3"))
                        .contains("strictement supérieure à 0"),
                () -> assertThat(entier(bilan, "erreurs")).isEqualTo(1),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.nombreArticles(i)).as("articles en base").isZero(),
                () -> assertThat(fixture.auditDuTest(i, AJOUT)).as("audit").isEqualTo(auditAvant));
    }

    /** I8 : deux pivots distincts, même Code → la 2e ligne refusée par l'unicité du service, rollback. */
    @Test
    void i8_deuxLignesMemeCode_secondeEnErreur_rollback() throws Exception {
        String doublon = code("DBL");
        Map<String, Object> premiere = valide(code("P1"));
        premiere.put(CODE, doublon);
        Map<String, Object> seconde = valide(code("P2"));
        seconde.put(CODE, doublon);

        String bilan = ecrire(fichier(premiere, seconde));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isNotEqualTo(ERREUR),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 3)).contains(doublon),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.nombreArticles(i)).isZero());
    }

    // ------------------------------------------------------------------ I9 et messages de pivot

    @Test
    void i9_pivotDUnSousArticle_messageOmran() throws Exception {
        StkItem sous = fixture.sousArticle(i, code("SA"), null);

        String bilan = simuler(fichier(valide(sous.getCode())));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Il existe un sous article avec le même pivot"));
    }

    @Test
    void r1_4_pivotVide_manqueLePivot() throws Exception {
        Map<String, Object> l = valide(code("SANSPIVOT"));
        l.put(PIVOT, null);

        String bilan = simuler(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le pivot"));
    }

    @Test
    void r1_4_codeDeReferentielIntrouvable_messageOmranNommantLeCode() throws Exception {
        Map<String, Object> l = valide(code("CATX"));
        l.put(CATEGORIE, "CAT-INCONNUE-" + i.jeton());

        String bilan = ecrire(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Le code CAT-INCONNUE-" + i.jeton() + " saisi est " + INTROUVABLE),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.nombreArticles(i)).isZero());
    }

    @Test
    void r1_4_trancheDuFichierAutreQueLaTrancheChoisie_introuvable() throws Exception {
        Map<String, Object> l = valide(code("TRX"));
        l.put(TRANCHE, fixture.autreTranche(i).getCode());

        String bilan = simuler(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Le code tranche saisi est " + INTROUVABLE));
    }

    @Test
    void r1_4_trancheParLibelle_casseIgnoree_acceptee() throws Exception {
        Map<String, Object> l = valide(code("TRLIB"));
        l.put(TRANCHE, i.tranche().getDescription().toLowerCase(Locale.ROOT));

        String bilan = simuler(fichier(l));

        assertThat(resultat(bilan, 2)).isEqualTo(CREATION);
    }

    @Test
    void r1_4_natureAutreQueBatiment_erreur() throws Exception {
        Map<String, Object> l = valide(code("NAT"));
        l.put(NATURE, fixture.natureAutre(i).getCode());

        String bilan = simuler(fichier(l));

        assertThat(resultat(bilan, 2)).isEqualTo(ERREUR);
    }

    @Test
    void r1_4_codeVide_codeEgalAuPivot() throws Exception {
        String pivot = code("PIV");
        Map<String, Object> l = valide(pivot);
        l.put(CODE, null);

        String bilan = ecrire(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(CREATION),
                () -> assertThat(fixture.articles(i, pivot)).hasSize(1));
    }

    // ------------------------------------------------------------------ I10 article occupé

    @Test
    void i10_articleOccupe_prixChange_erreurDuService() throws Exception {
        StkItem occupe = fixture.occupe(i, code("OCC"));

        String bilan = simuler(fichier(colonnes(PIVOT, occupe.getCode(), PRIX_DEFI_HT, new BigDecimal("20000"))));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("DOS-" + occupe.getCode()));
    }

    @Test
    void i10_articleOccupe_ligneSansPrix_miseAJour() throws Exception {
        StkItem occupe = fixture.occupe(i, code("OCCD"));

        String bilan = simuler(fichier(colonnes(PIVOT, occupe.getCode(), DESCRIPTION, "Nouvelle description")));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(MISE_A_JOUR),
                () -> assertThat(message(bilan, 2)).contains("Mise à jour de l'article : " + occupe.getCode()));
    }

    // ------------------------------------------------------------------ I11 / I12 mise à jour

    @Test
    void i11_ligneIdentiqueALaBase_inchangee_aucuneLigneDAudit() throws Exception {
        StkItem article = fixture.principal(i, code("IDEM"));

        String bilan = ecrire(fichier(colonnes(PIVOT, article.getCode(), CODE, article.getCode(),
                DESCRIPTION, article.getDescription())));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(INCHANGEE),
                () -> assertThat(message(bilan, 2)).contains("l'article : " + article.getCode() + " n'a pas été modifié"),
                () -> assertThat(entier(bilan, "inchangees")).isEqualTo(1),
                () -> assertThat(fixture.audit(article.getId(), "Modified", OBJET_ARTICLE)).isZero());
    }

    @Test
    void i12_celluleVideEnMiseAJour_champIntact() throws Exception {
        StkItem article = fixture.principal(i, code("VIDE"), item -> item.setNotes("Avant"));

        String bilan = ecrire(fichier(colonnes(PIVOT, article.getCode(), DESCRIPTION, "Nouvelle description",
                NOTES, null)));

        Map<String, Object> a = seulArticle(article.getCode());
        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(MISE_A_JOUR),
                () -> assertThat(a).containsEntry(DESCRIPTION, "Nouvelle description").containsEntry(NOTES, "Avant"));
    }

    // ------------------------------------------------------------------ I13 / I14 en-têtes et référentiels

    @Test
    void i13_colonneInconnue_avertissement_importPossible() throws Exception {
        String code = code("INC");
        Map<String, Object> l = valide(code);
        l.put("Colonne inconnue P9A", "x");

        String bilan = ecrire(fichier(l));

        List<String> avertissements = JsonPath.read(bilan, "$.avertissements");
        assertAll(
                () -> assertThat(avertissements).anySatisfy(a -> assertThat(a).contains("Colonne inconnue P9A")),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(fixture.articles(i, code)).hasSize(1));
    }

    @Test
    void i14_immeubleEtageCategorieParLibelle_casseDifferente_resolus() throws Exception {
        String code = code("LIB");
        Map<String, Object> l = valide(code);
        l.put(IMMEUBLE, i.c().immeuble().getDescription().toLowerCase(Locale.ROOT));
        l.put(ETAGE, i.c().etage().getDescription().toUpperCase(Locale.ROOT));
        l.put(CATEGORIE, i.c().categorie().getDescription().toLowerCase(Locale.ROOT));

        String bilan = ecrire(fichier(l));

        Map<String, Object> a = seulArticle(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(CREATION),
                () -> assertThat(String.valueOf(a.get("StkBuildingId"))).isEqualToIgnoringCase(
                        i.c().immeuble().getId().toString()),
                () -> assertThat(String.valueOf(a.get("StkFloorId"))).isEqualToIgnoringCase(
                        i.c().etage().getId().toString()),
                () -> assertThat(String.valueOf(a.get("StkItemCategoryId"))).isEqualToIgnoringCase(
                        i.c().categorie().getId().toString()));
    }

    @Test
    void i14_typeParLibelle_introuvable_codeSeul() throws Exception {
        Map<String, Object> l = valide(code("TYPLIB"));
        l.put(TYPE, i.c().type().getDescription());

        String bilan = simuler(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(INTROUVABLE));
    }

    // ------------------------------------------------------------------ §1.2 cellules, Q4, colonnes inversées

    @Test
    void q4_tvaEnPourcentage_diviseePar100() throws Exception {
        String code = code("TVA");
        Map<String, Object> l = valide(code);
        l.put(TVA, new BigDecimal("20"));

        ecrire(fichier(l));

        assertThat((BigDecimal) seulArticle(code).get("Vat")).isEqualByComparingTo("0.2");
    }

    @Test
    void r1_4_indisponibleVenteWebNon_disponibleVenteWeb() throws Exception {
        String code = code("WEB");
        Map<String, Object> l = valide(code);
        l.put("Indisponible pour la vente sur le web", "Non");

        ecrire(fichier(l));

        assertThat(seulArticle(code)).containsEntry("IsAvailableForSaleInWebSite", 1);
    }

    @Test
    void r1_2_nombresEnTexteAuFormatFrancais_lus() throws Exception {
        String code = code("NBR");
        Map<String, Object> l = valide(code);
        l.put(SURFACE_DEFI, "63,5");
        l.put(PRIX_DEFI_HT, "1 234,56");

        ecrire(fichier(l));

        Map<String, Object> a = seulArticle(code);
        assertAll(
                () -> assertThat((BigDecimal) a.get("Area")).isEqualByComparingTo("63.5"),
                () -> assertThat((BigDecimal) a.get("ComSaleUnitCostHT")).isEqualByComparingTo("1234.56"));
    }

    @Test
    void r1_2_dateEnTexteJjMmAaaa_lue() throws Exception {
        String code = code("DAT");
        Map<String, Object> l = valide(code);
        l.put("Date du Titre foncier", "01/02/2024");

        ecrire(fichier(l));

        assertThat(seulArticle(code)).containsEntry(TITRE_DATE, "2024-02-01");
    }

    @Test
    void r1_2_valeurIllisible_erreurDeLigneNommantLaColonne() throws Exception {
        Map<String, Object> l = valide(code("ILL"));
        l.put(SURFACE_PROV, "abc");

        String bilan = simuler(fichier(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Colonne " + SURFACE_PROV).contains("abc")
                        .contains("illisible"));
    }

    @Test
    void r1_2_feuilleNommee_lueAuLieuDeLaPremiere() throws Exception {
        String code = code("FEU");
        Map<String, Object> l = valide(code);
        List<String> entetes = new ArrayList<>(l.keySet());
        Map<String, List<List<Object>>> feuilles = new LinkedHashMap<>();
        feuilles.put("AIDE", ClasseurDeTest.avecEntetes(List.of("Code", "Libellé"), List.of(ClasseurDeTest.ligne("X", "Y"))));
        feuilles.put("DONNEES", ClasseurDeTest.avecEntetes(entetes, List.of(entetes.stream().map(l::get).toList())));
        MockMultipartHttpServletRequestBuilder requete = requete(ARTICLES, i.trancheId(), true,
                ClasseurDeTest.xlsx(feuilles), connecte(i.admin()));
        requete.param("feuille", "DONNEES");

        String bilan = corps(mockMvc.perform(requete).andExpect(status().isOk()).andReturn());

        assertThat(resultat(bilan, 2)).isEqualTo(CREATION);
    }

    // ------------------------------------------------------------------ §1.4 sous-articles de la ligne

    @Test
    void r1_4_sousArticle1DeLaLigne_creeRattacheAuditSousArticle() throws Exception {
        String code = code("PAR");
        String codeSous = code("SA1");
        Map<String, Object> l = valide(code);
        l.put("Sous article 1 code", codeSous);
        l.put("Sous article 1 catégorie code", i.c().categorieSous().getCode());
        l.put("Sous article 1 type code", i.c().typeSous().getCode());
        l.put("Sous article 1 Superficie Prov. (m²)", BigDecimal.TEN);
        l.put("Sous article 1 Superficie Defi. (m²)", BigDecimal.TEN);
        l.put("Sous article 1 prix m² Defi. HT", new BigDecimal("5000"));
        l.put("Sous article 1 TVA", new BigDecimal("0.2"));

        String bilan = ecrire(fichier(l));

        String parentId = (String) seulArticle(code).get("PKey");
        Map<String, Object> sous = seulArticle(codeSous);
        assertAll(
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(sous).containsEntry("IsUnderItem", 1),
                () -> assertThat(String.valueOf(sous.get("ParentId"))).isEqualToIgnoringCase(parentId),
                () -> assertThat(fixture.audit(UUID.fromString((String) sous.get("PKey")), AJOUT, OBJET_SOUS_ARTICLE))
                        .isEqualTo(1));
    }

    // ------------------------------------------------------------------ §7 / Q13 durée

    @Test
    void q13_quatreCentsLignes_moinsDe60Secondes() throws Exception {
        List<Map<String, Object>> lignes = IntStream.rangeClosed(1, 400).mapToObj(n -> valide(code("D" + n))).toList();
        byte[] classeur = fichier(lignes);
        long debut = System.nanoTime();

        String bilan = simuler(classeur);

        Duration duree = Duration.ofNanos(System.nanoTime() - debut);
        assertAll(
                () -> assertThat(entier(bilan, CREATIONS)).isEqualTo(400),
                () -> assertThat(duree).isLessThan(Duration.ofSeconds(60)));
    }
}
