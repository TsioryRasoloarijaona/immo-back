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
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.modele;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planificationDuProjet;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tache;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;

/** Revue N9b, 2ᵉ tour : ids croisés entre routes (modèle ↔ planification de projet, prédécesseur caché). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationRevueIdsCroisesTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 2);
    private static final String DESCRIPTION = "description";
    private static final String MODELE_ID = "modeleId";
    private static final String DESCRIPTION_PLANIF = "SELECT Description FROM CfgGanttProject WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;

    private AppelsPlanification api;
    private String jeton;
    private UUID vide;
    private UUID planifProjet;
    private UUID planifCachee;
    private UUID version;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique("N9X-" + jeton);
        UUID projet = parite.projet("N9XP-" + jeton, societe);
        UUID cache = parite.projet("N9XH-" + jeton, societe);
        vide = parite.projet("N9XV-" + jeton, societe);
        admin = connecte(parite.administrateur("n9b.x.a." + jeton, projet, vide), "Administrateur");
        planifProjet = fixture.planificationProjet(projet, "XPRJ-" + jeton, DEBUT);
        planifCachee = fixture.planificationProjet(cache, "XCAC-" + jeton, DEBUT);
        fixture.tache(planifCachee, null, "1", 1, 0, DEBUT, 1);
        version = fixture.version(planifProjet, "XVER-" + jeton, DEBUT);
    }

    // R8 : seul un modèle se clone ; planification de projet (visible ou cachée) ou version → 400, rien d'écrit.
    @Test
    void r8_cloneDepuisUnePlanificationQuiNEstPasUnModele_400_rienEcrit() throws Exception {
        List<Reponse> refus = List.of(
                api.creer(planificationDuProjet(vide), corps().avec(MODELE_ID, planifCachee), admin),
                api.creer(planificationDuProjet(vide), corps().avec(MODELE_ID, planifProjet), admin),
                api.creer(planificationDuProjet(vide), corps().avec(MODELE_ID, version), admin));

        SoftAssertions s = new SoftAssertions();
        refus.forEach(r -> s.assertThat(r.statut()).as(r.corps()).isEqualTo(400));
        s.assertThat(refus.get(0).corps()).doesNotContain("XCAC-");
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE ObjectId = ?", vide)).isZero();
        s.assertAll();
    }

    // Routes /modeles/{id} avec l'id d'une planification de projet ou d'une version → 404, ligne intacte.
    @Test
    void modeles_idDePlanificationDeProjetOuDeVersion_404_rienModifie() throws Exception {
        List<Reponse> refus = List.of(
                api.modifier(modele(planifProjet), corps().avec(DESCRIPTION, "Pirate"), admin),
                api.modifier(modele(planifCachee), corps().avec(DESCRIPTION, "Pirate"), admin),
                api.modifier(modele(version), corps().avec(DESCRIPTION, "Pirate"), admin),
                api.supprimer(modele(planifProjet), admin),
                api.supprimer(modele(planifCachee), admin),
                api.supprimer(modele(version), admin));

        SoftAssertions s = new SoftAssertions();
        refus.forEach(r -> s.assertThat(r.statut()).as(r.corps()).isEqualTo(404));
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE PKey IN (?, ?, ?)", planifProjet, planifCachee,
                version)).isEqualTo(3);
        s.assertThat(fixture.lire(DESCRIPTION_PLANIF, planifCachee)).isNotEqualTo("Pirate");
        s.assertThat(fixture.lire(DESCRIPTION_PLANIF, planifProjet)).isNotEqualTo("Pirate");
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?", planifCachee))
                .isEqualTo(1);
        s.assertAll();
    }

    // R10 : un prédécesseur pris dans une planification cachée → 400, tâche intacte.
    @Test
    void r10_predecesseurDUnePlanificationCachee_400() throws Exception {
        UUID cachee = fixture.tache(planifCachee, null, "2", 2, 0, DEBUT, 1);
        UUID t = fixture.tache(planifProjet, null, "1", 1, 0, DEBUT, 1);

        Reponse r = api.modifier(tache(planifProjet, t),
                corps().avec("predecesseurs", List.of(corps().avec("tacheId", cachee))), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(fixture.lire("SELECT Predecessors FROM CfgGanttTask WHERE PKey = ?", t)).isNull();
        s.assertAll();
    }
}
