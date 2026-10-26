package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixtureImportGenerique;
import com.tsix.immocrm.fixtures.FixtureImportGenerique.Generique;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADDED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADRESSE_1;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROJETS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_PROGRAMME;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.id;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.seule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des programmes ({@code projets}) — FEATURE-patrimoine-import-generique.md §1.1, §1.4 (références par code ou
 * description sans casse, messages de bilan, message du service tel quel), §4 V8 et V9 ; décisions du manager 25/10 et 26/10.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportProjetsTest {

    private static final String LIBELLE = "Programme importe ";
    private static final String ENTITE_EN_BASE = "CfgCompanyId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportGenerique fixture;

    private Generique g;

    @BeforeEach
    void setUp() {
        g = fixture.preparer("P9EP");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(g);
    }

    private Map<String, Object> valide(String code, Object entite) {
        return colonnes(ENTITE, entite, CODE, code, DESCRIPTION, LIBELLE + code, ADRESSE_1, "12 rue des Arts");
    }

    private String executer(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(PROJETS, simulation, fichier(lignes), null, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Map<String, Object> enBase(String code) {
        return seule(fixture.projets(code), "programme " + code);
    }

    private String entiteDuTest() {
        return g.entite().getId().toString();
    }

    // ------------------------------------------------------------------ V9 référence à l'entité

    @Test
    void v9_entiteParCode_creationDansLEntite_traceAdded() throws Exception {
        String code = g.code("PC");

        String bilan = executer(false, List.of(valide(code, g.entite().getCode())));

        Map<String, Object> p = enBase(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(String.valueOf(p.get(ENTITE_EN_BASE))).isEqualToIgnoringCase(entiteDuTest()),
                () -> assertThat(p).containsEntry("Description", LIBELLE + code).containsEntry("Address", "12 rue des Arts"),
                () -> assertThat(fixture.audit(id(p), ADDED, TYPE_PROGRAMME)).isEqualTo(1));
    }

    @Test
    void v9_entiteParDescriptionEnCasseDifferente_resolue() throws Exception {
        String code = g.code("PD");
        String description = g.entite().getDescription().toUpperCase(Locale.ROOT);

        String bilan = executer(false, List.of(valide(code, description)));

        assertThat(String.valueOf(enBase(code).get(ENTITE_EN_BASE))).as(bilan).isEqualToIgnoringCase(entiteDuTest());
    }

    @Test
    void v9_entiteParCodeEnMinuscules_resolue() throws Exception {
        String code = g.code("PM");

        String bilan = executer(false, List.of(valide(code, g.entite().getCode().toLowerCase(Locale.ROOT))));

        assertThat(String.valueOf(enBase(code).get(ENTITE_EN_BASE))).as(bilan).isEqualToIgnoringCase(entiteDuTest());
    }

    @Test
    void v9_entiteInconnue_erreurDeLigneInexistente_rienEcrit() throws Exception {
        String code = g.code("PI");
        String inconnue = "ENT-INCONNUE-" + g.jeton();

        String bilan = executer(false, List.of(valide(code, inconnue)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("La valeur [").contains(inconnue)
                        .contains("est inexistente dans notre référenciel !"),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.projets(code)).isEmpty());
    }

    // ------------------------------------------------------------------ §1.4.7 message du service

    @Test
    void r1_4_7_creationSansEntite_erreurDeLigne_messageDuService() throws Exception {
        String code = g.code("PSE");

        String bilan = executer(true, List.of(colonnes(CODE, code, DESCRIPTION, LIBELLE + code)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("L'entité est obligatoire."));
    }

    // ------------------------------------------------------------------ §1.4.6 message de mise à jour, V8

    @Test
    void r1_4_6_miseAJour_messageMiseAJourDuProgramme_v8_celluleVideEtColonneAbsenteIntactes() throws Exception {
        String code = g.code("PMAJ");
        CfgProject p = fixture.projet(g, code, g.entite());
        fixture.definir("CfgProject", p.getId(), "Address", "Adresse avant");
        fixture.definir("CfgProject", p.getId(), "City", "Tanger");

        String bilan = executer(false, List.of(colonnes(CODE, code, DESCRIPTION, "Programme renomme", ADRESSE_1, null)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(message(bilan, 2)).startsWith("Mise à jour du programme : "),
                () -> assertThat(enBase(code)).containsEntry("Description", "Programme renomme")
                        .containsEntry("Address", "Adresse avant")
                        .containsEntry("City", "Tanger"));
    }
}
