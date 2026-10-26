package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportGenerique;
import com.tsix.immocrm.fixtures.FixtureImportGenerique.Generique;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.fixtures.AppelsImportGenerique.DETAIL;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.analyse;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.avec;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.importer;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.indexPropose;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.lireColonnes;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.mappingPropose;
import static com.tsix.immocrm.fixtures.AppelsImportGenerique.propriete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ACTIVE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ADDED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.MODIFIED;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROGRAMME;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROJETS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TRANCHES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_PROGRAMME;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TYPE_TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.seule;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité (règle 6) de l'import générique — FEATURE-patrimoine-import-generique.md §1.2 (colonne « Active » de
 * l'entité refusée en 400 avant toute écriture, analyse et import, en-tête reconnu ou mapping manuel), §1.4.2 et §4 V2,
 * V9, V13 : une entité inactive, un programme ou une tranche d'entité inactive ne sont jamais écrits par l'import.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportGeneriqueRevueTest {

    private static final String MESSAGE_ACTIVE = "La colonne « Active » ne peut pas être importée";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportGenerique fixture;

    private Generique g;

    @BeforeEach
    void setUp() {
        g = fixture.preparer("P9ER");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(g);
    }

    private Map<String, Object> entite(String code) {
        return seule(fixture.entites(code), "entité " + code);
    }

    private String bilan(String type, List<Map<String, Object>> lignes) throws Exception {
        return corps(mockMvc.perform(importer(type, false, fichier(lignes), null, connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    // ------------------------------------------------------------------ V2 colonne « Active »

    @ParameterizedTest(name = "simulation={0}")
    @ValueSource(booleans = {true, false})
    void v2_enTeteActiveReconnu_400_rienEcrit_entiteExistanteInchangee(boolean simulation) throws Exception {
        String existant = g.code("EACT");
        String nouveau = g.code("ENEW");
        fixture.entite(g, existant, true);
        byte[] classeur = fichier(List.of(
                colonnes(CODE, existant, DESCRIPTION, "Renommee", ACTIVE, "Non"),
                colonnes(CODE, nouveau, DESCRIPTION, "Nouvelle", ACTIVE, "Oui")));
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_ENTITE);
        int modificationsAvant = fixture.auditDuTest(g, MODIFIED, TYPE_ENTITE);

        mockMvc.perform(importer(ENTITES, simulation, classeur, null, connecte(g.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_ACTIVE)));

        assertAll(
                () -> assertThat(entite(existant)).containsEntry("IsActive", 1).containsEntry("Description",
                        "Entite " + existant),
                () -> assertThat(fixture.entites(nouveau)).isEmpty(),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_ENTITE) - ajoutsAvant).isZero(),
                () -> assertThat(fixture.auditDuTest(g, MODIFIED, TYPE_ENTITE) - modificationsAvant).isZero());
    }

    @Test
    void v2_activeOuiSurUneEntiteInactive_400_resteInactive() throws Exception {
        String code = g.code("EINA");
        fixture.entite(g, code, false);

        mockMvc.perform(importer(ENTITES, false, fichier(List.of(colonnes(CODE, code, ACTIVE, "Oui"))), null,
                        connecte(g.admin())))
                .andExpect(status().isBadRequest());

        assertThat(entite(code)).containsEntry("IsActive", 0);
    }

    /** Mapping manuel : une colonne du fichier nommée autrement, mappée sur la cible « Active ». */
    @Test
    void v2_mappingManuelSurActive_400_rienEcrit() throws Exception {
        String code = g.code("EMAN");
        fixture.entite(g, code, true);
        byte[] classeur = fichier(List.of(colonnes(CODE, code, DESCRIPTION, "Renommee", "Statut ouvert", "Non")));
        String catalogue = corps(mockMvc.perform(lireColonnes(ENTITES, connecte(g.admin())))
                .andExpect(status().isOk()).andReturn());
        String analyse = corps(mockMvc.perform(analyse(ENTITES, classeur, null, connecte(g.admin())))
                .andExpect(status().isOk()).andReturn());
        String mapping = avec(mappingPropose(analyse), ACTIVE, propriete(catalogue, ACTIVE, "isActive"), 2);

        mockMvc.perform(importer(ENTITES, false, classeur, mapping, connecte(g.admin())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_ACTIVE)));

        assertThat(entite(code)).containsEntry("IsActive", 1).containsEntry("Description", "Entite " + code);
    }

    /** L'analyse ne propose jamais « Active » mappée : soit 400, soit un mapping proposé qui la laisse à -1. */
    @Test
    void v2_analyseDUnFichierAvecActive_400OuActiveNonMappee() throws Exception {
        String code = g.code("EANA");
        byte[] classeur = fichier(List.of(colonnes(CODE, code, DESCRIPTION, "Nouvelle", ACTIVE, "Non")));

        MvcResult resultat = mockMvc.perform(analyse(ENTITES, classeur, null, connecte(g.admin()))).andReturn();

        int statut = resultat.getResponse().getStatus();
        assertAll(
                () -> assertThat(statut).as(corps(resultat)).isIn(200, 400),
                () -> assertThat(statut == 400 || indexPropose(corps(resultat), ACTIVE) < 0)
                        .as("mapping proposé : " + corps(resultat)).isTrue(),
                () -> assertThat(fixture.entites(code)).isEmpty());
    }

    // ------------------------------------------------------------------ V9 / V13 entité inactive

    @Test
    void v13_programmeSurUneEntiteInactive_erreurDeLigne_rienEcrit() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTI"), false);
        String code = g.code("PINA");
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_PROGRAMME);

        String bilan = bilan(PROJETS, List.of(colonnes(ENTITE, inactive.getCode(), CODE, code, DESCRIPTION, "Programme")));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.projets(code)).isEmpty(),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_PROGRAMME) - ajoutsAvant).isZero());
    }

    @Test
    void v9_programmeDUneEntiteInactiveParDescription_erreurDeLigne_rienEcrit() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTD"), false);
        String code = g.code("PIND");

        String bilan = bilan(PROJETS, List.of(colonnes(ENTITE, inactive.getDescription(), CODE, code,
                DESCRIPTION, "Programme")));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.projets(code)).isEmpty());
    }

    /** Un programme invisible (entité inactive) n'est ni modifié ni rattaché à une entité visible par son code. */
    @Test
    void v13_programmeExistantDUneEntiteInactive_jamaisModifie() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTP"), false);
        String code = g.code("PEXI");
        CfgProject invisible = fixture.projet(g, code, inactive);

        String bilan = bilan(PROJETS, List.of(colonnes(ENTITE, g.entite().getCode(), CODE, code,
                DESCRIPTION, "Programme deplace")));

        Map<String, Object> p = seule(fixture.projets(code), "programme " + code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(p).containsEntry("Description", invisible.getDescription()),
                () -> assertThat(String.valueOf(p.get("CfgCompanyId"))).isEqualToIgnoringCase(inactive.getId().toString()),
                () -> assertThat(fixture.auditDuTest(g, MODIFIED, TYPE_PROGRAMME)).isZero());
    }

    @Test
    void v13_trancheSurUnProgrammeDEntiteInactive_erreurDeLigne_rienEcrit() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTT"), false);
        CfgProject projet = fixture.projet(g, g.code("PRJT"), inactive);
        String code = g.code("TINA");
        int ajoutsAvant = fixture.auditDuTest(g, ADDED, TYPE_TRANCHE);

        String bilan = bilan(TRANCHES, List.of(colonnes(PROGRAMME, projet.getCode(), CODE, code, DESCRIPTION, "Tranche")));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(fixture.tranches(code)).isEmpty(),
                () -> assertThat(fixture.auditDuTest(g, ADDED, TYPE_TRANCHE) - ajoutsAvant).isZero());
    }

    @Test
    void v13_trancheExistanteInvisible_jamaisModifiee() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTV"), false);
        CfgProject projet = fixture.projet(g, g.code("PRJV"), inactive);
        String code = g.code("TINV");
        CfgTranche invisible = fixture.tranche(g, code, projet);

        String bilan = bilan(TRANCHES, List.of(colonnes(PROGRAMME, g.projet().getCode(), CODE, code,
                DESCRIPTION, "Tranche modifiee")));

        Map<String, Object> t = seule(fixture.tranches(code), "tranche " + code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(t).containsEntry("Description", invisible.getDescription()),
                () -> assertThat(String.valueOf(t.get("CfgProjectId"))).isEqualToIgnoringCase(projet.getId().toString()),
                () -> assertThat(fixture.auditDuTest(g, MODIFIED, TYPE_TRANCHE)).isZero());
    }

    /** Feuilles d'aide du modèle des programmes : l'entité active du test y est, jamais une entité inactive. */
    @Test
    void r1_4_2_modeleDesProgrammes_feuillesDAide_sansEntiteInactive() throws Exception {
        CfgCompany inactive = fixture.entite(g, g.code("ENTM"), false);

        MvcResult resultat = mockMvc.perform(get(MODELE, PROJETS).with(connecte(g.admin())))
                .andExpect(status().isOk())
                .andReturn();

        List<String> cellules = ClasseurDeTest.lire(resultat.getResponse().getContentAsByteArray()).values().stream()
                .flatMap(List::stream).flatMap(List::stream).toList();
        assertThat(cellules).contains(g.entite().getCode()).doesNotContain(inactive.getCode());
    }
}
