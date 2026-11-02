package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFichesVerrouillees;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.ETAT;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.NON_VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.SESSION;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.FERMEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.OUVERTE;
import static com.tsix.immocrm.fixtures.FixtureSessions.UTILISATEUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Déverrouillages, relus en SQL natif — FEATURE-admin-fiches-verrouillees.md, règles 7, 8, 9 et décisions du manager. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichesVerrouilleesDeverrouillageTest {

    private static final String URL = "/api/administration/fiches-verrouillees";
    private static final String URL_DEVERROUILLER = URL + "/{id}/deverrouiller";
    private static final String URL_TOUT = URL + "/deverrouiller-tout";
    private static final String LIBEREE = String.valueOf(NON_VERROUILLEE);
    private static final String ETAT_NON_VERROUILLEE = "NON_VERROUILLEE";
    private static final String NOMBRE = "$.nombre";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions sessions;
    @Autowired private FixtureFichesVerrouillees fixture;

    private AdmUser admin;
    private String marqueur;
    private UUID session;

    @BeforeEach
    void setUp() {
        sessions.assurerTables();
        fixture.assurerTable();
        admin = patrimoine.administrateur();
        marqueur = UUID.randomUUID().toString();
        session = sessions.session(admin, DATE).inserer();
    }

    private ResultActions deverrouiller(UUID id) throws Exception {
        return mockMvc.perform(post(URL_DEVERROUILLER, id).with(connecte(admin)));
    }

    private ResultActions toutDeverrouiller() throws Exception {
        return mockMvc.perform(post(URL_TOUT).with(connecte(admin)));
    }

    private FixtureFichesVerrouillees.Ligne verrou() {
        return fixture.verrou(session, marqueur, DATE);
    }

    private UUID verrouAvecEtat(int etat) {
        return verrou().avec(ETAT, etat).inserer();
    }

    private static Map<String, String> horsEtat(Map<String, String> ligne) {
        Map<String, String> reste = new HashMap<>(ligne);
        reste.remove(ETAT);
        return reste;
    }

    // ------------------------------------------------------------------ règle 7 : déverrouiller

    @Test
    void r7_deverrouiller_state2_relueEnSqlNatif() throws Exception {
        UUID id = verrou().inserer();

        deverrouiller(id).andExpect(status().isOk());

        assertThat(fixture.ligne(id)).containsEntry(ETAT, LIBEREE);
    }

    @Test
    void r7_aucuneAutreColonneModifiee() throws Exception {
        UUID id = verrou().inserer();
        Map<String, String> avant = fixture.ligne(id);

        deverrouiller(id).andExpect(status().isOk());

        assertThat(horsEtat(fixture.ligne(id))).isEqualTo(horsEtat(avant));
    }

    @Test
    void r7_reponse_ligneAJour() throws Exception {
        UUID compteSession = sessions.session(admin, DATE).avec(UTILISATEUR, "Karim Résidence").inserer();
        UUID id = fixture.verrou(compteSession, marqueur, DATE).inserer();

        deverrouiller(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.entite").value("Settlement"))
                .andExpect(jsonPath("$.libelle").value("Fiche " + marqueur))
                .andExpect(jsonPath("$.dateVerrouillage", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath("$.utilisateur").value("Karim Résidence"))
                .andExpect(jsonPath("$.etat").value(ETAT_NON_VERROUILLEE));
    }

    // Aucune session à cette PKey : ci/schema/160 n'a pas de FK, pour que ce cas reste testable.
    @Test
    void r7_sessionAbsente_deverrouillee_utilisateurNull() throws Exception {
        UUID id = verrou().avec(SESSION, UUID.randomUUID()).inserer();

        deverrouiller(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateur").doesNotExist())
                .andExpect(jsonPath("$.etat").value(ETAT_NON_VERROUILLEE));
    }

    @Test
    void r7_listeRelue_ligneNonVerrouillee() throws Exception {
        UUID id = verrou().inserer();

        deverrouiller(id).andExpect(status().isOk());

        mockMvc.perform(get(URL).with(connecte(admin)).param("recherche", marqueur).param("etat", "VERROUILLEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(fixture.ligne(id)).containsEntry(ETAT, LIBEREE);
    }

    @Test
    void r7_lesAutresLignesNeBougentPas() throws Exception {
        UUID id = verrou().inserer();
        UUID temoin = verrou().inserer();
        Map<String, String> avant = fixture.ligne(temoin);

        deverrouiller(id).andExpect(status().isOk());

        assertThat(fixture.ligne(temoin)).isEqualTo(avant);
    }

    @ParameterizedTest
    @ValueSource(ints = {NON_VERROUILLEE, 0, 3})
    void r7_dejaNonVerrouillee_409_ligneInchangee(int etat) throws Exception {
        UUID id = verrouAvecEtat(etat);
        Map<String, String> avant = fixture.ligne(id);

        deverrouiller(id).andExpect(status().isConflict());

        assertThat(fixture.ligne(id)).isEqualTo(avant);
    }

    @Test
    void r7_secondDeverrouillage_409() throws Exception {
        UUID id = verrou().inserer();
        deverrouiller(id).andExpect(status().isOk());

        deverrouiller(id).andExpect(status().isConflict());

        assertThat(fixture.ligne(id)).containsEntry(ETAT, LIBEREE);
    }

    // Un 404 seul est vert tant que la route manque : le verrou connu prouve que la route répond.
    @Test
    void r7_idInconnu_404_routeExistante() throws Exception {
        UUID connu = verrou().inserer();

        assertAll(
                () -> deverrouiller(connu).andExpect(status().isOk()),
                () -> deverrouiller(UUID.randomUUID()).andExpect(status().isNotFound()));
    }

    // ------------------------------------------------------------------ décisions du manager

    @ParameterizedTest
    @ValueSource(ints = {OUVERTE, FERMEE})
    void decision_etatDeLaSession_nEmpechePasDeDeverrouiller(int etatSession) throws Exception {
        UUID saSession = sessions.session(admin, DATE).avec(FixtureSessions.ETAT, etatSession).inserer();
        UUID id = fixture.verrou(saSession, marqueur, DATE).inserer();

        deverrouiller(id).andExpect(status().isOk());

        assertThat(fixture.ligne(id)).containsEntry(ETAT, LIBEREE);
    }

    // ------------------------------------------------------------------ règle 8 : déverrouiller tout

    @Test
    void r8_toutes_lesLignesState1_passentA2_nombreLignesTouchees() throws Exception {
        UUID une = verrou().inserer();
        UUID autre = verrou().inserer();
        int actifs = fixture.actifs();

        toutDeverrouiller().andExpect(status().isOk()).andExpect(jsonPath(NOMBRE).value(actifs));

        assertAll(
                () -> assertThat(actifs).isGreaterThanOrEqualTo(2),
                () -> assertThat(fixture.ligne(une)).containsEntry(ETAT, LIBEREE),
                () -> assertThat(fixture.ligne(autre)).containsEntry(ETAT, LIBEREE),
                () -> assertThat(fixture.actifs()).isZero());
    }

    @Test
    void r8_lignesTouchees_seulLEtatChange() throws Exception {
        UUID id = verrou().inserer();
        Map<String, String> avant = fixture.ligne(id);

        toutDeverrouiller().andExpect(status().isOk());

        assertThat(horsEtat(fixture.ligne(id))).isEqualTo(horsEtat(avant));
    }

    @ParameterizedTest
    @ValueSource(ints = {NON_VERROUILLEE, 0, 3})
    void r8_lignesNonState1_gardentLeursColonnes_etNeComptentPas(int etat) throws Exception {
        fixture.aucunVerrouActif();
        UUID active = verrou().inserer();
        UUID autre = verrouAvecEtat(etat);
        Map<String, String> avant = fixture.ligne(autre);

        toutDeverrouiller().andExpect(status().isOk()).andExpect(jsonPath(NOMBRE).value(1));

        assertAll(
                () -> assertThat(fixture.ligne(autre)).isEqualTo(avant),
                () -> assertThat(fixture.ligne(active)).containsEntry(ETAT, LIBEREE));
    }

    @Test
    void r8_aucunVerrouActif_200_nombreZero() throws Exception {
        fixture.aucunVerrouActif();
        verrouAvecEtat(NON_VERROUILLEE);

        toutDeverrouiller().andExpect(status().isOk()).andExpect(jsonPath(NOMBRE).value(0));
    }

    @Test
    void r8_reponse_seulementNombre() throws Exception {
        String reponse = toutDeverrouiller().andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> corps = JsonPath.read(reponse, "$");

        assertThat(corps).containsOnlyKeys("nombre");
    }

    @Test
    void back_deverrouillerTout_neTientAucunCompteDesFiltres() throws Exception {
        UUID horsFiltre = verrou().inserer();

        mockMvc.perform(post(URL_TOUT).with(connecte(admin))
                        .param("etat", ETAT_NON_VERROUILLEE)
                        .param("recherche", "aucune-ligne-" + marqueur)
                        .param("dateDebut", "2001-01-01")
                        .param("dateFin", "2001-01-01"))
                .andExpect(status().isOk());

        assertThat(fixture.ligne(horsFiltre)).containsEntry(ETAT, LIBEREE);
    }

    // ------------------------------------------------------------------ règle 9 : rien d'autre ne bouge

    // Compte de toute la table : elle n'est écrite que par les tests de ce lot, aucune insertion ne vient d'ailleurs.
    @Test
    void r9_deverrouiller_niInsertionNiSuppression_sessionIntacte() throws Exception {
        UUID id = verrou().inserer();
        int lignes = fixture.lignes();
        Map<String, String> sessionAvant = sessions.ligne(session);

        deverrouiller(id).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignes()).isEqualTo(lignes),
                () -> assertThat(sessions.ligne(session)).isEqualTo(sessionAvant));
    }

    @Test
    void r9_deverrouillerTout_niInsertionNiSuppression_sessionIntacte() throws Exception {
        verrou().inserer();
        verrouAvecEtat(NON_VERROUILLEE);
        int lignes = fixture.lignes();
        Map<String, String> sessionAvant = sessions.ligne(session);

        toutDeverrouiller().andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignes()).isEqualTo(lignes),
                () -> assertThat(sessions.ligne(session)).isEqualTo(sessionAvant));
    }

    // Session dont l'état reste ouvert : la garde porte sur le verrou seul, jamais sur la session.
    @Test
    void r9_verrouDejaLibre_409_sessionIntacte() throws Exception {
        UUID id = verrouAvecEtat(NON_VERROUILLEE);
        Map<String, String> sessionAvant = sessions.ligne(session);

        deverrouiller(id).andExpect(status().isConflict());

        assertThat(sessions.ligne(session)).isEqualTo(sessionAvant).containsEntry(FixtureSessions.ETAT,
                String.valueOf(OUVERTE));
    }

    // Garde de fixture, verte sans le code : un état distinct de 1 reste lisible tel quel en base.
    @Test
    void fixture_etatRelu() {
        assertThat(fixture.ligne(verrouAvecEtat(VERROUILLEE))).containsEntry(ETAT, String.valueOf(VERROUILLEE));
    }
}
