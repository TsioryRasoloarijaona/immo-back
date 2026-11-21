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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.dateDebut;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.intervenant;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.intervenants;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planification;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tache;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.taches;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/**
 * N9b, éditeur de planification sur un modèle : R9 (tâche, rang et profondeur serveur, arbre), R10 (prédécesseurs), R11
 * (intervenants) et R12 (« Modifier la date de début ») — FEATURE-n9-planification.md. Routes {@code …/{id}/taches},
 * {@code …/{id}/intervenants}, {@code …/{id}/date-debut}. Rang et profondeur vérifiés en base ({@code Sorting}, {@code Depth}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationTachesTest {

    private static final String PARENT = "parentId";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String DUREE = "duree";
    private static final String PREDECESSEURS = "predecesseurs";
    private static final String TACHE_ID = "tacheId";
    private static final String INTERVENANTS = "intervenants";
    private static final String SORTING = "Sorting";
    private static final String DEPTH = "Depth";
    private static final String DEBUT = "Debut";
    private static final String LIGNE = """
            SELECT Code, Description, Sorting, Depth, Duration, CONVERT(varchar(10), StartDate, 23) AS Debut,
                CONVERT(varchar(10), ConstraintDate, 23) AS Contrainte, CONVERT(varchar(10), Deadline, 23) AS Echeance,
                CAST(ParentId AS nvarchar(36)) AS Parent, Predecessors, ResourceNames FROM CfgGanttTask WHERE PKey = ?""";
    private static final String TACHE_EXISTE = "SELECT COUNT(*) FROM CfgGanttTask WHERE PKey = ?";
    private static final LocalDate DEBUT_PLANIFICATION = LocalDate.of(2026, 3, 2);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;

    private AppelsPlanification api;
    private String jeton;
    private RequestPostProcessor admin;
    private UUID planif;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        admin = connecte(parite.administrateur("n9b.tac." + jeton), "Administrateur");
        planif = fixture.modele("TAC-" + jeton, DEBUT_PLANIFICATION);
    }

    private Map<String, Object> ligne(UUID tache) {
        List<Map<String, Object>> lignes = fixture.lignes(LIGNE, tache);
        return lignes.isEmpty() ? Map.of() : lignes.get(0);
    }

    private UUID racine(String code, int rang) {
        return fixture.tache(planif, null, code, rang, 0, DEBUT_PLANIFICATION, 2);
    }

    private static int entier(Map<String, Object> ligne, String colonne) {
        Object valeur = ligne.get(colonne);
        return valeur == null ? -1 : ((Number) valeur).intValue();
    }

    // ------------------------------------------------------------------ R9 : valeurs par défaut

    // R9 (GanttEditor.cs:AddRootTask) : Code = nombre de tâches + 1, « Tâche <code> », durée 1, début de la planification.
    @Test
    void r9_nouvelleRacine_codeNombrePlusUn_descriptionDureeEtDebutDeLaPlanification() throws Exception {
        racine("1", 1);
        racine("2", 2);

        Reponse r = api.creer(taches(planif), corps(), admin);

        UUID creee = fixture.tacheParCode(planif, "3");
        Map<String, Object> enBase = creee == null ? Map.of() : ligne(creee);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(creee).as("tâche de code 3").isNotNull();
        s.assertThat(enBase).containsEntry("Description", "Tâche 3").containsEntry("Duration", 1)
                .containsEntry(DEBUT, "2026-03-02").containsEntry("Parent", null)
                .containsEntry(SORTING, 3).containsEntry(DEPTH, 0);
        s.assertAll();
    }

    // R9 (AddChildTask) : Code « <parent>.<n> », « Sous-tâche <code> », durée du parent ; rang serveur, la racine suivante glisse.
    @Test
    void r9_sousTache_codeParentPointN_dureeDuParent_rangsRecalcules() throws Exception {
        UUID un = fixture.tache(planif, null, "1", 1, 0, DEBUT_PLANIFICATION, 5);
        fixture.tache(planif, un, "1.1", 2, 1, DEBUT_PLANIFICATION, 5);
        UUID deux = racine("2", 3);

        Reponse r = api.creer(taches(planif), corps().avec(PARENT, un), admin);

        UUID creee = fixture.tacheParCode(planif, "1.2");
        Map<String, Object> enBase = creee == null ? Map.of() : ligne(creee);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(creee).as("tâche de code 1.2").isNotNull();
        s.assertThat(enBase).containsEntry("Description", "Sous-tâche 1.2").containsEntry("Duration", 5)
                .containsEntry(SORTING, 3).containsEntry(DEPTH, 1);
        s.assertThat(String.valueOf(enBase.get("Parent"))).isEqualToIgnoringCase(un.toString());
        s.assertThat(entier(ligne(deux), SORTING)).isEqualTo(4);
        s.assertAll();
    }

    // ------------------------------------------------------------------ R9 : arbre, rang et profondeur serveur

    // R9 : indenter = changer ParentId ; Sorting (ordre en profondeur) et Depth recalculés pour la tâche et ses descendants.
    @Test
    void r9_indentation_profondeurEtRangRecalculesSurLeSousArbre() throws Exception {
        UUID a = racine("A", 1);
        UUID b = racine("B", 2);
        UUID b1 = fixture.tache(planif, b, "B.1", 3, 1, DEBUT_PLANIFICATION, 2);
        UUID c = racine("C", 4);

        Reponse r = api.modifier(tache(planif, b), corps().avec(PARENT, a), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(ligne(a)).containsEntry(SORTING, 1).containsEntry(DEPTH, 0);
        s.assertThat(ligne(b)).containsEntry(SORTING, 2).containsEntry(DEPTH, 1);
        s.assertThat(ligne(b1)).containsEntry(SORTING, 3).containsEntry(DEPTH, 2);
        s.assertThat(ligne(c)).containsEntry(SORTING, 4).containsEntry(DEPTH, 0);
        s.assertThat(String.valueOf(ligne(b).get("Parent"))).isEqualToIgnoringCase(a.toString());
        s.assertAll();
    }

    // R9 : ParentId d'une autre planification → 400.
    @Test
    void r9_parentDUneAutrePlanification_400() throws Exception {
        UUID a = racine("A", 1);
        UUID autre = fixture.modele("AUT-" + jeton, DEBUT_PLANIFICATION);
        UUID etrangere = fixture.tache(autre, null, "X", 1, 0, DEBUT_PLANIFICATION, 1);

        Reponse temoin = api.modifier(tache(planif, a), corps().avec(DESCRIPTION, "Témoin"), admin);
        Reponse modification = api.modifier(tache(planif, a), corps().avec(PARENT, etrangere), admin);
        Reponse creation = api.creer(taches(planif), corps().avec(PARENT, etrangere), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(400);
        s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(400);
        s.assertThat(ligne(a)).containsEntry("Parent", null);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE ParentId = ?", etrangere)).isZero();
        s.assertAll();
    }

    // R9 : un parent qui crée un cycle (soi-même ou un descendant) → 400.
    @Test
    void r9_cycle_soiMemeOuDescendant_400() throws Exception {
        UUID a = racine("A", 1);
        UUID a1 = fixture.tache(planif, a, "A.1", 2, 1, DEBUT_PLANIFICATION, 2);
        UUID a11 = fixture.tache(planif, a1, "A.1.1", 3, 2, DEBUT_PLANIFICATION, 2);

        Reponse temoin = api.modifier(tache(planif, a11), corps().avec(DESCRIPTION, "Témoin"), admin);
        Reponse soiMeme = api.modifier(tache(planif, a), corps().avec(PARENT, a), admin);
        Reponse descendant = api.modifier(tache(planif, a), corps().avec(PARENT, a11), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(soiMeme.statut()).as(soiMeme.corps()).isEqualTo(400);
        s.assertThat(descendant.statut()).as(descendant.corps()).isEqualTo(400);
        s.assertThat(ligne(a)).containsEntry("Parent", null).containsEntry(DEPTH, 0);
        s.assertAll();
    }

    // R9 : Duration en jours entiers ≥ 0 ; Code obligatoire.
    @Test
    void r9_dureeNegativeOuCodeVide_400() throws Exception {
        UUID a = racine("A", 1);

        Reponse temoin = api.modifier(tache(planif, a), corps().avec(DUREE, 0), admin);
        Reponse negative = api.modifier(tache(planif, a), corps().avec(DUREE, -1), admin);
        Reponse codeVide = api.modifier(tache(planif, a), corps().avec(CODE, " "), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(negative.statut()).as(negative.corps()).isEqualTo(400);
        s.assertThat(codeVide.statut()).as(codeVide.corps()).isEqualTo(400);
        s.assertThat(ligne(a)).containsEntry("Code", "A").containsEntry("Duration", 0);
        s.assertAll();
    }

    // R9 : supprimer une tâche supprime ses descendants.
    @Test
    void r9_suppression_supprimeLesDescendants_autresIntactes() throws Exception {
        UUID a = racine("A", 1);
        UUID b = racine("B", 2);
        UUID b1 = fixture.tache(planif, b, "B.1", 3, 1, DEBUT_PLANIFICATION, 2);
        UUID b11 = fixture.tache(planif, b1, "B.1.1", 4, 2, DEBUT_PLANIFICATION, 2);

        Reponse r = api.supprimer(tache(planif, b), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 204);
        s.assertThat(fixture.compter(TACHE_EXISTE, b) + fixture.compter(TACHE_EXISTE, b1)
                + fixture.compter(TACHE_EXISTE, b11)).isZero();
        s.assertThat(fixture.compter(TACHE_EXISTE, a)).isEqualTo(1);
        s.assertAll();
    }

    // ------------------------------------------------------------------ R10 : prédécesseurs

    // R10 : Predecessors = "<Sorting>|<type>" ; une sous-tâche insérée décale les rangs → le serveur réécrit le texte.
    @Test
    void r10_rangsDecales_predecesseursReecritsAvecLeursTypes() throws Exception {
        UUID a = racine("A", 1);
        racine("B", 2);
        UUID c = racine("C", 3);
        fixture.predecesseurs(c, "1|0;2|1");

        Reponse r = api.creer(taches(planif), corps().avec(PARENT, a), admin);

        String texte = String.valueOf(ligne(c).get("Predecessors"));
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(entier(ligne(c), SORTING)).isEqualTo(4);
        s.assertThat(Arrays.asList(texte.split(";"))).containsExactlyInAnyOrder("1|0", "3|1");
        s.assertAll();
    }

    // R10 : le DTO expose les prédécesseurs en liste structurée { tacheId, type } ; R11 : intervenants = liste d'ids.
    @Test
    void r10_r11_detail_predecesseursEtIntervenantsEnIds() throws Exception {
        racine("A", 1);
        UUID b = racine("B", 2);
        UUID c = racine("C", 3);
        fixture.predecesseurs(c, "2|1");
        UUID proprietaire = fixture.intervenant(planif, "OWN-" + jeton, true);
        fixture.ressources(c, proprietaire);

        Reponse r = api.lire(planification(planif), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        if (r.statut() == 200) {
            String tacheC = "$.taches[?(@.id == '" + c + "')]";
            s.assertThat(r.ids(tacheC + "." + PREDECESSEURS + "[*]." + TACHE_ID)).containsExactly(b.toString());
            s.assertThat(r.ids(tacheC + "." + INTERVENANTS + "[*]")).containsExactly(proprietaire.toString());
            s.assertThat(r.ids("$." + INTERVENANTS + "[*].id")).contains(proprietaire.toString());
        }
        s.assertAll();
    }

    // R10 : un prédécesseur inconnu ou la tâche elle-même → 400, Predecessors inchangé.
    @Test
    void r10_predecesseurInconnuOuSoiMeme_400() throws Exception {
        racine("A", 1);
        UUID b = racine("B", 2);
        fixture.predecesseurs(b, "1|1");

        Reponse inconnu = api.modifier(tache(planif, b),
                corps().avec(PREDECESSEURS, List.of(corps().avec(TACHE_ID, UUID.randomUUID()))), admin);
        Reponse soiMeme = api.modifier(tache(planif, b), corps().avec(PREDECESSEURS, List.of(corps().avec(TACHE_ID, b))), admin);
        String apresRefus = String.valueOf(ligne(b).get("Predecessors"));
        Reponse temoin = api.modifier(tache(planif, b), corps().avec(PREDECESSEURS, List.of()), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(inconnu.statut()).as(inconnu.corps()).isEqualTo(400);
        s.assertThat(soiMeme.statut()).as(soiMeme.corps()).isEqualTo(400);
        s.assertThat(apresRefus).isEqualTo("1|1");
        s.assertAll();
    }

    // ------------------------------------------------------------------ R11 : intervenants

    // R11 : Visible vrai par défaut, rattaché à la planification de l'URL.
    @Test
    void r11_creationIntervenant_visibleParDefaut() throws Exception {
        String code = "INT-" + jeton;

        Reponse r = api.creer(intervenants(planif), corps().avec(CODE, code).avec(DESCRIPTION, "Bureau d'études"), admin);

        List<Map<String, Object>> enBase = fixture.lignes("""
                SELECT CAST(Visible AS int) AS Visible, CAST(CfgGanttProjectId AS nvarchar(36)) AS Planification
                FROM CfgGanttOwner WHERE Code = ?""", code);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(enBase).hasSize(1);
        if (enBase.size() == 1) {
            s.assertThat(enBase.get(0)).containsEntry("Visible", 1);
            s.assertThat(String.valueOf(enBase.get(0).get("Planification"))).isEqualToIgnoringCase(planif.toString());
        }
        s.assertAll();
    }

    // R11 : ResourceNames = ids d'intervenants « ; » ; un id étranger à la planification → 400.
    @Test
    void r11_intervenantsDeTache_etrangerRefuse400_propreEcrit() throws Exception {
        UUID a = racine("A", 1);
        UUID propre = fixture.intervenant(planif, "OWN-" + jeton, true);
        UUID autre = fixture.modele("AUT-" + jeton, DEBUT_PLANIFICATION);
        UUID etranger = fixture.intervenant(autre, "ETR-" + jeton, true);

        Reponse refus = api.modifier(tache(planif, a), corps().avec(INTERVENANTS, List.of(propre, etranger)), admin);
        String apresRefus = String.valueOf(ligne(a).get("ResourceNames"));
        Reponse temoin = api.modifier(tache(planif, a), corps().avec(INTERVENANTS, List.of(propre)), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
        s.assertThat(apresRefus).doesNotContainIgnoringCase(etranger.toString());
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(String.valueOf(ligne(a).get("ResourceNames"))).isEqualToIgnoringCase(propre.toString());
        s.assertAll();
    }

    // R11 : supprimer un intervenant le retire des ResourceNames des tâches.
    @Test
    void r11_suppressionIntervenant_retireDesTaches() throws Exception {
        UUID a = racine("A", 1);
        UUID retire = fixture.intervenant(planif, "RET-" + jeton, true);
        UUID garde = fixture.intervenant(planif, "GAR-" + jeton, true);
        fixture.ressources(a, retire, garde);

        Reponse r = api.supprimer(intervenant(planif, retire), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 204);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE PKey = ?", retire)).isZero();
        s.assertThat(String.valueOf(ligne(a).get("ResourceNames"))).isEqualToIgnoringCase(garde.toString());
        s.assertAll();
    }

    // ------------------------------------------------------------------ R12 : date de début

    // R12 (GanttEditor.cs:ModifyStartDate) : StartDate et ConstraintDate décalés de l'écart ; Deadline seulement si elle
    // était après l'ancienne date de début (ici avant l'ancienne ou après la nouvelle : les deux lectures concordent).
    @Test
    void r12_dateDebut_decaleDebutsEtContraintes_echeanceAnterieureIntacte() throws Exception {
        UUID t1 = racine("1", 1);
        fixture.contrainteEtEcheance(t1, LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 1));
        UUID t2 = fixture.tache(planif, null, "2", 2, 0, LocalDate.of(2026, 3, 10), 3);
        fixture.contrainteEtEcheance(t2, null, LocalDate.of(2026, 2, 20));
        UUID t3 = racine("3", 3);

        Reponse r = api.modifier(dateDebut(planif), corps().avec("dateDebut", "2026-03-09"), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(fixture.lire("SELECT CONVERT(varchar(10), StartDate, 23) FROM CfgGanttProject WHERE PKey = ?", planif))
                .isEqualTo("2026-03-09");
        s.assertThat(ligne(t1)).containsEntry(DEBUT, "2026-03-09").containsEntry("Contrainte", "2026-03-12")
                .containsEntry("Echeance", "2026-04-08");
        s.assertThat(ligne(t2)).containsEntry(DEBUT, "2026-03-17").containsEntry("Contrainte", null)
                .containsEntry("Echeance", "2026-02-20");
        s.assertThat(ligne(t3)).containsEntry(DEBUT, "2026-03-09").containsEntry("Echeance", null);
        s.assertAll();
    }
}
