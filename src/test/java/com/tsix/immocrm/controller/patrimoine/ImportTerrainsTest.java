package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.NATURE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.NOTES;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PAYS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TERRAINS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.VILLE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.VOCATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des terrains (format {@code STOCK_TERRAINS}) — FEATURE-patrimoine-import-autres.md §0 (T1–T5), §1 décision 1,
 * §3.2, décisions du manager 25/10 (2 « Titré » ignorée, 3 pas de tranche) ; import-export §2.2, §0.3 (rapprochement sur les
 * terrains de l'entité) ; {@code ImportLands.cs:ImportVerif} l.635-793 pour les messages. Sans {@code trancheId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportTerrainsTest {

    private static final String DESCRIPTION_IMPORT = "Terrain importe ";
    private static final String AVERTISSEMENTS = "$.avertissements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;

    private Autres a;
    private FixtureTerrains.Contexte c;
    private String vocation;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BT");
        c = fixture.terrains(a);
        vocation = fixture.codeVocationProjet(a);
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    // ------------------------------------------------------------------ outils

    private String code(String suffixe) {
        return suffixe + "-" + a.jeton();
    }

    private UUID entite() {
        return c.entite().getId();
    }

    /** Ligne de création complète et valide : 1 200 m², TVA 0,2. */
    private Map<String, Object> valide(String code) {
        return colonnes(PIVOT, code, NATURE, c.natureTer().getCode(), ENTITE, c.entite().getCode(), PAYS, c.pays().getCode(),
                VILLE, "VIL-" + c.jeton(), CODE, code, DESCRIPTION, DESCRIPTION_IMPORT + code, VOCATION, vocation,
                CATEGORIE, c.categorie().getCode(), TYPE, c.type().getCode(), SURFACE_PROV, new BigDecimal("1200"),
                SURFACE_DEFI, new BigDecimal("1200"), TVA, new BigDecimal("0.2"));
    }

    @SafeVarargs
    private String importer(Boolean simulation, Map<String, Object>... lignes) throws Exception {
        return corps(mockMvc.perform(requete(TERRAINS, null, simulation, fichier(List.of(lignes)), connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    @SafeVarargs
    private String ecrire(Map<String, Object>... lignes) throws Exception {
        return importer(false, lignes);
    }

    @SafeVarargs
    private String simuler(Map<String, Object>... lignes) throws Exception {
        return importer(true, lignes);
    }

    private Map<String, Object> seul(UUID entiteId, String code) {
        List<Map<String, Object>> trouves = fixture.terrainsDe(entiteId, code);
        assertThat(trouves).as("terrain " + code).hasSize(1);
        return trouves.get(0);
    }

    // ------------------------------------------------------------------ T1 rapprochement par entité

    @Test
    void t1_pivotInconnuDansLEntite_creationParTerrainService_auditAdded() throws Exception {
        String code = code("TC");

        String bilan = ecrire(valide(code));

        Map<String, Object> t = seul(entite(), code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(t).containsEntry("Nature", "TER").containsEntry("Description", DESCRIPTION_IMPORT + code),
                () -> assertThat((BigDecimal) t.get("AreaFloor")).isEqualByComparingTo("1200"),
                () -> assertThat(String.valueOf(t.get("StkItemCategoryId"))).isEqualToIgnoringCase(
                        c.categorie().getId().toString()),
                () -> assertThat(fixture.audit((String) t.get("PKey"))).as("audit P0").isPositive());
    }

    @Test
    void t1_pivotDUnTerrainDeLEntite_miseAJour() throws Exception {
        String code = code("TM");
        fixture.terrain(a, c, code);

        String bilan = ecrire(colonnes(PIVOT, code, ENTITE, c.entite().getCode(), DESCRIPTION, "Nouvelle description"));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(seul(entite(), code)).containsEntry("Description", "Nouvelle description"));
    }

    @Test
    void t1_memeCodeDansUneAutreEntite_creationDansLEntiteDeLaLigne_autreIntact() throws Exception {
        FixtureTerrains.Contexte autre = fixture.terrains(a);
        String code = code("TX");
        UUID ailleurs = fixture.terrain(a, autre, code);

        String bilan = ecrire(valide(code));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.terrainsDe(entite(), code)).hasSize(1),
                () -> assertThat(fixture.article(ailleurs)).containsEntry("Description", "Terrain " + code));
    }

    @Test
    void t1_codeDejaPrisDansLEntiteParUnAutreTerrain_erreurDeLigne409DuService() throws Exception {
        String codeA = code("TA");
        String codeB = code("TB");
        fixture.terrain(a, c, codeA);
        fixture.terrain(a, c, codeB);

        String bilan = ecrire(colonnes(PIVOT, codeA, ENTITE, c.entite().getCode(), CODE, codeB));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("déjà pris"),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.terrainsDe(entite(), codeA)).hasSize(1));
    }

    // ------------------------------------------------------------------ T2 terrain occupé

    @Test
    void t2_terrainAvecDossierDeVente_pasDeMiseAJour() throws Exception {
        String code = code("TOCC");
        UUID terrain = fixture.terrain(a, c, code);
        fixture.occuper(a, terrain, code);

        String bilan = simuler(colonnes(PIVOT, code, ENTITE, c.entite().getCode(), DESCRIPTION, "Nouvelle description"));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Pas de mise à jour pour un article ayant un dossier de vente"));
    }

    // ------------------------------------------------------------------ T3 colonnes sans champ

    @Test
    void t3_colonnesOmranSansChampEtTitre_avertissementColonneIgnoree_importPossible() throws Exception {
        String code = code("TIGN");
        Map<String, Object> l = valide(code);
        l.put("Titré", "Oui");
        l.put("Cos", new BigDecimal("1.5"));
        l.put("Avance min.", new BigDecimal("1000"));

        String bilan = ecrire(l);

        List<String> avertissements = JsonPath.read(bilan, AVERTISSEMENTS);
        assertAll(
                () -> assertThat(avertissements).anySatisfy(av -> assertThat(av).contains("Titré"))
                        .anySatisfy(av -> assertThat(av).contains("Cos"))
                        .anySatisfy(av -> assertThat(av).contains("Avance min.")),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(fixture.terrainsDe(entite(), code)).hasSize(1));
    }

    // ------------------------------------------------------------------ T4 pivot d'un bâtiment

    @Test
    void t4_pivotDUnBatimentDeLEntite_erreurDeLigne_batimentIntact() throws Exception {
        String code = code("TBAT");
        UUID batiment = fixture.batiment(a, c, code);

        String bilan = ecrire(valide(code));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.article(batiment)).containsEntry("Description", "Terrain " + code),
                () -> assertThat(fixture.terrainsDe(entite(), code)).hasSize(1));
    }

    @Test
    void r3_2_natureAutreQueTerrain_erreurDeLigne() throws Exception {
        Map<String, Object> l = valide(code("TNAT"));
        l.put(NATURE, "BAT");

        String bilan = simuler(l);

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR);
    }

    // ------------------------------------------------------------------ T5 messages OMRAN gardés

    @ParameterizedTest(name = "{0} vide → {1}")
    @CsvSource(delimiter = '|', value = {
            "Nature|Manque le code de la nature",
            "Entité|Manque le code de l'entité",
            "Vocation|Manque le code de la vocation",
            "Catégorie|Manque le code de la catégorie",
            "Type|Manque le code du type"})
    void t5_colonneVideEnCreation_messageOmran(String colonne, String attendu) throws Exception {
        Map<String, Object> l = valide(code("TV"));
        l.put(colonne, null);

        String bilan = simuler(l);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(attendu));
    }

    @Test
    void t5_pivotEtCodeVides_manqueLeCodeDeLArticle() throws Exception {
        Map<String, Object> l = valide(code("TSC"));
        l.put(PIVOT, null);
        l.put(CODE, null);

        String bilan = simuler(l);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le code de l'article"));
    }

    @Test
    void t5_paysInconnu_lePaysDuTierEstIntrouvable() throws Exception {
        Map<String, Object> l = valide(code("TPAYS"));
        l.put(PAYS, "PAYS-INCONNU-" + a.jeton());

        String bilan = simuler(l);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Le pays du tier est introuvable"));
    }

    @Test
    void t5_villeInconnue_laVilleDuTierEstIntrouvable() throws Exception {
        Map<String, Object> l = valide(code("TVIL"));
        l.put(VILLE, "VIL-INCONNUE-" + a.jeton());

        String bilan = simuler(l);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("La ville du tier est introuvable"));
    }

    @Test
    void r3_2_entiteInactive_erreurDeLigne_rienEcrit() throws Exception {
        CfgCompany inactive = fixture.entiteInactive(a);
        String code = code("TINA");
        Map<String, Object> l = valide(code);
        l.put(ENTITE, inactive.getCode());

        String bilan = ecrire(l);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.terrainsDe(inactive.getId(), code)).isEmpty());
    }

    // ------------------------------------------------------------------ moteur repris (I12, Q4, I6)

    @Test
    void i12_celluleVideEnMiseAJour_champIntact() throws Exception {
        String code = code("TVID");
        UUID terrain = fixture.terrain(a, c, code);
        fixture.notes(terrain, "Avant");

        String bilan = ecrire(colonnes(PIVOT, code, ENTITE, c.entite().getCode(), DESCRIPTION, "Nouvelle description",
                NOTES, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(fixture.article(terrain)).containsEntry("Description", "Nouvelle description")
                        .containsEntry("Notes", "Avant"));
    }

    @Test
    void q4_tvaEnPourcentage_diviseePar100_avertissement() throws Exception {
        String code = code("TTVA");
        Map<String, Object> l = valide(code);
        l.put(TVA, new BigDecimal("20"));

        String bilan = ecrire(l);

        List<String> avertissements = JsonPath.read(bilan, AVERTISSEMENTS);
        assertAll(
                () -> assertThat((BigDecimal) seul(entite(), code).get("Vat")).isEqualByComparingTo("0.2"),
                () -> assertThat(avertissements).isNotEmpty());
    }

    @Test
    void i6_simulationParDefaut_rienEnBase() throws Exception {
        String code = code("TSIM");

        String bilan = importer(null, valide(code));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.terrainsDe(entite(), code)).isEmpty());
    }

    // ------------------------------------------------------------------ modèle STOCK_TERRAINS

    @Test
    void modele_sansTranche_nomStockTerrains_preRempliAvecLesTerrainsVisibles() throws Exception {
        String code = code("TMOD");
        fixture.terrain(a, c, code);

        MvcResult resultat = mockMvc.perform(get(MODELE, TERRAINS).with(connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn();

        List<List<String>> donnees = ClasseurDeTest.lire(resultat.getResponse().getContentAsByteArray())
                .values().iterator().next();
        assertAll(
                () -> assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .contains("STOCK_TERRAINS").contains(".xlsx"),
                () -> assertThat(donnees).isNotEmpty(),
                () -> assertThat(donnees.get(0)).contains(PIVOT, NATURE, ENTITE, CODE, DESCRIPTION),
                () -> assertThat(donnees).anySatisfy(r -> assertThat(r).contains(code)));
    }
}
