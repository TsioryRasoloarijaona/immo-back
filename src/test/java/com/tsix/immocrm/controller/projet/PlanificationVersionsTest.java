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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.dateDebut;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planification;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tache;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.taches;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.versions;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/**
 * N9b, versions : R13 (sauvegarde = clone daté, projet seulement, version en lecture seule) et R5 (version = {@code VersionDate}
 * non nul + {@code CfgGanttProjectMainVersionId}) — FEATURE-n9-planification.md. Route {@code …/{id}/versions}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationVersionsTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 2);
    private static final String VERSIONS_DE = "SELECT COUNT(*) FROM CfgGanttProject WHERE CfgGanttProjectMainVersionId = ?";
    private static final String TACHES_DE = "SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?";
    private static final String DESCRIPTION = "description";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private Clock horlogeMetier;

    private AppelsPlanification api;
    private String jeton;
    private UUID projet;
    private UUID duProjet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        projet = parite.projet("N9P-" + jeton, parite.entiteJuridique("N9E-" + jeton));
        admin = connecte(parite.administrateur("n9b.ver." + jeton, projet), "Administrateur");
        duProjet = fixture.planificationProjet(projet, "PRJ-" + jeton, DEBUT);
        UUID racine = fixture.tache(duProjet, null, "1", 1, 0, DEBUT, 3);
        fixture.tache(duProjet, racine, "1.1", 2, 1, DEBUT, 3);
    }

    // R13 : Clone(null, null, maintenant) — Code « <code> - dd/MM/yyyy HH:mm:ss », Description = date courte, tâches copiées ;
    // message « Version '<Description>' sauvegardée correctement. » ; la planification principale reste celle du projet.
    @Test
    void r13_sauvegarde_versionDateeAvecTaches_message() throws Exception {
        String aujourdhui = LocalDate.now(horlogeMetier).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Reponse r = api.creer(versions(duProjet), corps(), admin);

        List<Map<String, Object>> enBase = fixture.lignes("""
                SELECT CAST(PKey AS nvarchar(36)) AS Id, Code, Description, CONVERT(varchar(10), VersionDate, 23) AS DateVersion
                FROM CfgGanttProject WHERE CfgGanttProjectMainVersionId = ?""", duProjet);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
        s.assertThat(r.corps()).contains("Version '" + aujourdhui + "' sauvegardée correctement.");
        s.assertThat(enBase).hasSize(1);
        if (enBase.size() == 1) {
            Map<String, Object> version = enBase.get(0);
            s.assertThat(String.valueOf(version.get("Code")))
                    .matches(Pattern.quote("PRJ-" + jeton) + " - \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
            s.assertThat(version).containsEntry("Description", aujourdhui)
                    .containsEntry("DateVersion", LocalDate.now(horlogeMetier).toString());
            s.assertThat(fixture.compter(TACHES_DE, version.get("Id"))).isEqualTo(2);
        }
        s.assertThat(fixture.planificationDuProjet(projet)).isEqualTo(duProjet);
        s.assertAll();
    }

    // R13 : disponible seulement pour une planification de projet (ObjectId non nul) ; sur un modèle → 409.
    @Test
    void r13_sauvegardeSurUnModele_409_aucuneVersion() throws Exception {
        UUID modele = fixture.modele("MOD-" + jeton, DEBUT);
        fixture.tache(modele, null, "1", 1, 0, DEBUT, 1);

        Reponse temoin = api.creer(versions(duProjet), corps(), admin);
        Reponse r = api.creer(versions(modele), corps(), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        s.assertThat(fixture.compter(VERSIONS_DE, modele)).isZero();
        s.assertAll();
    }

    // R13 : une version ne s'édite pas (409) — tâches, date de début ; la planification du projet, elle, s'édite.
    @Test
    void r13_versionNonEditable_409_tachesEtDateDebutInchangees() throws Exception {
        UUID version = fixture.version(duProjet, "VER-" + jeton, LocalDate.of(2026, 3, 5));
        UUID tacheVersion = fixture.tache(version, null, "1", 1, 0, DEBUT, 3);
        UUID tacheProjet = fixture.tacheParCode(duProjet, "1");

        Reponse temoin = api.modifier(tache(duProjet, tacheProjet), corps().avec(DESCRIPTION, "Témoin"), admin);
        Reponse ajout = api.creer(taches(version), corps(), admin);
        Reponse modification = api.modifier(tache(version, tacheVersion), corps().avec(DESCRIPTION, "Interdit"), admin);
        Reponse suppression = api.supprimer(tache(version, tacheVersion), admin);
        Reponse date = api.modifier(dateDebut(version), corps().avec("dateDebut", "2026-04-01"), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(ajout.statut()).as(ajout.corps()).isEqualTo(409);
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(409);
        s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(409);
        s.assertThat(date.statut()).as(date.corps()).isEqualTo(409);
        s.assertThat(fixture.compter(TACHES_DE, version)).isEqualTo(1);
        s.assertThat(fixture.lire("SELECT Description FROM CfgGanttTask WHERE PKey = ?", tacheVersion)).isEqualTo("Tâche 1");
        s.assertThat(fixture.lire("SELECT CONVERT(varchar(10), StartDate, 23) FROM CfgGanttProject WHERE PKey = ?", version))
                .isEqualTo("2026-03-05");
        s.assertAll();
    }

    // R13 (ShowVersions) : liste des versions de la planification ; une version s'ouvre (lecture).
    @Test
    void r13_listeDesVersions_etOuvertureDUneVersion() throws Exception {
        UUID version = fixture.version(duProjet, "VER-" + jeton, LocalDate.of(2026, 3, 5));
        UUID tacheVersion = fixture.tache(version, null, "1", 1, 0, DEBUT, 3);
        UUID modele = fixture.modele("MOD-" + jeton, DEBUT);

        Reponse liste = api.lire(versions(duProjet), admin);
        Reponse ouverture = api.lire(planification(version), admin);
        Reponse detail = api.lire(planification(duProjet), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        s.assertThat(ouverture.statut()).as(ouverture.corps()).isEqualTo(200);
        s.assertThat(detail.statut()).as(detail.corps()).isEqualTo(200);
        if (detail.statut() == 200) {
            s.assertThat(detail.ids("$.versions[*].id")).containsExactly(version.toString());
        }
        if (liste.statut() == 200) {
            s.assertThat(liste.ids("$..id")).contains(version.toString())
                    .doesNotContain(duProjet.toString(), modele.toString());
        }
        if (ouverture.statut() == 200) {
            s.assertThat(ouverture.ids("$.taches[*].id")).containsExactly(tacheVersion.toString());
        }
        s.assertAll();
    }
}
