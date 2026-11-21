package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePlanification;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.CODE_ABSENTE;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.DETAIL;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.MESSAGE_ABSENTE;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planificationDuProjet;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tachesDuLot;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;

/**
 * N9b, planification d'un projet : R5 (au plus une par projet), R7 (absente → 404 {@code PLANIFICATION_ABSENTE}, tâches d'un
 * lot) et R8 (clone d'un modèle vers le projet, remappages) — FEATURE-n9-planification.md. Routes
 * {@code /api/projet/projets/{projetId}/planification} et {@code /api/projet/lots/{lotId}/taches}.
 * Modèle : intervenants OA (visible), OB (masqué) ; tâches 1 (catégorie du lot), 1.1 (enfant de 1, OA;OB, catégorie sans lot),
 * 2 (prédécesseur « 1|1 », sans catégorie).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationClonageTest {

    private static final String MODELE_ID = "modeleId";
    private static final String CODE = "$.code";
    private static final LocalDate DEBUT_MODELE = LocalDate.of(2026, 3, 2);
    private static final String TACHE = """
            SELECT Code, Description, Notes, Sorting, Depth, Duration, CONVERT(varchar(10), StartDate, 23) AS Debut,
                Predecessors, ResourceNames, CAST(ParentId AS nvarchar(36)) AS Parent,
                CAST(PrjMarketId AS nvarchar(36)) AS Lot, CAST(StlCategoryId AS nvarchar(36)) AS Categorie
            FROM CfgGanttTask WHERE PKey = ?""";
    private static final String PLANIFICATIONS_DU_PROJET = "SELECT COUNT(*) FROM CfgGanttProject WHERE ObjectId = ? AND VersionDate IS NULL";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private Clock horlogeMetier;

    private AppelsPlanification api;
    private String jeton;
    private String codeProjet;
    private UUID projet;
    private UUID lot;
    private UUID modele;
    private UUID oa;
    private UUID ob;
    private UUID t1;
    private UUID categorieLot;
    private UUID categorieSansLot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique("N9E-" + jeton);
        codeProjet = "N9P-" + jeton;
        projet = parite.projet(codeProjet, societe);
        UUID autreProjet = parite.projet("N9Q-" + jeton, societe);
        admin = connecte(parite.administrateur("n9b.clo." + jeton, projet, autreProjet), "Administrateur");

        parite.genresDeLot();
        UUID statut = parite.statut(ENTREPRENEURS, "N9S-" + jeton);
        categorieLot = fixture.categorie("N9C-" + jeton);
        categorieSansLot = fixture.categorie("N9D-" + jeton);
        UUID lotAutreProjet = parite.lot(autreProjet, "N9M-" + jeton, ENTREPRENEURS, statut);
        fixture.categorieDuLot(lotAutreProjet, categorieLot);
        lot = parite.lot(projet, "N9L-" + jeton, ENTREPRENEURS, statut);
        fixture.categorieDuLot(lot, categorieLot);

        modele = fixture.modele("MOD-" + jeton, DEBUT_MODELE);
        fixture.notes(modele, "Notes du modèle");
        oa = fixture.intervenant(modele, "OA", true);
        ob = fixture.intervenant(modele, "OB", false);
        t1 = fixture.tache(modele, null, "1", 1, 0, DEBUT_MODELE, 4);
        fixture.categorieDeTache(t1, categorieLot);
        UUID t11 = fixture.tache(modele, t1, "1.1", 2, 1, LocalDate.of(2026, 3, 3), 2);
        fixture.ressources(t11, oa, ob);
        fixture.categorieDeTache(t11, categorieSansLot);
        UUID t2 = fixture.tache(modele, null, "2", 3, 0, LocalDate.of(2026, 3, 6), 1);
        fixture.predecesseurs(t2, "1|1");
    }

    private Reponse cloner(UUID versProjet) throws Exception {
        return api.creer(planificationDuProjet(versProjet), corps().avec(MODELE_ID, modele), admin);
    }

    private Map<String, Object> tache(UUID planification, String code) {
        UUID id = fixture.tacheParCode(planification, code);
        if (id == null) {
            return Map.of();
        }
        List<Map<String, Object>> lignes = fixture.lignes(TACHE, id);
        return lignes.isEmpty() ? Map.of() : lignes.get(0);
    }

    private static String minuscules(Object valeur) {
        return valeur == null ? null : valeur.toString().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ R8 : en-tête du clone

    // R8 : Code = « <code du projet> - dd/MM/yyyy HH:mm:ss », « Version principale », notes copiées, début aujourd'hui.
    @Test
    void r8_clone_enTete_codeDuProjetEtDate_versionPrincipale_notes_debutAujourdhui() throws Exception {
        Reponse r = cloner(projet);

        List<Map<String, Object>> enBase = fixture.lignes("""
                SELECT Code, Description, Notes, CONVERT(varchar(10), StartDate, 23) AS Debut, VersionDate,
                    CAST(CfgGanttProjectMainVersionId AS nvarchar(36)) AS Principale
                FROM CfgGanttProject WHERE ObjectId = ?""", projet);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(enBase).hasSize(1);
        if (enBase.size() == 1) {
            Map<String, Object> ligne = enBase.get(0);
            s.assertThat(String.valueOf(ligne.get("Code")))
                    .matches(Pattern.quote(codeProjet) + " - \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
            s.assertThat(ligne).containsEntry("Description", "Version principale").containsEntry("Notes", "Notes du modèle")
                    .containsEntry("Debut", LocalDate.now(horlogeMetier).toString()).containsEntry("VersionDate", null);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R8 : tâches et intervenants

    // R8 : mêmes tâches (Code, Description, Notes, Sorting, Depth, StartDate, Duration, Predecessors) ; ParentId et
    // ResourceNames remappés vers les copies ; intervenants copiés (Visible compris) ; modèle intact.
    @Test
    void r8_clone_tachesCopiees_parentEtIntervenantsRemappes() throws Exception {
        Reponse r = cloner(projet);

        UUID copie = fixture.planificationDuProjet(projet);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(copie).as("planification du projet").isNotNull();
        if (copie != null) {
            verifierTaches(s, copie);
            verifierIntervenants(s, copie);
        }
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?", modele)).isEqualTo(3);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE CfgGanttProjectId = ?", modele)).isEqualTo(2);
        s.assertAll();
    }

    private void verifierTaches(SoftAssertions s, UUID copie) {
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?", copie)).isEqualTo(3);
        Map<String, Object> un = tache(copie, "1");
        Map<String, Object> unUn = tache(copie, "1.1");
        Map<String, Object> deux = tache(copie, "2");
        UUID idUn = fixture.tacheParCode(copie, "1");
        s.assertThat(un).containsEntry("Description", "Tâche 1").containsEntry("Notes", "Notes 1").containsEntry("Sorting", 1)
                .containsEntry("Depth", 0).containsEntry("Duration", 4).containsEntry("Debut", "2026-03-02")
                .containsEntry("Parent", null);
        s.assertThat(unUn).containsEntry("Sorting", 2).containsEntry("Depth", 1).containsEntry("Debut", "2026-03-03");
        s.assertThat(idUn).as("copie de la tâche 1").isNotNull().isNotEqualTo(t1);
        s.assertThat(minuscules(unUn.get("Parent"))).isEqualTo(idUn == null ? null : idUn.toString());
        s.assertThat(deux).containsEntry("Predecessors", "1|1").containsEntry("Sorting", 3);
        UUID copieOa = intervenantParCode(copie, "OA");
        UUID copieOb = intervenantParCode(copie, "OB");
        String ressources = minuscules(unUn.get("ResourceNames"));
        s.assertThat(ressources == null ? List.of() : Arrays.asList(ressources.split(";")))
                .containsExactlyInAnyOrder(String.valueOf(copieOa), String.valueOf(copieOb))
                .doesNotContain(oa.toString(), ob.toString());
    }

    private void verifierIntervenants(SoftAssertions s, UUID copie) {
        List<Map<String, Object>> intervenants = fixture.lignes("""
                SELECT Code, CAST(Visible AS int) AS Visible FROM CfgGanttOwner WHERE CfgGanttProjectId = ? ORDER BY Code ASC""",
                copie);
        s.assertThat(intervenants).extracting(l -> l.get("Code")).containsExactly("OA", "OB");
        s.assertThat(intervenants).extracting(l -> l.get("Visible")).containsExactly(1, 0);
    }

    private UUID intervenantParCode(UUID planification, String code) {
        String id = fixture.lire("SELECT CAST(PKey AS nvarchar(36)) FROM CfgGanttOwner WHERE CfgGanttProjectId = ? AND Code = ?",
                planification, code);
        return id == null ? null : UUID.fromString(id);
    }

    // R8 : PrjMarketId = lot du projet cible dont StlCategoryId égale celle de la tâche, sinon NULL.
    @Test
    void r8_clone_lotDuProjetRattacheParCategorie_sinonNul() throws Exception {
        Reponse r = cloner(projet);

        UUID copie = fixture.planificationDuProjet(projet);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(copie).as("planification du projet").isNotNull();
        if (copie != null) {
            s.assertThat(minuscules(tache(copie, "1").get("Lot"))).isEqualTo(lot.toString());
            s.assertThat(minuscules(tache(copie, "1").get("Categorie"))).isEqualTo(categorieLot.toString());
            s.assertThat(tache(copie, "1.1")).containsEntry("Lot", null);
            s.assertThat(minuscules(tache(copie, "1.1").get("Categorie"))).isEqualTo(categorieSansLot.toString());
            s.assertThat(tache(copie, "2")).containsEntry("Lot", null);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R5 : une seule planification par projet

    @Test
    void r5_secondClone_409_uneSeulePlanificationDuProjet() throws Exception {
        Reponse premier = cloner(projet);
        Reponse second = cloner(projet);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(premier.statut()).as(premier.corps()).isIn(200, 201);
        s.assertThat(second.statut()).as(second.corps()).isEqualTo(409);
        s.assertThat(fixture.compter(PLANIFICATIONS_DU_PROJET, projet)).isEqualTo(1);
        s.assertAll();
    }

    // ------------------------------------------------------------------ R7 : lecture de la planification du projet

    // R7 / D2 : absente → 404 métier, code PLANIFICATION_ABSENTE, message OMRAN exact.
    @Test
    void r7_planificationAbsente_404CodeEtMessageOmran() throws Exception {
        Reponse r = api.lire(planificationDuProjet(projet), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        s.assertThat(r.corps()).contains(CODE_ABSENTE);
        if (r.corps().contains(CODE_ABSENTE)) {
            s.assertThat(r.valeur(CODE)).isEqualTo(CODE_ABSENTE);
            s.assertThat(r.valeur(DETAIL)).isEqualTo(MESSAGE_ABSENTE);
        }
        s.assertAll();
    }

    // R7 : présente → on l'ouvre (détail de la planification du projet).
    @Test
    void r7_planificationPresente_200_detailDeLaPlanificationDuProjet() throws Exception {
        UUID existante = fixture.planificationProjet(projet, "PRJ-" + jeton, DEBUT_MODELE);
        UUID tacheExistante = fixture.tache(existante, null, "1", 1, 0, DEBUT_MODELE, 1);

        Reponse r = api.lire(planificationDuProjet(projet), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        if (r.statut() == 200) {
            s.assertThat(r.valeur("$.id")).isEqualTo(existante.toString());
            s.assertThat(r.ids("$.taches[*].id")).containsExactly(tacheExistante.toString());
        }
        s.assertAll();
    }

    // R7 : tâches d'un lot = tâches de la planification du projet dont PrjMarketId = lot.
    @Test
    void r7_tachesDuLot_seulementCellesRattacheesAuLot() throws Exception {
        UUID existante = fixture.planificationProjet(projet, "PRJ-" + jeton, DEBUT_MODELE);
        UUID duLot = fixture.tache(existante, null, "1", 1, 0, DEBUT_MODELE, 1);
        UUID horsLot = fixture.tache(existante, null, "2", 2, 0, DEBUT_MODELE, 1);
        fixture.lotDeTache(duLot, lot);

        Reponse r = api.lire(tachesDuLot(lot), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        if (r.statut() == 200) {
            s.assertThat(r.ids("$..id")).contains(duLot.toString()).doesNotContain(horsLot.toString());
        }
        s.assertAll();
    }

    // R7 / D2 : bouton Tâches d'un lot sans planification de projet → même 404 métier.
    @Test
    void r7_tachesDuLot_planificationAbsente_404Code() throws Exception {
        Reponse r = api.lire(tachesDuLot(lot), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        s.assertThat(r.corps()).contains(CODE_ABSENTE);
        if (r.corps().contains(CODE_ABSENTE)) {
            s.assertThat(r.valeur(CODE)).isEqualTo(CODE_ABSENTE);
            s.assertThat(r.valeur(DETAIL)).isEqualTo(MESSAGE_ABSENTE);
        }
        s.assertAll();
    }
}
