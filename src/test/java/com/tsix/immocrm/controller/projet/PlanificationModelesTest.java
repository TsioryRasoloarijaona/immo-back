package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePlanification;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.MODELES;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.modele;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/**
 * N9b, modèles de planification : R5 (un modèle = {@code ObjectId} et {@code VersionDate} NULL) et R6 (liste, CRUD, code unique,
 * suppression des versions, tâches et intervenants) — FEATURE-n9-planification.md. Route {@code /api/projet/planification/modeles}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationModelesTest {

    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String NOTES = "notes";
    private static final String DATE_DEBUT = "dateDebut";
    private static final String LIGNE = """
            SELECT Code, Description, Notes, CONVERT(varchar(10), StartDate, 23) AS Debut,
                CAST(ObjectId AS nvarchar(36)) AS Projet, VersionDate FROM CfgGanttProject WHERE Code = ?""";
    private static final String PAR_CODE = "SELECT COUNT(*) FROM CfgGanttProject WHERE Code = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private Clock horlogeMetier;

    private AppelsPlanification api;
    private String jeton;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        admin = connecte(parite.administrateur("n9b.mod." + jeton), "Administrateur");
    }

    // ------------------------------------------------------------------ R6 : création

    // R5 / R6 : modèle = ObjectId NULL et VersionDate NULL ; Données : StartDate par défaut aujourd'hui.
    @Test
    void r6_creation_modeleSansProjetNiVersion_debutParDefautAujourdhui() throws Exception {
        String code = "MOD-" + jeton;

        Reponse r = api.creer(MODELES, corps().avec(CODE, code).avec(DESCRIPTION, "Modèle immeuble").avec(NOTES, "N"), admin);

        List<Map<String, Object>> enBase = fixture.lignes(LIGNE, code);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(enBase).hasSize(1);
        if (enBase.size() == 1) {
            s.assertThat(enBase.get(0)).containsEntry("Description", "Modèle immeuble").containsEntry("Notes", "N")
                    .containsEntry("Debut", LocalDate.now(horlogeMetier).toString())
                    .containsEntry("Projet", null).containsEntry("VersionDate", null);
        }
        s.assertAll();
    }

    @Test
    void r6_creation_dateDebutFournie_conservee() throws Exception {
        String code = "MOD-" + jeton;

        Reponse r = api.creer(MODELES, corps().avec(CODE, code).avec(DESCRIPTION, "D").avec(DATE_DEBUT, "2026-03-02"), admin);

        String debut = fixture.lire("SELECT CONVERT(varchar(10), StartDate, 23) FROM CfgGanttProject WHERE Code = ?", code);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(debut).isEqualTo("2026-03-02");
        s.assertAll();
    }

    // R6 : Code unique.
    @Test
    void r6_codeEnDouble_409_aucuneSecondeLigne() throws Exception {
        String code = "DBL-" + jeton;
        fixture.modele(code, LocalDate.of(2026, 3, 2));

        Reponse temoin = api.creer(MODELES, corps().avec(CODE, "OK-" + jeton).avec(DESCRIPTION, "D"), admin);
        Reponse r = api.creer(MODELES, corps().avec(CODE, code).avec(DESCRIPTION, "D"), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        s.assertThat(fixture.compter(PAR_CODE, code)).isEqualTo(1);
        s.assertAll();
    }

    // Données : Code et Description obligatoires.
    @ParameterizedTest(name = "{0} absent")
    @ValueSource(strings = {CODE, DESCRIPTION})
    void r6_champObligatoireAbsent_400(String champ) throws Exception {
        String code = "OBL-" + jeton;

        Reponse temoin = api.creer(MODELES, corps().avec(CODE, "OK-" + jeton).avec(DESCRIPTION, "D"), admin);
        Reponse r = api.creer(MODELES, corps().avec(CODE, code).avec(DESCRIPTION, "D").sans(champ), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(fixture.compter(PAR_CODE, code)).isZero();
        s.assertAll();
    }

    // ------------------------------------------------------------------ R5 / R6 : liste

    @Test
    void r5_liste_modelesSeulement_niPlanificationDeProjetNiVersion() throws Exception {
        UUID societe = parite.entiteJuridique("N9E-" + jeton);
        UUID projet = parite.projet("N9P-" + jeton, societe);
        UUID modele = fixture.modele("LST-" + jeton, LocalDate.of(2026, 3, 2));
        UUID duProjet = fixture.planificationProjet(projet, "PRJ-" + jeton, LocalDate.of(2026, 3, 2));
        UUID version = fixture.version(duProjet, "VER-" + jeton, LocalDate.of(2026, 3, 5));

        Reponse r = api.lire(MODELES, admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        if (r.statut() == 200) {
            s.assertThat(r.ids("$..id")).contains(modele.toString())
                    .doesNotContain(duProjet.toString(), version.toString());
            s.assertThat(r.ids("$..code")).contains("LST-" + jeton).doesNotContain("PRJ-" + jeton, "VER-" + jeton);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R6 : modification

    @Test
    void r6_modification_descriptionEtNotes() throws Exception {
        String code = "MAJ-" + jeton;
        UUID id = fixture.modele(code, LocalDate.of(2026, 3, 2));

        Reponse r = api.modifier(modele(id), corps().avec(DESCRIPTION, "Nouvelle").avec(NOTES, "Notes 2"), admin);

        List<Map<String, Object>> enBase = fixture.lignes(LIGNE, code);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(enBase).hasSize(1);
        if (enBase.size() == 1) {
            s.assertThat(enBase.get(0)).containsEntry("Description", "Nouvelle").containsEntry("Notes", "Notes 2")
                    .containsEntry("Projet", null).containsEntry("VersionDate", null);
        }
        s.assertAll();
    }

    // ------------------------------------------------------------------ R6 / D7 : suppression

    // R6 : supprimer un modèle supprime ses versions, tâches et intervenants (BeforeDelete) ; jamais refusé.
    @Test
    void r6_suppression_supprimeVersionsTachesEtIntervenants() throws Exception {
        LocalDate debut = LocalDate.of(2026, 3, 2);
        UUID id = fixture.modele("SUP-" + jeton, debut);
        UUID racine = fixture.tache(id, null, "1", 1, 0, debut, 3);
        fixture.tache(id, racine, "1.1", 2, 1, debut, 3);
        fixture.intervenant(id, "INT-" + jeton, true);
        UUID version = fixture.version(id, "SUPV-" + jeton, LocalDate.of(2026, 3, 5));
        fixture.tache(version, null, "1", 1, 0, debut, 3);
        UUID autre = fixture.modele("GARDE-" + jeton, debut);
        fixture.tache(autre, null, "1", 1, 0, debut, 3);

        Reponse r = api.supprimer(modele(id), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 204);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE PKey IN (?, ?)", id, version)).isZero();
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId IN (?, ?)", id, version))
                .isZero();
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE CfgGanttProjectId = ?", id)).isZero();
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?", autre)).isEqualTo(1);
        s.assertAll();
    }
}
