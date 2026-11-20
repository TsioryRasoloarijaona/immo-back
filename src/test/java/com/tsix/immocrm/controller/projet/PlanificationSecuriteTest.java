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

import static com.tsix.immocrm.controller.projet.AppelsPlanification.MODELES;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.dateDebut;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.intervenants;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.modele;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planification;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planificationDuProjet;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tache;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.taches;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tachesDuLot;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.versions;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixturePlanification.DROIT_GANTT;
import static com.tsix.immocrm.fixtures.FixturePlanification.DROIT_VERSION;

/**
 * N9b, droits et périmètre : D5 (un code {@code AdmRight} par appel : {@code Yes.Cfg.Front.Gantt.GanttProject} pour la
 * planification, {@code Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion} pour la sauvegarde de version), D6 (projet relu
 * depuis la ligne stockée, {@code ObjectId}) et ordre 404 → 403 → 400 — FEATURE-n9-planification.md.
 * Projet visible P (planification avec tâche T) ; projet caché H (non habilité, planification avec tâche TH, un lot).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationSecuriteTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 2);
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String PARENT = "parentId";
    private static final String COMMERCIAL = "Commercial";
    private static final String DESCRIPTION_TACHE = "SELECT Description FROM CfgGanttTask WHERE PKey = ?";
    private static final String TACHES_DE = "SELECT COUNT(*) FROM CfgGanttTask WHERE CfgGanttProjectId = ?";
    private static final String STATIC = "No static resource";
    private static final String MODELE_ID = "modeleId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;

    private AppelsPlanification api;
    private String jeton;
    private UUID projet;
    private UUID cache;
    private UUID planifProjet;
    private UUID tacheProjet;
    private UUID planifCachee;
    private UUID tacheCachee;
    private UUID modeleId;
    private RequestPostProcessor admin;
    private RequestPostProcessor gantt;
    private RequestPostProcessor sansDroit;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique("N9E-" + jeton);
        projet = parite.projet("N9P-" + jeton, societe);
        cache = parite.projet("N9H-" + jeton, societe);
        admin = connecte(parite.administrateur("n9b.sec.a." + jeton, projet), "Administrateur");
        gantt = connecte(parite.avecDroits("n9b.sec.g." + jeton, List.of(DROIT_GANTT), projet), COMMERCIAL);
        sansDroit = connecte(parite.avecDroits("n9b.sec.s." + jeton, List.of(), projet), COMMERCIAL);
        planifProjet = fixture.planificationProjet(projet, "PRJ-" + jeton, DEBUT);
        tacheProjet = fixture.tache(planifProjet, null, "1", 1, 0, DEBUT, 2);
        planifCachee = fixture.planificationProjet(cache, "CAC-" + jeton, DEBUT);
        tacheCachee = fixture.tache(planifCachee, null, "1", 1, 0, DEBUT, 2);
        modeleId = fixture.modele("MOD-" + jeton, DEBUT);
        fixture.tache(modeleId, null, "1", 1, 0, DEBUT, 2);
    }

    // ------------------------------------------------------------------ D5 : droit Gantt

    @Test
    void d5_sansDroitGantt_ecrituresDeModele403() throws Exception {
        Reponse temoin = api.creer(MODELES, corps().avec(CODE, "OK-" + jeton).avec(DESCRIPTION, "D"), gantt);
        Reponse creation = api.creer(MODELES, corps().avec(CODE, "KO-" + jeton).avec(DESCRIPTION, "D"), sansDroit);
        Reponse modification = api.modifier(modele(modeleId), corps().avec(DESCRIPTION, "Pirate"), sansDroit);
        Reponse suppression = api.supprimer(modele(modeleId), sansDroit);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(403);
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(403);
        s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(403);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE Code = ?", "KO-" + jeton)).isZero();
        s.assertThat(fixture.lire("SELECT Description FROM CfgGanttProject WHERE PKey = ?", modeleId))
                .isEqualTo("Planification MOD-" + jeton);
        s.assertAll();
    }

    @Test
    void d5_sansDroitGantt_ecrituresDeTachesIntervenantsEtDate403() throws Exception {
        Reponse temoin = api.creer(taches(planifProjet), corps(), gantt);
        Reponse ajout = api.creer(taches(planifProjet), corps(), sansDroit);
        Reponse modification = api.modifier(tache(planifProjet, tacheProjet), corps().avec(DESCRIPTION, "Pirate"), sansDroit);
        Reponse suppression = api.supprimer(tache(planifProjet, tacheProjet), sansDroit);
        Reponse intervenant = api.creer(intervenants(planifProjet), corps().avec(CODE, "KO").avec(DESCRIPTION, "D"), sansDroit);
        Reponse date = api.modifier(dateDebut(planifProjet), corps().avec("dateDebut", "2026-04-01"), sansDroit);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(ajout.statut()).as(ajout.corps()).isEqualTo(403);
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(403);
        s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(403);
        s.assertThat(intervenant.statut()).as(intervenant.corps()).isEqualTo(403);
        s.assertThat(date.statut()).as(date.corps()).isEqualTo(403);
        s.assertThat(fixture.lire(DESCRIPTION_TACHE, tacheProjet)).isEqualTo("Tâche 1");
        s.assertThat(fixture.compter(TACHES_DE, planifProjet)).isEqualTo(2);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE CfgGanttProjectId = ?", planifProjet)).isZero();
        s.assertAll();
    }

    @Test
    void d5_sansDroitGantt_clonageVersLeProjet403() throws Exception {
        UUID vide = parite.projet("N9V-" + jeton, parite.entiteJuridique("N9F-" + jeton));
        RequestPostProcessor ganttVide = connecte(parite.avecDroits("n9b.sec.gv." + jeton, List.of(DROIT_GANTT), vide),
                COMMERCIAL);
        RequestPostProcessor sansDroitVide = connecte(parite.avecDroits("n9b.sec.sv." + jeton, List.of(), vide), COMMERCIAL);

        Reponse refus = api.creer(planificationDuProjet(vide), corps().avec(MODELE_ID, modeleId), sansDroitVide);
        int apresRefus = fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE ObjectId = ?", vide);
        Reponse temoin = api.creer(planificationDuProjet(vide), corps().avec(MODELE_ID, modeleId), ganttVide);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(apresRefus).isZero();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertAll();
    }

    // D5 : la sauvegarde de version exige Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion.
    @Test
    void d5_sauvegardeDeVersion_sansDroitToolSaveVersion403() throws Exception {
        RequestPostProcessor versionneur = connecte(parite.avecDroits("n9b.sec.v." + jeton,
                List.of(DROIT_GANTT, DROIT_VERSION), projet), COMMERCIAL);

        Reponse refus = api.creer(versions(planifProjet), corps(), gantt);
        int apresRefus = fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE CfgGanttProjectMainVersionId = ?",
                planifProjet);
        Reponse temoin = api.creer(versions(planifProjet), corps(), versionneur);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(apresRefus).isZero();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertAll();
    }

    // ------------------------------------------------------------------ D6 : périmètre projet depuis la ligne stockée

    @Test
    void d6_planificationDUnProjetHorsPerimetre_lectureEtEcrituresRefusees() throws Exception {
        parite.genresDeLot();
        UUID lotCache = parite.lot(cache, "N9L-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "N9S-" + jeton));
        fixture.lotDeTache(tacheCachee, lotCache);

        Reponse temoin = api.lire(planification(planifProjet), admin);
        List<Reponse> refus = List.of(
                api.lire(planificationDuProjet(cache), admin),
                api.creer(planificationDuProjet(cache), corps().avec(MODELE_ID, modeleId), admin),
                api.lire(planification(planifCachee), admin),
                api.lire(tachesDuLot(lotCache), admin),
                api.creer(taches(planifCachee), corps(), admin),
                api.modifier(tache(planifCachee, tacheCachee), corps().avec(DESCRIPTION, "Pirate"), admin),
                api.supprimer(tache(planifCachee, tacheCachee), admin),
                api.creer(intervenants(planifCachee), corps().avec(CODE, "KO").avec(DESCRIPTION, "D"), admin),
                api.modifier(dateDebut(planifCachee), corps().avec("dateDebut", "2026-04-01"), admin),
                api.creer(versions(planifCachee), corps(), admin));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        refus.forEach(r -> s.assertThat(r.statut()).as(r.corps()).isIn(403, 404));
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE ObjectId = ?", cache)).isEqualTo(1);
        s.assertThat(fixture.compter(TACHES_DE, planifCachee)).isEqualTo(1);
        s.assertThat(fixture.lire(DESCRIPTION_TACHE, tacheCachee)).isEqualTo("Tâche 1");
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttProject WHERE CfgGanttProjectMainVersionId = ?",
                planifCachee)).isZero();
        s.assertAll();
    }

    // D6 : la tâche d'une planification cachée, adressée sous l'URL d'une planification visible, reste refusée.
    @Test
    void d6_tacheCachee_sousLURLDUnePlanificationVisible_refusee() throws Exception {
        Reponse temoin = api.modifier(tache(planifProjet, tacheProjet), corps().avec(DESCRIPTION, "Témoin"), admin);
        Reponse modification = api.modifier(tache(planifProjet, tacheCachee), corps().avec(DESCRIPTION, "Pirate"), admin);
        Reponse suppression = api.supprimer(tache(planifProjet, tacheCachee), admin);
        Reponse sousTache = api.creer(taches(planifProjet), corps().avec(PARENT, tacheCachee), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(modification.statut()).as(modification.corps()).isIn(400, 403, 404);
        s.assertThat(suppression.statut()).as(suppression.corps()).isIn(400, 403, 404);
        s.assertThat(sousTache.statut()).as(sousTache.corps()).isIn(400, 403, 404);
        s.assertThat(fixture.lire(DESCRIPTION_TACHE, tacheCachee)).isEqualTo("Tâche 1");
        s.assertThat(fixture.compter(TACHES_DE, planifCachee)).isEqualTo(1);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttTask WHERE ParentId = ?", tacheCachee)).isZero();
        s.assertAll();
    }

    // ------------------------------------------------------------------ D6 : ordre 404 → 403 → 400

    @Test
    void d6_ordre_inconnu404_puisSansDroit403_puisCorpsInvalide400() throws Exception {
        Reponse temoin = api.creer(taches(planifProjet), corps(), gantt);
        Reponse inconnue = api.creer(taches(UUID.randomUUID()), corps(), sansDroit);
        Reponse sansDroitCorpsInvalide = api.creer(taches(planifProjet), corps().avec(PARENT, tacheCachee), sansDroit);
        Reponse corpsInvalide = api.creer(taches(planifProjet), corps().avec(PARENT, tacheCachee), gantt);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
        s.assertThat(inconnue.statut()).as(inconnue.corps()).isEqualTo(404);
        s.assertThat(inconnue.corps()).doesNotContain(STATIC);
        s.assertThat(sansDroitCorpsInvalide.statut()).as(sansDroitCorpsInvalide.corps()).isEqualTo(403);
        s.assertThat(corpsInvalide.statut()).as(corpsInvalide.corps()).isEqualTo(400);
        s.assertAll();
    }
}
