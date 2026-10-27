package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.entier;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.ADDED;
import static com.tsix.immocrm.fixtures.FixtureLotissements.TYPE_IMPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import de lots ({@code ImportLotLand}) — FEATURE-patrimoine-lotissements.md §1.5, règles L12, L13, L14 ; réponse 2 du
 * manager (27/10) : type {@code lotissements} de l'assistant générique {@code /api/import-patrimoine/{type}}, tranche imposée.
 * L'import ouvre sa propre transaction : jeu validé pour de bon ({@link FixtureLotissements}), rollback compté en base et
 * dans {@code AdmWebHistorics}, tout effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportLotissementsTest {

    private static final String INTROUVABLE = "saisi est introuvable dans notre référenciel";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CI");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    // ------------------------------------------------------------------ outils

    /** Ligne de création complète et valide : vocation du jeu, catégorie / type TER, 1 000 m², TVA 0,2. */
    private Map<String, Object> valide(String code) {
        return colonnes(PIVOT, code, CODE, code, DESCRIPTION, FixtureLotissements.description(code),
                VOCATION, j.codeVocation(), CATEGORIE, j.ter().codeCategorie(), TYPE, j.ter().codeType(),
                SURFACE_PROV, new BigDecimal("1000"), SURFACE_DEFI, new BigDecimal("1000"), TVA, new BigDecimal("0.2"));
    }

    private List<Map<String, Object>> cinqLots() {
        return new ArrayList<>(IntStream.rangeClosed(1, 5).mapToObj(n -> valide(j.code("LI" + n))).toList());
    }

    private String importer(UUID trancheId, Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(requete(TYPE_IMPORT, trancheId, simulation, fichier(lignes), connecte(j.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private String ecrire(List<Map<String, Object>> lignes) throws Exception {
        return importer(j.tranche(), false, lignes);
    }

    private String simuler(List<Map<String, Object>> lignes) throws Exception {
        return importer(j.tranche(), true, lignes);
    }

    private int lotsDuJeu() {
        return fixture.compter("SELECT COUNT(*) FROM StkItem WHERE StkHierarchyId = ? AND Code LIKE ?",
                j.tranche().toString(), "%" + j.jeton());
    }

    // ------------------------------------------------------------------ L12

    /** L12 : 5 lots justes en simulation ⇒ 5 CREATION, 0 en base, 0 audit. */
    @Test
    void l12_cinqLotsJustes_simulation_5Creations_rienEnBase_aucunAudit() throws Exception {
        String bilan = simuler(cinqLots());

        assertAll(
                () -> assertThat(entier(bilan, "creations")).as(bilan).isEqualTo(5),
                () -> assertThat(resultat(bilan, 2)).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(lotsDuJeu()).isZero(),
                () -> assertThat(fixture.auditDuJeu(j, ADDED)).isZero());
    }

    /** L12 : import réel ⇒ 5 lots (nature TER, entité « article ») et 5 lignes Added de type lot. */
    @Test
    void l12_cinqLotsJustes_reel_5Lots_5LignesAdded() throws Exception {
        String bilan = ecrire(cinqLots());

        List<UUID> premier = fixture.ids(j.tranche(), j.code("LI1"));
        assertAll(
                () -> assertThat(entier(bilan, "creations")).as(bilan).isEqualTo(5),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(message(bilan, 2)).contains("Création du lot : " + j.code("LI1")),
                () -> assertThat(lotsDuJeu()).isEqualTo(5),
                () -> assertThat(premier).hasSize(1),
                () -> assertThat(fixture.entiteArticle(premier.get(0))).isTrue(),
                () -> assertThat(fixture.auditDuJeu(j, ADDED)).isEqualTo(5));
    }

    /** §1.5 : colonne Tranche facultative ; égale à la tranche choisie ⇒ acceptée. */
    @Test
    void l1_5_colonneTrancheEgaleALaTrancheChoisie_creation() throws Exception {
        Map<String, Object> l = valide(j.code("LTC"));
        l.put(TRANCHE, "TR-" + j.jeton());

        String bilan = simuler(List.of(l));

        assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION);
    }

    // ------------------------------------------------------------------ L13 erreurs et rollback

    /** L13 : pivot d'un article bâti de la tranche ⇒ erreur de ligne ; la ligne juste n'est pas écrite (tout ou rien). */
    @Test
    void l13_pivotDUnArticleDeLaTranche_erreur_rollbackComplet() throws Exception {
        String bati = j.code("LAB");
        UUID article = fixture.article(j, bati);
        String juste = j.code("LJU");

        String bilan = ecrire(List.of(valide(juste), valide(bati)));

        assertAll(
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 3)).contains("Ce code désigne un article : utilisez l'import des articles"),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.ids(j.tranche(), juste)).isEmpty(),
                () -> assertThat(fixture.ligne(article)).containsEntry("Description", FixtureLotissements.description(bati)),
                () -> assertThat(fixture.auditDuJeu(j, ADDED)).isZero());
    }

    @Test
    void l13_vocationIntrouvable_erreur_rollbackComplet() throws Exception {
        String juste = j.code("LJV");
        Map<String, Object> l = valide(j.code("LVI"));
        l.put(VOCATION, "VOC-INCONNUE-" + j.jeton());

        String bilan = ecrire(List.of(valide(juste), l));

        assertAll(
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 3)).contains("Le code VOC-INCONNUE-" + j.jeton() + " " + INTROUVABLE),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(lotsDuJeu()).isZero(),
                () -> assertThat(fixture.auditDuJeu(j, ADDED)).isZero());
    }

    @Test
    void l13_colonneTrancheDifferenteDeLaTrancheChoisie_erreur() throws Exception {
        Map<String, Object> l = valide(j.code("LTD"));
        l.put(TRANCHE, "TR2-" + j.jeton());

        String bilan = ecrire(List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Le code tranche saisi est introuvable dans notre référenciel"),
                () -> assertThat(lotsDuJeu()).isZero(),
                () -> assertThat(fixture.ids(j.autreTranche(), j.code("LTD"))).isEmpty());
    }

    /** L13 / §1.5 : sans trancheId ⇒ 400 « Vous devez sélectionner une Tranche ! ». */
    @Test
    void l13_sansTrancheId_400_messageOmran() throws Exception {
        mockMvc.perform(requete(TYPE_IMPORT, null, false, fichier(List.of(valide(j.code("LSA")))), connecte(j.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vous devez sélectionner une Tranche !")));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItem WHERE Code = ?", j.code("LSA"))).isZero();
    }

    @Test
    void l1_5_trancheInvisible_404() throws Exception {
        UUID inactive = fixture.trancheInactive(j);

        mockMvc.perform(requete(TYPE_IMPORT, inactive, false, fichier(List.of(valide(j.code("LTI")))),
                        connecte(j.admin())))
                .andExpect(status().isNotFound());
    }

    /** L1 : import réservé à l'Administrateur. */
    @Test
    void l1_commercial_403() throws Exception {
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(j.admin());

        mockMvc.perform(requete(TYPE_IMPORT, j.tranche(), false, fichier(List.of(valide(j.code("LCO")))), commercial))
                .andExpect(status().isForbidden());

        assertThat(lotsDuJeu()).isZero();
    }

    @Test
    void l1_5_pivotVide_manqueLePivot() throws Exception {
        Map<String, Object> l = valide(j.code("LPV"));
        l.put(PIVOT, null);

        String bilan = simuler(List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le pivot"));
    }

    @ParameterizedTest(name = "{0} vide → {1}")
    @CsvSource(delimiter = '|', value = {
            "Vocation|Manque le code de la vocation",
            "Catégorie|Manque le code de la catégorie",
            "Type|Manque le code du type"})
    void l1_5_colonneVideEnCreation_messageOmran(String colonne, String attendu) throws Exception {
        Map<String, Object> l = valide(j.code("LCV"));
        l.put(colonne, null);

        String bilan = simuler(List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(attendu));
    }

    // ------------------------------------------------------------------ L14 réimport

    /** L14 : réimport du même fichier ⇒ 5 INCHANGEE, aucune nouvelle ligne d'audit. */
    @Test
    void l14_reimportDuMemeFichier_5Inchangees_aucunAudit() throws Exception {
        List<Map<String, Object>> lignes = cinqLots();
        ecrire(lignes);
        int auditAvant = fixture.compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectLabel LIKE ?",
                "%" + j.jeton() + "%");

        String bilan = ecrire(lignes);

        int auditApres = fixture.compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectLabel LIKE ?",
                "%" + j.jeton() + "%");
        assertAll(
                () -> assertThat(entier(bilan, "inchangees")).as(bilan).isEqualTo(5),
                () -> assertThat(resultat(bilan, 2)).isEqualTo(INCHANGEE),
                () -> assertThat(auditApres).isEqualTo(auditAvant),
                () -> assertThat(lotsDuJeu()).isEqualTo(5));
    }

    // ------------------------------------------------------------------ modèle

    /** §1.5 / §2 : modèle pré-rempli avec les lots de la tranche (OMRAN {@code importLotLand.ListItems}). */
    @Test
    void modele_preRempliAvecLesLotsDeLaTranche() throws Exception {
        String code = j.code("LMO");
        fixture.lot(j, code);

        MvcResult resultat = mockMvc.perform(get(MODELE, TYPE_IMPORT).param("trancheId", j.tranche().toString())
                        .with(connecte(j.admin())))
                .andExpect(status().isOk())
                .andReturn();

        List<List<String>> donnees = ClasseurDeTest.lire(resultat.getResponse().getContentAsByteArray())
                .values().iterator().next();
        assertAll(
                () -> assertThat(donnees).isNotEmpty(),
                () -> assertThat(donnees.get(0)).contains(PIVOT, CODE, VOCATION, CATEGORIE, TYPE),
                () -> assertThat(donnees).anySatisfy(r -> assertThat(r).contains(code)));
    }
}
