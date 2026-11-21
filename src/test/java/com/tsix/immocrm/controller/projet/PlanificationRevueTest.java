package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePlanification;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.intervenant;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.intervenants;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.planification;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tache;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.taches;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.tachesDuLot;
import static com.tsix.immocrm.controller.projet.AppelsPlanification.versions;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue N9b : visibilité des versions, intervenants hors planification, versions en lecture seule, prédécesseurs après suppression. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationRevueTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 2);
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String OWNER_DESCRIPTION = "SELECT Description FROM CfgGanttOwner WHERE PKey = ?";
    private static final String RANG = "SELECT CAST(Sorting AS nvarchar(10)) FROM CfgGanttTask WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private AppelsPlanification api;
    private String jeton;
    private UUID planifProjet;
    private UUID planifCachee;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        api = new AppelsPlanification(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID societe = parite.entiteJuridique("N9R-" + jeton);
        UUID projet = parite.projet("N9RP-" + jeton, societe);
        UUID cache = parite.projet("N9RH-" + jeton, societe);
        admin = connecte(parite.administrateur("n9b.rev.a." + jeton, projet), "Administrateur");
        planifProjet = fixture.planificationProjet(projet, "RPRJ-" + jeton, DEBUT);
        planifCachee = fixture.planificationProjet(cache, "RCAC-" + jeton, DEBUT);
    }

    // D6 : une version (ObjectId NULL) hérite du périmètre de sa planification principale.
    @Test
    void d6_versionDUnProjetHorsPerimetre_404() throws Exception {
        UUID versionCachee = fixture.version(planifCachee, "RVC-" + jeton, DEBUT);
        UUID versionVisible = fixture.version(planifProjet, "RVV-" + jeton, DEBUT);

        Reponse temoin = api.lire(planification(versionVisible), admin);
        Reponse lecture = api.lire(planification(versionCachee), admin);
        Reponse liste = api.lire(versions(planifCachee), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(404);
        s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(404);
        s.assertAll();
    }

    // D6 : l'intervenant d'une planification cachée, adressé sous l'URL d'une planification visible, reste intact.
    @Test
    void d6_intervenantCache_sousLURLDUnePlanificationVisible_404() throws Exception {
        UUID cacheOwner = fixture.intervenant(planifCachee, "RO-" + jeton, true);

        Reponse modification = api.modifier(intervenant(planifProjet, cacheOwner), corps().avec(DESCRIPTION, "Pirate"), admin);
        Reponse suppression = api.supprimer(intervenant(planifProjet, cacheOwner), admin);
        Reponse direct = api.modifier(intervenant(planifCachee, cacheOwner), corps().avec(DESCRIPTION, "Pirate"), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(404);
        s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(404);
        s.assertThat(direct.statut()).as(direct.corps()).isEqualTo(404);
        s.assertThat(fixture.lire(OWNER_DESCRIPTION, cacheOwner)).isEqualTo("Intervenant RO-" + jeton);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE PKey = ?", cacheOwner)).isEqualTo(1);
        s.assertAll();
    }

    // R13 : une version ne s'édite pas, intervenants et ordre des tâches compris (409).
    @Test
    void r13_version_intervenantsEtOrdre_409() throws Exception {
        UUID version = fixture.version(planifProjet, "RVE-" + jeton, DEBUT);
        UUID owner = fixture.intervenant(version, "RVO-" + jeton, true);
        UUID t1 = fixture.tache(version, null, "1", 1, 0, DEBUT, 1);
        UUID t2 = fixture.tache(version, null, "2", 2, 0, DEBUT, 1);

        Reponse ajout = api.creer(intervenants(version), corps().avec(CODE, "X").avec(DESCRIPTION, "D"), admin);
        Reponse modification = api.modifier(intervenant(version, owner), corps().avec(DESCRIPTION, "Pirate"), admin);
        Reponse suppression = api.supprimer(intervenant(version, owner), admin);
        Reponse ordre = ordonner(version, List.of(t2, t1));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(ajout.statut()).as(ajout.corps()).isEqualTo(409);
        s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(409);
        s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(409);
        s.assertThat(ordre.statut()).as(ordre.corps()).isEqualTo(409);
        s.assertThat(fixture.compter("SELECT COUNT(*) FROM CfgGanttOwner WHERE CfgGanttProjectId = ?", version)).isEqualTo(1);
        s.assertThat(fixture.lire(RANG,t1)).isEqualTo("1");
        s.assertAll();
    }

    // D6 : réordonner les tâches d'une planification cachée → 404.
    @Test
    void d6_ordreSurPlanificationCachee_404() throws Exception {
        UUID t1 = fixture.tache(planifCachee, null, "1", 1, 0, DEBUT, 1);
        UUID t2 = fixture.tache(planifCachee, null, "2", 2, 0, DEBUT, 1);

        Reponse ordre = ordonner(planifCachee, List.of(t2, t1));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(ordre.statut()).as(ordre.corps()).isEqualTo(404);
        s.assertThat(fixture.lire(RANG,t1)).isEqualTo("1");
        s.assertAll();
    }

    // R10 : libellé de dépendance inconnu → 400 (pas 500).
    @Test
    void r10_typeDeDependanceInconnu_400() throws Exception {
        UUID a = fixture.tache(planifProjet, null, "1", 1, 0, DEBUT, 1);
        UUID b = fixture.tache(planifProjet, null, "2", 2, 0, DEBUT, 1);

        Reponse r = api.modifier(tache(planifProjet, b),
                corps().avec("predecesseurs", List.of(corps().avec("tacheId", a).avec("type", "INCONNU"))), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(fixture.lire("SELECT Predecessors FROM CfgGanttTask WHERE PKey = ?", b)).isNull();
        s.assertAll();
    }

    // R10 : supprimer une tâche décale les rangs ; le texte des prédécesseurs restants suit, le lien supprimé disparaît.
    @Test
    void r10_suppressionDUnPredecesseur_textesReecrits() throws Exception {
        UUID a = fixture.tache(planifProjet, null, "1", 1, 0, DEBUT, 1);
        fixture.tache(planifProjet, null, "2", 2, 0, DEBUT, 1);
        UUID c = fixture.tache(planifProjet, null, "3", 3, 0, DEBUT, 1);
        fixture.predecesseurs(c, "1|0;2|1");

        Reponse r = api.supprimer(tache(planifProjet, a), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(fixture.lire(RANG,c)).isEqualTo("2");
        s.assertThat(fixture.lire("SELECT Predecessors FROM CfgGanttTask WHERE PKey = ?", c)).isEqualTo("1|1");
        s.assertAll();
    }

    // R11 : intervenant d'une autre planification visible, ajouté à une tâche → 400.
    @Test
    void r11_intervenantDUneAutrePlanificationVisible_400() throws Exception {
        UUID modele = fixture.modele("RMO-" + jeton, DEBUT);
        UUID autre = fixture.intervenant(modele, "RAO-" + jeton, true);
        UUID t = fixture.tache(planifProjet, null, "1", 1, 0, DEBUT, 1);

        Reponse r = api.modifier(tache(planifProjet, t), corps().avec("intervenants", List.of(autre)), admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(fixture.lire("SELECT ResourceNames FROM CfgGanttTask WHERE PKey = ?", t)).isNull();
        s.assertAll();
    }

    // FEATURE-visibilite-omran.md R1 : un lot au statut invisible du compte restreint = 404, y compris sous /lots/{id}/taches.
    @Test
    void r1_tachesDUnLotAuStatutInvisible_404CommeLaFicheDuLot() throws Exception {
        UUID fournisseur = lots.fournisseur("RF-" + jeton);
        UUID projetA = lots.projet("RPA-" + jeton);
        String roleU = "RRU-" + jeton;
        var compteU = visibilite.compte("n9b.rev.u." + jeton, roleU, true);
        UUID entite = visibilite.entite("RLOTV-" + jeton);
        UUID visibleS = visibilite.statut(entite, "RS1-" + jeton);
        UUID cacheS = visibilite.statut(entite, "RS2-" + jeton);
        visibilite.intervenantRole(entite, visibleS, visibilite.idRole(roleU));
        visibilite.intervenantRole(entite, cacheS, visibilite.role("RRX-" + jeton));
        UUID entiteProjet = visibilite.entite("RPRJV-" + jeton);
        UUID sp = visibilite.statut(entiteProjet, "RSP-" + jeton);
        visibilite.intervenantRole(entiteProjet, sp, visibilite.idRole(roleU));
        visibilite.statutProjet(projetA, entiteProjet, sp);
        UUID lotVisible = lots.lot(projetA, "RL1-" + jeton, fournisseur);
        visibilite.statutLot(lotVisible, entite, visibleS);
        UUID lotCache = lots.lot(projetA, "RL2-" + jeton, fournisseur);
        visibilite.statutLot(lotCache, entite, cacheS);
        lots.habiliter(compteU, projetA);
        RequestPostProcessor u = connecte(compteU, roleU);
        UUID planif = fixture.planificationProjet(projetA, "RPA-" + jeton, DEBUT);
        fixture.lotDeTache(fixture.tache(planif, null, "1", 1, 0, DEBUT, 1), lotVisible);
        fixture.lotDeTache(fixture.tache(planif, null, "2", 2, 0, DEBUT, 1), lotCache);

        Reponse ficheCachee = api.lire("/api/projet/lots/" + lotCache, u);
        Reponse temoin = api.lire(tachesDuLot(lotVisible), u);
        Reponse tachesCachees = api.lire(tachesDuLot(lotCache), u);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(ficheCachee.statut()).as("fiche du lot invisible : " + ficheCachee.corps()).isEqualTo(404);
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(tachesCachees.statut()).as(tachesCachees.corps()).isEqualTo(404);
        s.assertAll();
    }

    private Reponse ordonner(UUID planification, List<UUID> ids) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(put(taches(planification) + "/ordre").with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec("tacheIds", ids).json()))
                .andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }
}
