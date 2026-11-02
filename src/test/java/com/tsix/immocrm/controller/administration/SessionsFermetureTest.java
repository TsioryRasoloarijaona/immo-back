package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureSessions.ADRESSE_IP;
import static com.tsix.immocrm.fixtures.FixtureSessions.COMPTE;
import static com.tsix.immocrm.fixtures.FixtureSessions.ETAT;
import static com.tsix.immocrm.fixtures.FixtureSessions.FERMEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.FIN;
import static com.tsix.immocrm.fixtures.FixtureSessions.ORIGINE;
import static com.tsix.immocrm.fixtures.FixtureSessions.OUVERTE;
import static com.tsix.immocrm.fixtures.FixtureSessions.UTILISATEUR;
import static com.tsix.immocrm.fixtures.FixtureSessions.VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.VERSION;
import static com.tsix.immocrm.fixtures.FixtureSessions.WEB_SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fermeture d'une session, relue en SQL natif — FEATURE-admin-sessions.md, règles 7 et 8 (réponse). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionsFermetureTest {

    private static final String URL = "/api/administration/sessions";
    private static final String URL_FERMER = URL + "/{id}/fermer";
    private static final String ETAT_FERME = String.valueOf(FERMEE);
    private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 3, 10, 9, 15, 30);
    private static final LocalDateTime FIN_ANCIENNE = LocalDateTime.of(2026, 3, 10, 18, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions fixture;
    @Autowired private Clock clock;

    private AdmUser admin;
    private AdmUser proprietaire;

    @BeforeEach
    void setUp() {
        fixture.assurerTables();
        admin = patrimoine.administrateur();
        proprietaire = patrimoine.administrateur();
    }

    private ResultActions fermer(UUID id) throws Exception {
        return mockMvc.perform(post(URL_FERMER, id).with(connecte(admin)));
    }

    private static String corps(ResultActions reponse) throws Exception {
        return reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private FixtureSessions.Ligne session(int etat) {
        return fixture.session(proprietaire, DEBUT).avec(ETAT, etat);
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.now(clock);
    }

    private static Map<String, String> horsEtatEtDate(Map<String, String> ligne) {
        Map<String, String> reste = new HashMap<>(ligne);
        reste.remove(ETAT);
        reste.remove(FIN);
        return reste;
    }

    private void fermeeMaintenant(UUID id, LocalDateTime avant) {
        Map<String, String> ligne = fixture.ligne(id);
        assertThat(ligne).containsEntry(ETAT, ETAT_FERME).containsKey(FIN);
        assertThat(LocalDateTime.parse(ligne.get(FIN)))
                .isBetween(avant.minusSeconds(1), maintenant().plusSeconds(1));
    }

    // Garde de fixture, verte sans le code : la FK vers AdmUser existe en CI, pas dans yesRemsTest.
    @Test
    void fixture_userIdPointeUnCompteReel() {
        assertThat(fixture.comptesReels(session(OUVERTE).inserer())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règle 7 : fermeture

    @ParameterizedTest
    @ValueSource(ints = {OUVERTE, VERROUILLEE})
    void r7_fermer_state4_endDateMaintenant(int etat) throws Exception {
        UUID id = session(etat).inserer();
        LocalDateTime avant = maintenant();

        fermer(id).andExpect(status().isOk());

        fermeeMaintenant(id, avant);
    }

    @ParameterizedTest
    @ValueSource(ints = {OUVERTE, VERROUILLEE})
    void r7_aucuneAutreColonneModifiee(int etat) throws Exception {
        UUID id = session(etat).avec(ORIGINE, fixture.origine()).inserer();
        Map<String, String> avant = fixture.ligne(id);

        fermer(id).andExpect(status().isOk());

        assertThat(horsEtatEtDate(fixture.ligne(id))).isEqualTo(horsEtatEtDate(avant))
                .containsEntry(COMPTE, proprietaire.getId().toString().toUpperCase(Locale.ROOT))
                .containsKeys(WEB_SESSION, UTILISATEUR, FixtureSessions.DEBUT, ADRESSE_IP, VERSION, ORIGINE);
    }

    // La section Back pose EndDate = maintenant sans exception pour une session ouverte qui a déjà une date.
    @Test
    void r7_sessionOuverteAvecEndDate_dateRemplacee() throws Exception {
        UUID id = session(OUVERTE).avec(FIN, FIN_ANCIENNE).inserer();
        LocalDateTime avant = maintenant();

        fermer(id).andExpect(status().isOk());

        fermeeMaintenant(id, avant);
    }

    @Test
    void r7_reponse_ligneAJour() throws Exception {
        UUID id = session(OUVERTE).avec(UTILISATEUR, "Karim Résidence").avec(ADRESSE_IP, "poste/10.0.0.7").inserer();

        String reponse = corps(fermer(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.utilisateur").value("Karim Résidence"))
                .andExpect(jsonPath("$.dateOuverture", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath("$.etat").value("FERMEE"))
                .andExpect(jsonPath("$.adresseIp").value("poste/10.0.0.7"))
                .andExpect(jsonPath("$.version").value("Desktop: Version 4.5.57.0")));

        String dateFermeture = JsonPath.read(reponse, "$.dateFermeture");
        assertThat(LocalDateTime.parse(dateFermeture))
                .isCloseTo(LocalDateTime.parse(fixture.ligne(id).get(FIN)), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void r7_listeRelue_sessionNonOuverte() throws Exception {
        String adresse = "poste/" + UUID.randomUUID();
        UUID id = session(OUVERTE).avec(ADRESSE_IP, adresse).inserer();

        fermer(id).andExpect(status().isOk());

        mockMvc.perform(get(URL).with(connecte(admin)).param("ip", adresse).param("ouverte", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(fixture.ligne(id)).containsEntry(ETAT, ETAT_FERME);
    }

    @Test
    void r7_lesAutresSessionsNeBougentPas() throws Exception {
        UUID id = session(OUVERTE).inserer();
        UUID temoin = session(OUVERTE).inserer();
        Map<String, String> avant = fixture.ligne(temoin);

        fermer(id).andExpect(status().isOk());

        assertThat(fixture.ligne(temoin)).isEqualTo(avant).doesNotContainKey(FIN);
    }

    // ------------------------------------------------------------------ règle 7 : session déjà fermée

    @ParameterizedTest
    @ValueSource(ints = {FERMEE, 0, 1, 3, 16})
    void r7_dejaFermee_409_ligneInchangee(int etat) throws Exception {
        UUID id = session(etat).avec(FIN, FIN_ANCIENNE).inserer();
        Map<String, String> avant = fixture.ligne(id);

        fermer(id).andExpect(status().isConflict());

        assertThat(fixture.ligne(id)).isEqualTo(avant).containsKey(FIN);
    }

    @Test
    void r7_fermeeSansEndDate_409_dateResteVide() throws Exception {
        UUID id = session(FERMEE).inserer();

        fermer(id).andExpect(status().isConflict());

        assertThat(fixture.ligne(id)).containsEntry(ETAT, ETAT_FERME).doesNotContainKey(FIN);
    }

    @Test
    void r7_secondeFermeture_409_datePremiereConservee() throws Exception {
        UUID id = session(OUVERTE).inserer();
        fermer(id).andExpect(status().isOk());
        Map<String, String> apresPremiere = fixture.ligne(id);

        fermer(id).andExpect(status().isConflict());

        assertThat(fixture.ligne(id)).isEqualTo(apresPremiere);
    }

    // ------------------------------------------------------------------ règle 7 : id inconnu

    // Un 404 seul est vert tant que la route manque : la session connue prouve que la route répond.
    @Test
    void r7_idInconnu_404_routeExistante() throws Exception {
        UUID connue = session(OUVERTE).inserer();

        assertAll(
                () -> fermer(connue).andExpect(status().isOk()),
                () -> fermer(UUID.randomUUID()).andExpect(status().isNotFound()));
    }

    // ------------------------------------------------------------------ règle 8 : réponse

    @Test
    void r8_reponse_niWebSessionNiOrigine() throws Exception {
        UUID origine = fixture.origine();
        UUID id = session(OUVERTE).avec(ORIGINE, origine).inserer();

        String reponse = corps(fermer(id).andExpect(status().isOk()));

        assertThat(reponse)
                .doesNotContain(fixture.ligne(id).get(WEB_SESSION))
                .doesNotContainIgnoringCase(origine.toString())
                .doesNotContainIgnoringCase(WEB_SESSION)
                .doesNotContainIgnoringCase(ORIGINE);
    }

    @Test
    void r8_refus409_sansWebSession() throws Exception {
        UUID id = session(FERMEE).inserer();

        String reponse = corps(fermer(id).andExpect(status().isConflict()));

        assertThat(reponse).doesNotContain(fixture.ligne(id).get(WEB_SESSION)).doesNotContainIgnoringCase(WEB_SESSION);
    }
}
