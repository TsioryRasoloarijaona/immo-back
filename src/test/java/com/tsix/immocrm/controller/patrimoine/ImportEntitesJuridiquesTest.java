package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.FixtureImportGenerique;
import com.tsix.immocrm.fixtures.FixtureImportGenerique.Generique;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.entier;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.lignes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADDED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.FORME_JURIDIQUE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.MODIFIED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PAYS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.VILLE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.id;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.seule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des entités juridiques ({@code entites-juridiques}) — FEATURE-patrimoine-import-generique.md §1.1, §1.3, §1.4,
 * §4 (V4–V10) et décisions du manager du 25/10 (cellule vide = intact, pivot = code seul) et du 26/10 (route
 * {@code /api/import-patrimoine/{type}}). Sans mapping : en-têtes du fichier = libellés cibles (écart E7).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportEntitesJuridiquesTest {

    private static final String LIBELLE = "Entite importee ";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportGenerique fixture;

    private Generique g;

    @BeforeEach
    void setUp() {
        g = fixture.preparer("P9EE");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(g);
    }

    // ------------------------------------------------------------------ outils

    private Map<String, Object> valide(String code) {
        return colonnes(CODE, code, DESCRIPTION, LIBELLE + code, FORME_JURIDIQUE, "SARL", VILLE, "Casablanca");
    }

    private List<String> cinqCodes() {
        List<String> codes = new ArrayList<>();
        for (int k = 1; k <= 5; k++) {
            codes.add(g.code("EI" + k));
        }
        return codes;
    }

    private List<Map<String, Object>> cinqLignes(List<String> codes) {
        return codes.stream().map(this::valide).toList();
    }

    private String executer(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(ENTITES, simulation, fichier(lignes), null, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Map<String, Object> enBase(String code) {
        return seule(fixture.entites(code), "entité " + code);
    }

    // ------------------------------------------------------------------ V4 / V5 simulation et écriture

    @Test
    void v4_cinqEntitesJustes_simulation_cinqCreations_rienEnBase_aucuneTrace() throws Exception {
        List<String> codes = cinqCodes();
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_ENTITE);

        String bilan = executer(true,cinqLignes(codes));

        assertAll(
                () -> assertThat(entier(bilan, "creations")).as(bilan).isEqualTo(5),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(codes.stream().mapToInt(c -> fixture.entites(c).size()).sum()).isZero(),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_ENTITE) - ajoutsAvant).isZero());
    }

    @Test
    void v5_cinqEntitesJustes_reel_creeesActives_uneTraceAddedChacune_messageOmran() throws Exception {
        List<String> codes = cinqCodes();
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_ENTITE);

        String bilan = executer(false,cinqLignes(codes));

        assertAll(
                () -> assertThat(entier(bilan, "creations")).as(bilan).isEqualTo(5),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(message(bilan, 2)).startsWith("Création de l'entité juridique : "),
                () -> assertThat(codes).hasSize(5).allSatisfy(c -> assertThat(enBase(c))
                        .containsEntry("IsActive", 1)
                        .containsEntry("Description", LIBELLE + c)
                        .containsEntry("LegalForm", "SARL")),
                () -> assertThat(codes).hasSize(5).allSatisfy(c -> assertThat(
                        fixture.audit(id(enBase(c)), ADDED, TYPE_ENTITE)).isEqualTo(1)),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_ENTITE) - ajoutsAvant).as("témoin de V4")
                        .isEqualTo(5));
    }

    @Test
    void r0_2_simulationParDefaut_rienEcrit() throws Exception {
        String code = g.code("ESIM");

        String bilan = executer(null,List.of(valide(code)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.entites(code)).isEmpty());
    }

    // ------------------------------------------------------------------ V6 rollback complet

    @Test
    void v6_ligne3Sur5EnErreur_bilanCinqLignes_rienEcrit() throws Exception {
        List<String> codes = cinqCodes();
        List<Map<String, Object>> lignes = new ArrayList<>(cinqLignes(codes));
        Map<String, Object> fautive = valide(codes.get(2));
        fautive.put(DESCRIPTION, null);
        lignes.set(2, fautive);
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_ENTITE);

        String bilan = executer(false,lignes);

        assertAll(
                () -> assertThat(lignes(bilan)).as(bilan).hasSize(5),
                () -> assertThat(resultat(bilan, 4)).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(codes.stream().mapToInt(c -> fixture.entites(c).size()).sum()).isZero(),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_ENTITE) - ajoutsAvant).isZero());
    }

    // ------------------------------------------------------------------ V7 réimport

    @Test
    void v7_memeFichierReimporte_cinqInchangees_aucuneTraceModified() throws Exception {
        List<String> codes = cinqCodes();
        byte[] classeur = fichier(cinqLignes(codes));
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_ENTITE);
        String premier = corps(mockMvc.perform(importer(ENTITES, false, classeur, null, connecte(g.admin())))
                .andExpect(status().isOk()).andReturn());
        assertThat(ecrit(premier)).as("témoin : premier import écrit — " + premier).isTrue();
        int modificationsAvant = fixture.auditDuTest(g, MODIFIED, TYPE_ENTITE);

        String bilan = corps(mockMvc.perform(importer(ENTITES, false, classeur, null, connecte(g.admin())))
                .andExpect(status().isOk()).andReturn());

        assertAll(
                () -> assertThat(entier(bilan, "inchangees")).as(bilan).isEqualTo(5),
                () -> assertThat(resultat(bilan, 2)).isEqualTo(INCHANGEE),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_ENTITE) - ajoutsAvant).as("témoin").isEqualTo(5),
                () -> assertThat(fixture.auditDuTest(g, MODIFIED, TYPE_ENTITE) - modificationsAvant).isZero());
    }

    // ------------------------------------------------------------------ V8 cellule vide, colonne absente

    @Test
    void v8_celluleVideEtColonneNonMappee_champsIntacts() throws Exception {
        String code = g.code("EVID");
        CfgCompany e = fixture.entite(g, code, true);
        fixture.definir("CfgCompany", e.getId(), "LegalForm", "SA");
        fixture.definir("CfgCompany", e.getId(), "City", "Rabat");

        String bilan = executer(false,List.of(colonnes(CODE, code, DESCRIPTION, "Nouvelle description",
                FORME_JURIDIQUE, null)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(enBase(code)).containsEntry("Description", "Nouvelle description")
                        .containsEntry("LegalForm", "SA")
                        .containsEntry("City", "Rabat"));
    }

    // ------------------------------------------------------------------ §1.4.3 recherche de l'existant

    @Test
    void r1_4_3_codeEnCasseDifferente_miseAJourDeLExistant_pasDeDoublon() throws Exception {
        String code = g.code("ECAS");
        fixture.entite(g, code, true);

        String bilan = executer(false,List.of(colonnes(CODE, code.toLowerCase(Locale.ROOT), DESCRIPTION, "Renommee")));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(enBase(code)).containsEntry("Description", "Renommee"));
    }

    // ------------------------------------------------------------------ V9 références

    @Test
    void v9_paysParDescriptionEnCasseDifferente_resolu() throws Exception {
        String code = g.code("EPAY");
        AdmCountry pays = fixture.pays(g, "PAYS-" + g.jeton());
        Map<String, Object> l = valide(code);
        l.put(PAYS, pays.getDescription().toUpperCase(Locale.ROOT));

        String bilan = executer(false,List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(String.valueOf(enBase(code).get("AdmCountryId"))).isEqualToIgnoringCase(
                        pays.getId().toString()));
    }

    @Test
    void v9_paysParCode_resolu() throws Exception {
        String code = g.code("EPYC");
        AdmCountry pays = fixture.pays(g, "PAYC-" + g.jeton());
        Map<String, Object> l = valide(code);
        l.put(PAYS, pays.getCode());

        String bilan = executer(false,List.of(l));

        assertThat(String.valueOf(enBase(code).get("AdmCountryId"))).as(bilan).isEqualToIgnoringCase(
                pays.getId().toString());
    }

    @Test
    void v9_paysInconnu_erreurDeLigneInexistente_rienEcrit() throws Exception {
        String code = g.code("EPIN");
        Map<String, Object> l = valide(code);
        l.put(PAYS, "PAYS-INCONNU-" + g.jeton());

        String bilan = executer(false,List.of(l));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("est inexistente dans notre référenciel"),
                () -> assertThat(fixture.entites(code)).isEmpty());
    }

    // ------------------------------------------------------------------ V10 code répété

    @Test
    void v10_codeRepeteDansLeFichier_deuxiemeLigneEnErreur_nommeLaLigneDeLaPremiere() throws Exception {
        String code = g.code("EREP");

        String bilan = executer(true,List.of(valide(code), valide(code)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(resultat(bilan, 3)).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 3)).contains("Le code " + code + " apparaît déjà à la ligne 2 du fichier."));
    }

    // ------------------------------------------------------------------ §1.3 en-tête inconnu

    @Test
    void decision4_enTeteInconnu_avertissement_importPossible() throws Exception {
        String code = g.code("EINC");
        Map<String, Object> l = valide(code);
        l.put("Colonne inconnue " + g.jeton(), "x");

        String bilan = executer(false,List.of(l));

        List<String> avertissements = JsonPath.read(bilan, "$.avertissements");
        assertAll(
                () -> assertThat(avertissements).anySatisfy(a -> assertThat(a).contains("Colonne inconnue " + g.jeton())),
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.entites(code)).hasSize(1));
    }
}
