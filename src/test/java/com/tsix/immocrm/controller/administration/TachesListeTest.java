package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTaches.Statut;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin.TableTache;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTaches.ATTENTE;
import static com.tsix.immocrm.fixtures.FixtureTaches.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTaches.FICHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.NON_COMMENCEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.PRIORITE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.RACINE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUTS_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TEXTE;
import static com.tsix.immocrm.fixtures.FixtureTaches.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.permis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lecture des tâches — FEATURE-admin-taches.md, règle 1, filtres, {@code TacheDto}, {@code GET …/statuts}, décision tiers. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TachesListeTest {

    private static final String TOTAL = "$.totalElements";
    private static final String CODES = "$.content[*].code";
    private static final String ID_LIGNE_1 = "$.content[0].id";
    private static final LocalDateTime LE_2 = LocalDateTime.of(2026, 11, 2, 9, 30);
    private static final String DEBUT_LE_2 = "2026-11-02T09:30";

    static final List<Statut> STATUTS_DU_WORKFLOW = STATUTS_TACHE;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureTaches fixture;
    @Autowired private FixtureTachesAdmin referentiels;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private ResultActions lister(String... parametres) throws Exception {
        var requete = get(RACINE).with(connecte(admin)).param(TEXTE, jeton);
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete).andExpect(status().isOk());
    }

    private ResultActions lire(UUID id) throws Exception {
        return mockMvc.perform(get(FICHE, id).with(connecte(admin))).andExpect(status().isOk());
    }

    private String code(String suffixe) {
        return suffixe + "-" + jeton;
    }

    private static TableTache table(boolean priorite) {
        return FixtureTachesAdmin.tables().filter(t -> t.avecTri() == priorite).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ règle 1

    @Test
    void r1_liste_neRenvoieQueLEntiteTache_jamaisUnRendezVous() throws Exception {
        UUID tache = fixture.tache(code("TAC"));
        fixture.rendezVous(code("RDV"));

        lister().andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(tache.toString()));
    }

    @Test
    void r1_liste_trieeParDatePrevue() throws Exception {
        fixture.definir(fixture.tache(code("C")), "StartDatePrev", LocalDateTime.of(2026, 3, 3, 8, 0));
        fixture.definir(fixture.tache(code("A")), "StartDatePrev", LocalDateTime.of(2026, 1, 1, 8, 0));
        fixture.definir(fixture.tache(code("B")), "StartDatePrev", LocalDateTime.of(2026, 2, 2, 8, 0));

        lister().andExpect(jsonPath(CODES, contains(code("A"), code("B"), code("C"))));
    }

    @Test
    void liste_pagineeParPageEtSize() throws Exception {
        fixture.definir(fixture.tache(code("A")), "StartDatePrev", LocalDateTime.of(2026, 1, 1, 8, 0));
        fixture.definir(fixture.tache(code("B")), "StartDatePrev", LocalDateTime.of(2026, 2, 2, 8, 0));

        lister("page", "1", "size", "1")
                .andExpect(jsonPath(TOTAL).value(2))
                .andExpect(jsonPath(CODES, contains(code("B"))));
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void filtre_statutId() throws Exception {
        fixture.tache(code("A"), NON_COMMENCEE);
        UUID retenue = fixture.tache(code("B"), EN_COURS);

        lister(STATUT_ID, EN_COURS.fil())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(retenue.toString()));
    }

    @Test
    void filtre_typeId() throws Exception {
        UUID type = referentiels.ligne(table(false), code("TYP"));
        UUID autre = referentiels.ligne(table(false), code("TYQ"));
        fixture.definir(fixture.tache(code("A")), "AdmTaskTypeId", autre);
        UUID retenue = fixture.tache(code("B"));
        fixture.definir(retenue, "AdmTaskTypeId", type);

        lister(TYPE_ID, type.toString())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(retenue.toString()));
    }

    @Test
    void filtre_prioriteId() throws Exception {
        UUID priorite = referentiels.ligne(table(true), code("PRI"));
        UUID autre = referentiels.ligne(table(true), code("PRJ"));
        fixture.definir(fixture.tache(code("A")), "AdmTaskPriorityId", autre);
        UUID retenue = fixture.tache(code("B"));
        fixture.definir(retenue, "AdmTaskPriorityId", priorite);

        lister(PRIORITE_ID, priorite.toString())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(retenue.toString()));
    }

    @Test
    void filtre_gestionnaireId() throws Exception {
        UUID gestionnaire = terrains.fournisseur(code("GES"), null);
        UUID autre = terrains.fournisseur(code("GET"), null);
        fixture.definir(fixture.tache(code("A")), "CfgTierManagerId", autre);
        UUID retenue = fixture.tache(code("B"));
        fixture.definir(retenue, "CfgTierManagerId", gestionnaire);

        lister(GESTIONNAIRE_ID, gestionnaire.toString())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(retenue.toString()));
    }

    @Test
    void filtre_texte_surLeCode() throws Exception {
        String seul = "CODEUNIQUE" + jeton;
        fixture.tache(seul);
        fixture.tache(code("AUTRE"));

        mockMvc.perform(get(RACINE).with(connecte(admin)).param(TEXTE, seul))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(CODES, contains(seul)));
    }

    @Test
    void filtre_texte_surLaDescription() throws Exception {
        UUID tache = fixture.tache("SANSJETON" + FixtureReferentielsAdmin.jeton());
        fixture.definir(tache, "Description", "Relancer " + jeton);
        fixture.tache("HORS" + FixtureReferentielsAdmin.jeton());

        lister().andExpect(jsonPath(TOTAL).value(1)).andExpect(jsonPath(ID_LIGNE_1).value(tache.toString()));
    }

    // ------------------------------------------------------------------ TacheDto

    @Test
    void fiche_exposeLesChampsDeTacheDto() throws Exception {
        UUID type = referentiels.ligne(table(false), code("TYP"));
        UUID priorite = referentiels.ligne(table(true), code("PRI"));
        UUID declencheur = terrains.fournisseur(code("DEC"), null);
        UUID gestionnaire = terrains.fournisseur(code("GES"), null);
        UUID assigne = terrains.fournisseur(code("ASS"), null);
        UUID module = fixture.module(code("MOD"));
        UUID entite = patrimoine.entite(code("ENT"), true).getId();
        UUID projet = fixture.projet(code("PRJ"), entite);
        UUID id = fixture.tache(code("TAC"), EN_COURS);
        fixture.definir(id, "AdmTaskTypeId", type);
        fixture.definir(id, "AdmTaskPriorityId", priorite);
        fixture.definir(id, "CfgTierTriggerId", declencheur);
        fixture.definir(id, "CfgTierManagerId", gestionnaire);
        fixture.definir(id, "CfgTierAssignmentId", assigne);
        fixture.definir(id, "AdmModuleId", module);
        fixture.definir(id, "CfgCompanyId", entite);
        fixture.definir(id, "PrjProjectId", projet);
        fixture.definir(id, "StartDatePrev", LE_2);
        fixture.definir(id, "DelayPrev", 3);
        fixture.definir(id, "DueDatePrev", LocalDateTime.of(2026, 11, 5, 10, 0));
        fixture.definir(id, "StartDate", LocalDateTime.of(2026, 11, 3, 11, 0));
        fixture.definir(id, "Delay", 4);
        fixture.definir(id, "EndDate", LocalDateTime.of(2026, 11, 7, 12, 0));
        fixture.definir(id, "Notes", "Notes " + jeton);

        String json = lire(id)
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value(code("TAC")))
                .andExpect(jsonPath("$.description").value("Tache " + code("TAC")))
                .andExpect(jsonPath("$.typeId").value(type.toString()))
                .andExpect(jsonPath("$.typeLibelle").value("Libelle " + code("TYP")))
                .andExpect(jsonPath("$.declencheurId").value(declencheur.toString()))
                .andExpect(jsonPath("$.declencheurNom").value("Fournisseur " + code("DEC")))
                .andExpect(jsonPath("$.dureePrevue").value(3))
                .andExpect(jsonPath("$.duree").value(4))
                .andExpect(jsonPath("$.moduleId").value(module.toString()))
                .andExpect(jsonPath("$.prioriteId").value(priorite.toString()))
                .andExpect(jsonPath("$.prioriteLibelle").value("Libelle " + code("PRI")))
                .andExpect(jsonPath("$.gestionnaireId").value(gestionnaire.toString()))
                .andExpect(jsonPath("$.gestionnaireNom").value("Fournisseur " + code("GES")))
                .andExpect(jsonPath("$.assigneId").value(assigne.toString()))
                .andExpect(jsonPath("$.assigneNom").value("Fournisseur " + code("ASS")))
                .andExpect(jsonPath("$.entiteId").value(entite.toString()))
                .andExpect(jsonPath("$.projetId").value(projet.toString()))
                .andExpect(jsonPath("$.notes").value("Notes " + jeton))
                .andExpect(jsonPath("$.statutId").value(EN_COURS.fil()))
                .andExpect(jsonPath("$.statutLibelle").value(EN_COURS.libelle()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(List.<String>of(JsonPath.read(json, "$.datePrevue"), JsonPath.read(json, "$.dateFinPrevue"),
                JsonPath.read(json, "$.dateDebut"), JsonPath.read(json, "$.dateFin")))
                .satisfiesExactly(
                        d -> assertThat(d).startsWith(DEBUT_LE_2),
                        d -> assertThat(d).startsWith("2026-11-05T10:00"),
                        d -> assertThat(d).startsWith("2026-11-03T11:00"),
                        d -> assertThat(d).startsWith("2026-11-07T12:00"));
    }

    @Test
    void liste_porteLesLibellesDesColonnes() throws Exception {
        UUID type = referentiels.ligne(table(false), code("TYP"));
        UUID priorite = referentiels.ligne(table(true), code("PRI"));
        UUID gestionnaire = terrains.fournisseur(code("GES"), null);
        UUID id = fixture.tache(code("TAC"), ATTENTE);
        fixture.definir(id, "AdmTaskTypeId", type);
        fixture.definir(id, "AdmTaskPriorityId", priorite);
        fixture.definir(id, "CfgTierManagerId", gestionnaire);

        lister()
                .andExpect(jsonPath("$.content[0].typeLibelle").value("Libelle " + code("TYP")))
                .andExpect(jsonPath("$.content[0].prioriteLibelle").value("Libelle " + code("PRI")))
                .andExpect(jsonPath("$.content[0].gestionnaireNom").value("Fournisseur " + code("GES")))
                .andExpect(jsonPath("$.content[0].statutLibelle").value(ATTENTE.libelle()));
    }

    /** « Base » du spec : un statut vide est traité comme « Non commencée ». */
    @Test
    void statutVide_afficheNonCommencee() throws Exception {
        UUID id = fixture.tache(code("TAC"));

        lire(id)
                .andExpect(jsonPath("$.statutId").value(NON_COMMENCEE.fil()))
                .andExpect(jsonPath("$.statutLibelle").value(NON_COMMENCEE.libelle()));
    }

    /** Décision du manager : un tiers hors du type EMP, déjà en base, reste affiché dans la fiche. */
    @Test
    void decision_tiersHorsPersonnels_resteAffiche() throws Exception {
        UUID fournisseur = terrains.fournisseur(code("FOU"), null);
        UUID id = fixture.tache(code("TAC"));
        fixture.definir(id, "CfgTierTriggerId", fournisseur);

        lire(id)
                .andExpect(jsonPath("$.declencheurId").value(fournisseur.toString()))
                .andExpect(jsonPath("$.declencheurNom").value("Fournisseur " + code("FOU")));
    }

    // ------------------------------------------------------------------ GET …/statuts

    @Test
    void statuts_sixStatuts_dansLOrdreSorting() throws Exception {
        List<String> attendus = STATUTS_TACHE.stream().sorted(Comparator.comparingInt(Statut::ordre)).map(Statut::fil).toList();

        mockMvc.perform(get(STATUTS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(attendus.toArray())));
    }

    /** Noms du fil des transitions absents du spec : seules les PKey des statuts d'arrivée sont lues. */
    @ParameterizedTest(name = "{0}")
    @FieldSource("STATUTS_DU_WORKFLOW")
    void statuts_chacunPorteSesTransitions(Statut statut) throws Exception {
        String json = mockMvc.perform(get(STATUTS).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String ligne = JsonPath.read(json, "$[?(@.id=='" + statut.fil() + "')]").toString();

        List<Statut> permis = permis(statut);
        // AssertJ refuse contains(liste vide) ; statut final : aucun autre statut dans sa ligne, donc aucune transition.
        permis.forEach(s -> assertThat(ligne).contains(s.fil()));
        assertThat(STATUTS_TACHE.stream().filter(s -> !s.equals(statut) && !permis.contains(s)).map(Statut::fil).toList())
                .isNotEmpty().allSatisfy(id -> assertThat(ligne).doesNotContain(id));
    }
}
