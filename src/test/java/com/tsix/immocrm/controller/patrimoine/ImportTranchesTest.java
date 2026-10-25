package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ACTIVE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADDED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADRESSE_1;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.MODIFIED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROGRAMME;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TRANCHES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.id;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.seule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des tranches ({@code tranches}) — FEATURE-patrimoine-import-generique.md §1.1, §1.4.6 (« La tranche : X n'a pas
 * été modifiée »), §4 V12 (colonne non gérée = avertissement, import possible), écart E6 (programme posé en création
 * seulement) ; décisions du manager 25/10 (en-tête inconnu = avertissement) et 26/10.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportTranchesTest {

    private static final String LIBELLE = "Tranche importee ";
    private static final String PROGRAMME_EN_BASE = "CfgProjectId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportGenerique fixture;

    private Generique g;

    @BeforeEach
    void setUp() {
        g = fixture.preparer("P9ET");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(g);
    }

    private String executer(Boolean simulation, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(TRANCHES, simulation, fichier(lignes), null, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private Map<String, Object> enBase(String code) {
        return seule(fixture.tranches(code), "tranche " + code);
    }

    @Test
    void r1_1_creationParCodeDuProgramme_traceAdded() throws Exception {
        String code = g.code("TC");

        String bilan = executer(false, List.of(colonnes(PROGRAMME, g.projet().getCode(), CODE, code,
                DESCRIPTION, LIBELLE + code, ADRESSE_1, "Lot 7")));

        Map<String, Object> t = enBase(code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(String.valueOf(t.get(PROGRAMME_EN_BASE))).isEqualToIgnoringCase(
                        g.projet().getId().toString()),
                () -> assertThat(t).containsEntry("Address", "Lot 7"),
                () -> assertThat(fixture.audit(id(t), ADDED, TYPE_TRANCHE)).isEqualTo(1));
    }

    /** V7 sur une tranche : le premier import (témoin) écrit, le second ne change rien. */
    @Test
    void r1_4_6_v7_reimport_inchangee_messageLaTrancheNAPasEteModifiee() throws Exception {
        String code = g.code("TINC");
        List<Map<String, Object>> lignes = List.of(colonnes(PROGRAMME, g.projet().getCode(), CODE, code,
                DESCRIPTION, LIBELLE + code, ADRESSE_1, "Lot 9"));
        assertThat(ecrit(executer(false, lignes))).as("témoin : premier import écrit").isTrue();

        String bilan = executer(false, lignes);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(INCHANGEE),
                () -> assertThat(message(bilan, 2)).startsWith("La tranche : ").contains(" n'a pas été modifiée"),
                () -> assertThat(fixture.audit(id(enBase(code)), MODIFIED, TYPE_TRANCHE)).isZero());
    }

    /** §1.2 vise la colonne « Active » de l'entité ; « Active » d'une tranche = remise de concrétisation active. */
    @Test
    void r1_2_colonneActiveDUneTranche_nonRefusee_remiseActivee() throws Exception {
        String code = g.code("TACT");
        fixture.tranche(g, code, g.projet());

        MvcResult resultat = mockMvc.perform(importer(TRANCHES, false,
                        fichier(List.of(colonnes(CODE, code, ACTIVE, "Oui"))), null, connecte(g.admin())))
                .andReturn();

        assertAll(
                () -> assertThat(resultat.getResponse().getStatus()).as(corps(resultat)).isEqualTo(200),
                () -> assertThat(resultat(corps(resultat), 2)).isEqualTo(MISE_A_JOUR),
                () -> assertThat(enBase(code)).containsEntry("IsActiveAdvancePercentDiscount", 1));
    }

    @Test
    void decision4_enTeteInconnu_avertissementIgnoree_importPossible() throws Exception {
        String code = g.code("TINC2");
        String inconnu = "Colonne absente d'OMRAN " + g.jeton();

        String bilan = executer(false, List.of(colonnes(PROGRAMME, g.projet().getCode(), CODE, code,
                DESCRIPTION, LIBELLE + code, inconnu, "x")));

        List<String> avertissements = JsonPath.read(bilan, "$.avertissements");
        assertAll(
                () -> assertThat(avertissements).anySatisfy(a -> assertThat(a).contains(inconnu).contains("ignorée")),
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.tranches(code)).hasSize(1));
    }

    /** E6 : le programme d'une tranche ne se modifie pas ; l'import ne le pose qu'en création. */
    @Test
    void e6_programmeDifferentEnMiseAJour_programmeInchange() throws Exception {
        String code = g.code("TPRG");
        fixture.tranche(g, code, g.projet());
        CfgProject autre = fixture.projet(g, g.code("PRJA"), g.entite());

        String bilan = executer(false, List.of(colonnes(PROGRAMME, autre.getCode(), CODE, code,
                DESCRIPTION, "Tranche renommee")));

        assertThat(String.valueOf(enBase(code).get(PROGRAMME_EN_BASE))).as(bilan)
                .isEqualToIgnoringCase(g.projet().getId().toString());
    }
}
