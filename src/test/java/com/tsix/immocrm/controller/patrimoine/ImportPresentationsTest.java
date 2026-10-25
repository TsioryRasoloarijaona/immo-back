package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Contenance;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine;
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
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PRESENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import des types de présentation — FEATURE-patrimoine-import-autres.md §0 (P1–P3, I15), §3.1 ; import-export §2.1
 * ({@code PresentationType.cs:ImportVerif} l.254-307). Pas de {@code trancheId} : le fichier porte la tranche de chaque
 * ligne. Lignes Excel : 2 = première donnée. Sans {@code @Transactional} : l'import ouvre sa propre transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPresentationsTest {

    private static final String INTROUVABLE = "La tranche saisie est introuvable dans notre référenciel";
    private static final String DESCRIPTION_AVANT = "Presentation avant";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;
    @Autowired private FixtureImportPatrimoine base;

    private Autres a;
    private Contenance c1;
    private Contenance c2;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BP");
        c1 = fixture.contenance(a, "CT1-" + a.jeton());
        c2 = fixture.contenance(a, "CT2-" + a.jeton());
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    // ------------------------------------------------------------------ outils

    private String code(String suffixe) {
        return suffixe + "-" + a.jeton();
    }

    private UUID tranche() {
        return a.i().trancheId();
    }

    private String codeTranche() {
        return a.i().tranche().getCode();
    }

    /** Une colonne par contenance du test ; {@code null} = cellule vide. */
    private Map<String, Object> ligne(String tranche, String code, String description, Object n1, Object n2) {
        return colonnes(TRANCHE, tranche, CODE, code, DESCRIPTION, description, c1.libelle(), n1, c2.libelle(), n2);
    }

    @SafeVarargs
    private String importer(Boolean simulation, Map<String, Object>... lignes) throws Exception {
        return corps(mockMvc.perform(requete(PRESENTATIONS, null, simulation, fichier(List.of(lignes)),
                        connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    @SafeVarargs
    private String ecrire(Map<String, Object>... lignes) throws Exception {
        return importer(false, lignes);
    }

    private String seule(UUID trancheId, String code) {
        List<Map<String, Object>> trouvees = fixture.presentations(trancheId, code);
        assertThat(trouvees).as("présentation " + code).hasSize(1);
        return (String) trouvees.get(0).get("PKey");
    }

    /** Présentation existante, validée, avec ses contenances (nombre &gt; 0 seulement). */
    private UUID existante(String code, int n1, int n2) {
        UUID id = fixture.presentation(tranche(), code, DESCRIPTION_AVANT);
        if (n1 > 0) {
            fixture.contenanceDePresentation(id, c1.id(), n1);
        }
        if (n2 > 0) {
            fixture.contenanceDePresentation(id, c2.id(), n2);
        }
        return id;
    }

    // ------------------------------------------------------------------ P1 rapprochement (tranche, code)

    @Test
    void p1_aucuneLigneDeCeCode_creation_contenancesPositivesSeules() throws Exception {
        String code = code("PRS");

        String bilan = ecrire(ligne(codeTranche(), code, "Presentation " + code, 2, null));

        String id = seule(tranche(), code);
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(message(bilan, 2)).contains("Création :"),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(fixture.presentations(tranche(), code)).singleElement()
                        .satisfies(p -> assertThat(p).containsEntry("Description", "Presentation " + code)),
                () -> assertThat(fixture.nombres(id, c1.id())).containsExactly(2),
                () -> assertThat(fixture.nombres(id, c2.id())).isEmpty());
    }

    @Test
    void p1_uneLigneDeCeCode_miseAJour_descriptionEtContenance() throws Exception {
        String code = code("MAJ");
        UUID id = existante(code, 2, 0);

        String bilan = ecrire(ligne(codeTranche(), code, "Presentation apres", 3, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(message(bilan, 2)).contains("Mise à jour :"),
                () -> assertThat(fixture.presentations(tranche(), code)).singleElement()
                        .satisfies(p -> assertThat(p).containsEntry("Description", "Presentation apres")),
                () -> assertThat(fixture.nombres(id.toString(), c1.id())).containsExactly(3));
    }

    @Test
    void p1_plusieursLignesDeCeCode_erreurOmran() throws Exception {
        String code = code("DBL");
        fixture.presentation(tranche(), code, DESCRIPTION_AVANT);
        fixture.presentation(tranche(), code, DESCRIPTION_AVANT);

        String bilan = importer(true, ligne(codeTranche(), code, "Presentation " + code, 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Il existe plusieurs enregistrement avec le même code"));
    }

    @Test
    void p1_ligneIdentiqueALaBase_inchangee_pasDeModification_aucunAudit() throws Exception {
        String code = code("IDEM");
        UUID id = existante(code, 2, 0);
        int auditAvant = fixture.audit(id.toString());

        String bilan = ecrire(ligne(codeTranche(), code, DESCRIPTION_AVANT, 2, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(INCHANGEE),
                () -> assertThat(message(bilan, 2)).contains("Pas de modification"),
                () -> assertThat(fixture.audit(id.toString())).isEqualTo(auditAvant));
    }

    // ------------------------------------------------------------------ I15 contenance à 0 ou vide

    @Test
    void i15_contenanceA0SurLigneExistante_retiree_autreGardee() throws Exception {
        String code = code("ZERO");
        UUID id = existante(code, 2, 4);

        String bilan = ecrire(ligne(codeTranche(), code, DESCRIPTION_AVANT, 0, 4));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(fixture.nombres(id.toString(), c1.id())).isEmpty(),
                () -> assertThat(fixture.nombres(id.toString(), c2.id())).containsExactly(4));
    }

    @Test
    void i15_contenanceVideSurLigneExistante_retiree() throws Exception {
        String code = code("VIDE");
        UUID id = existante(code, 2, 4);

        String bilan = ecrire(ligne(codeTranche(), code, DESCRIPTION_AVANT, null, 4));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(fixture.nombres(id.toString(), c1.id())).isEmpty(),
                () -> assertThat(fixture.nombres(id.toString(), c2.id())).containsExactly(4));
    }

    // ------------------------------------------------------------------ P2 messages OMRAN

    @Test
    void p2_trancheVide_manqueLeTranche_problemesDimport() throws Exception {
        String bilan = importer(true, ligne(null, code("SANSTR"), "Presentation", 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le tranche").contains("Problèmes dimport"));
    }

    @Test
    void p2_trancheInconnue_introuvable_etCodeNonVerifiable() throws Exception {
        String bilan = importer(true, ligne("TR-INCONNUE-" + a.jeton(), code("TRX"), "Presentation", 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(INTROUVABLE)
                        .contains("Il est impossible de vérifier le code si la tranche n'est pas vérifiée"));
    }

    @Test
    void p2_codeVide_manqueLeCode() throws Exception {
        String bilan = importer(true, ligne(codeTranche(), null, "Presentation", 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque le code"));
    }

    @Test
    void p2_descriptionVideEnCreation_manqueLaDescription_rienEcrit() throws Exception {
        String code = code("SANSDESC");

        String bilan = ecrire(ligne(codeTranche(), code, null, 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Manque la description"),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.presentations(tranche(), code)).isEmpty());
    }

    // ------------------------------------------------------------------ P3 visibilité R4

    @Test
    void p3_trancheDUneEntiteInactive_introuvable_rienEcrit() throws Exception {
        CfgTranche invisible = base.trancheInvisible(a.i());
        fixture.suivreTranche(a, invisible.getId());
        String code = code("INV");

        String bilan = ecrire(ligne(invisible.getCode(), code, "Presentation " + code, 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains(INTROUVABLE),
                () -> assertThat(fixture.presentations(invisible.getId(), code)).isEmpty());
    }

    // ------------------------------------------------------------------ §3.1 traduction

    @Test
    void r3_1_trancheParLibelle_casseIgnoree_creation() throws Exception {
        String code = code("LIB");

        String bilan = ecrire(ligne(a.i().tranche().getDescription().toLowerCase(Locale.ROOT), code, "Presentation " + code,
                1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.presentations(tranche(), code)).hasSize(1));
    }

    @Test
    void r3_1_sansTrancheId_fichierSurDeuxTranches_chaqueLigneDansSaTranche() throws Exception {
        CfgTranche autre = base.autreTranche(a.i());
        fixture.suivreTranche(a, autre.getId());
        String code = code("MULTI");

        String bilan = ecrire(ligne(codeTranche(), code, "Presentation " + code, 1, null),
                ligne(autre.getCode(), code, "Presentation " + code, 2, null));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.presentations(tranche(), code)).hasSize(1),
                () -> assertThat(fixture.presentations(autre.getId(), code)).hasSize(1));
    }

    @Test
    void i13_colonneSansContenanceConnue_avertissementColonneIgnoree_importPossible() throws Exception {
        String code = code("INC");
        Map<String, Object> l = ligne(codeTranche(), code, "Presentation " + code, 1, null);
        l.put("Contenance inconnue " + a.jeton(), 5);

        String bilan = ecrire(l);

        List<String> avertissements = JsonPath.read(bilan, "$.avertissements");
        assertAll(
                () -> assertThat(avertissements).anySatisfy(av -> assertThat(av).contains("Contenance inconnue " + a.jeton())),
                () -> assertThat(ecrit(bilan)).isTrue(),
                () -> assertThat(fixture.presentations(tranche(), code)).hasSize(1));
    }

    // ------------------------------------------------------------------ I5 / I6 moteur repris

    @Test
    void i6_simulationParDefaut_creationAnnoncee_rienEnBase() throws Exception {
        String code = code("SIM");

        String bilan = importer(null, ligne(codeTranche(), code, "Presentation " + code, 1, null));

        assertAll(
                () -> assertThat((Boolean) JsonPath.read(bilan, "$.simulation")).isTrue(),
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.presentations(tranche(), code)).isEmpty());
    }

    @Test
    void i5_uneLigneEnErreur_toutAnnule() throws Exception {
        String juste = code("OK");

        String bilan = ecrire(ligne(codeTranche(), juste, "Presentation " + juste, 1, null),
                ligne(codeTranche(), code("KO"), null, 1, null));

        assertAll(
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.presentations(tranche(), juste)).isEmpty());
    }

    // ------------------------------------------------------------------ modèle

    @Test
    void modele_sansTranche_feuilleTypesPresentationEtFeuilleProjets() throws Exception {
        byte[] contenu = mockMvc.perform(get(MODELE, PRESENTATIONS).with(connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        Map<String, List<List<String>>> classeur = ClasseurDeTest.lire(contenu);
        List<List<String>> donnees = classeur.get("TYPES_PRESENTATION");
        List<List<String>> projets = classeur.get("PROJETS");
        assertAll(
                () -> assertThat(classeur.keySet()).as("feuilles").startsWith("TYPES_PRESENTATION").contains("PROJETS"),
                () -> assertThat(donnees).isNotEmpty(),
                () -> assertThat(donnees.get(0)).startsWith(TRANCHE, CODE, DESCRIPTION).contains(c1.libelle(), c2.libelle()),
                () -> assertThat(projets).isNotEmpty(),
                () -> assertThat(projets).anySatisfy(r -> assertThat(r).contains(codeTranche())));
    }
}
