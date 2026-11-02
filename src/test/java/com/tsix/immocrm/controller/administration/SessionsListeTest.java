package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureSessions.ADRESSE_IP;
import static com.tsix.immocrm.fixtures.FixtureSessions.ETAT;
import static com.tsix.immocrm.fixtures.FixtureSessions.FERMEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.FIN;
import static com.tsix.immocrm.fixtures.FixtureSessions.OUVERTE;
import static com.tsix.immocrm.fixtures.FixtureSessions.UTILISATEUR;
import static com.tsix.immocrm.fixtures.FixtureSessions.VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.VERSION;
import static com.tsix.immocrm.fixtures.FixtureSessions.WEB_SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste des sessions et liste des utilisateurs — FEATURE-admin-sessions.md, règles 1 à 6 et section Back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionsListeTest {

    private static final String URL = "/api/administration/sessions";
    private static final String URL_UTILISATEURS = URL + "/utilisateurs";
    private static final String P_DEBUT = "dateDebut";
    private static final String P_FIN = "dateFin";
    private static final String P_UTILISATEUR = "utilisateur";
    private static final String P_OUVERTE = "ouverte";
    private static final String P_IP = "ip";
    private static final String P_PAGE = "page";
    private static final String P_SIZE = "size";
    private static final String TOTAL = "$.totalElements";
    private static final String PREMIERE_LIGNE = "$.content[0].";
    private static final String JOUR = "2026-03-10";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions fixture;

    private AdmUser admin;
    private String jeton;
    private String ip;

    @BeforeEach
    void setUp() {
        fixture.assurerTables();
        admin = patrimoine.administrateur();
        jeton = jeton();
        ip = "poste-" + jeton + "/10.0.0.7";
    }

    private ResultActions lire(String url, String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(url).with(connecte(admin));
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private List<String> ids(String... parametres) throws Exception {
        return JsonPath.read(corps(lire(URL, parametres)), "$.content[*].id");
    }

    private static String corps(ResultActions reponse) throws Exception {
        return reponse.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Session à l'adresse du test : {@code ip=jeton} isole les lignes de ce test. */
    private FixtureSessions.Ligne session(LocalDateTime debut) {
        return fixture.session(admin, debut).avec(ADRESSE_IP, ip);
    }

    private UUID sessionAvecEtat(int etat) {
        return session(DATE).avec(ETAT, etat).inserer();
    }

    // ------------------------------------------------------------------ règle 1 : tri

    @Test
    void r1_triParStartDateDecroissante() throws Exception {
        UUID ancienne = session(DATE.minusDays(2)).inserer();
        UUID recente = session(DATE).inserer();
        UUID milieu = session(DATE.minusDays(1)).inserer();

        assertThat(ids(P_IP, jeton)).containsExactly(recente.toString(), milieu.toString(), ancienne.toString());
    }

    // Le sens du départage n'est pas écrit au spec : seul l'ordre par PKey, sur toutes les pages, est figé.
    @Test
    void r1_aDateEgale_ordreParPKey_stableEntreLesPages() throws Exception {
        for (int i = 0; i < 5; i++) {
            session(DATE).inserer();
        }
        List<String> parPKey = fixture.idsParPKey(ip);
        List<String> inverse = new ArrayList<>(parPKey);
        Collections.reverse(inverse);

        List<String> lus = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            lus.addAll(ids(P_IP, jeton, P_SIZE, "2", P_PAGE, String.valueOf(page)));
        }

        assertThat(lus).hasSize(5).isIn(parPKey, inverse);
    }

    // ------------------------------------------------------------------ règle 2 : période

    static Stream<Arguments> bornesDuJour() {
        LocalDateTime minuit = LocalDateTime.of(2026, 3, 10, 0, 0);
        return Stream.of(
                Arguments.of(minuit.minusSeconds(1), false),
                Arguments.of(minuit, true),
                Arguments.of(minuit.plusHours(23).plusMinutes(59), true),
                Arguments.of(minuit.plusDays(1).minusNanos(3_000_000), true),
                Arguments.of(minuit.plusDays(1), false));
    }

    @ParameterizedTest
    @MethodSource("bornesDuJour")
    void r2_periode_debutInclus_finInclusJusquAuDernierInstant(LocalDateTime debut, boolean attendu) throws Exception {
        UUID session = session(debut).inserer();

        List<String> lus = ids(P_IP, jeton, P_DEBUT, JOUR, P_FIN, JOUR);

        assertThat(lus).isEqualTo(attendu ? List.of(session.toString()) : List.of());
    }

    @Test
    void r2_sansDate_aucuneBorneParDefaut() throws Exception {
        UUID ancienne = session(LocalDateTime.of(2001, 1, 2, 3, 4)).inserer();

        assertThat(ids(P_IP, jeton)).containsExactly(ancienne.toString());
    }

    @Test
    void r2_dateDebutSeule_bornesParLeBasSeulement() throws Exception {
        UUID apres = session(DATE).inserer();
        session(DATE.minusDays(1)).inserer();

        assertThat(ids(P_IP, jeton, P_DEBUT, JOUR)).containsExactly(apres.toString());
    }

    @Test
    void r2_dateFinSeule_bornesParLeHautSeulement() throws Exception {
        UUID avant = session(DATE).inserer();
        session(DATE.plusDays(1)).inserer();

        assertThat(ids(P_IP, jeton, P_FIN, JOUR)).containsExactly(avant.toString());
    }

    // ------------------------------------------------------------------ règle 3 : ouverte

    @Test
    void r3_ouverteVrai_seulementState2() throws Exception {
        UUID ouverte = sessionAvecEtat(OUVERTE);
        sessionAvecEtat(FERMEE);
        sessionAvecEtat(VERROUILLEE);

        assertThat(ids(P_IP, jeton, P_OUVERTE, "true")).containsExactly(ouverte.toString());
    }

    @Test
    void r3_ouverteFaux_toutSaufState2_verrouilleeComprise() throws Exception {
        sessionAvecEtat(OUVERTE);
        UUID fermee = sessionAvecEtat(FERMEE);
        UUID verrouillee = sessionAvecEtat(VERROUILLEE);
        UUID autre = sessionAvecEtat(0);

        assertThat(ids(P_IP, jeton, P_OUVERTE, "false"))
                .containsExactlyInAnyOrder(fermee.toString(), verrouillee.toString(), autre.toString());
    }

    @Test
    void r3_ouverteAbsent_tout() throws Exception {
        UUID ouverte = sessionAvecEtat(OUVERTE);
        UUID fermee = sessionAvecEtat(FERMEE);
        UUID verrouillee = sessionAvecEtat(VERROUILLEE);

        assertThat(ids(P_IP, jeton))
                .containsExactlyInAnyOrder(ouverte.toString(), fermee.toString(), verrouillee.toString());
    }

    // ------------------------------------------------------------------ règle 4 : état sur le fil

    @ParameterizedTest
    @CsvSource({"2,OUVERTE", "8,VERROUILLEE", "4,FERMEE", "0,FERMEE", "1,FERMEE", "3,FERMEE", "16,FERMEE"})
    void r4_etat_selonState(int state, String attendu) throws Exception {
        sessionAvecEtat(state);

        lire(URL, P_IP, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "etat").value(attendu));
    }

    // ------------------------------------------------------------------ règle 5 : ip

    @Test
    void r5_ip_contient() throws Exception {
        UUID retenue = session(DATE).avec(ADRESSE_IP, "poste/" + jeton + "/10.0.0.7").inserer();
        session(DATE).avec(ADRESSE_IP, "poste/10.0.0.7").inserer();

        assertThat(ids(P_IP, jeton)).containsExactly(retenue.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a%b", "a_b", "a[b", "a[x]b"})
    void r5_caracteresJokersEchappes(String motif) throws Exception {
        UUID exacte = session(DATE).avec(ADRESSE_IP, jeton + motif).inserer();
        session(DATE).avec(ADRESSE_IP, jeton + "axb").inserer();

        assertThat(ids(P_IP, jeton + motif)).containsExactly(exacte.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void r5_ipVideOuBlanche_pasDeFiltre(String vide) throws Exception {
        String nom = "Karim " + jeton;
        UUID une = session(DATE).avec(UTILISATEUR, nom).avec(ADRESSE_IP, "poste/1").inserer();
        UUID autre = session(DATE).avec(UTILISATEUR, nom).avec(ADRESSE_IP, "poste/2").inserer();

        assertThat(ids(P_UTILISATEUR, nom, P_IP, vide)).containsExactlyInAnyOrder(une.toString(), autre.toString());
    }

    // ------------------------------------------------------------------ règle 6 : refus

    @Test
    void r6_dateDebutApresDateFin_400() throws Exception {
        lire(URL, P_DEBUT, "2026-03-11", P_FIN, JOUR).andExpect(status().isBadRequest());
    }

    @Test
    void r6_pageNegative_400() throws Exception {
        lire(URL, P_PAGE, "-1").andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "201"})
    void r6_sizeHorsBornes_400(String size) throws Exception {
        lire(URL, P_SIZE, size).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 200})
    void r6_sizeAuxBornes_200(int size) throws Exception {
        lire(URL, P_SIZE, String.valueOf(size))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(size));
    }

    // ------------------------------------------------------------------ section Back : filtre utilisateur

    @Test
    void back_utilisateur_egaliteSurUserDesc() throws Exception {
        String nom = "Karim " + jeton;
        UUID sienne = session(DATE).avec(UTILISATEUR, nom).inserer();
        session(DATE).avec(UTILISATEUR, nom + " bis").inserer();
        session(DATE).avec(UTILISATEUR, "Autre " + nom).inserer();

        assertThat(ids(P_UTILISATEUR, nom)).containsExactly(sienne.toString());
    }

    @Test
    void back_filtresCumules() throws Exception {
        String nom = "Karim " + jeton;
        UUID retenue = session(DATE).avec(UTILISATEUR, nom).inserer();
        session(DATE).avec(UTILISATEUR, nom).avec(ETAT, FERMEE).inserer();
        session(DATE.minusDays(1)).avec(UTILISATEUR, nom).inserer();
        session(DATE).avec(UTILISATEUR, "Autre " + jeton).inserer();
        session(DATE).avec(UTILISATEUR, nom).avec(ADRESSE_IP, "poste/autre").inserer();

        assertThat(ids(P_UTILISATEUR, nom, P_IP, jeton, P_OUVERTE, "true", P_DEBUT, JOUR, P_FIN, JOUR))
                .containsExactly(retenue.toString());
    }

    // ------------------------------------------------------------------ section Back : champs du fil

    @Test
    void back_ligne_exposeExactementLesChampsDuSpec() throws Exception {
        session(DATE).avec(FIN, DATE.plusHours(1)).avec(ETAT, FERMEE).inserer();

        Map<String, Object> ligne = JsonPath.read(corps(lire(URL, P_IP, jeton)), "$.content[0]");

        assertThat(ligne).containsOnlyKeys("id", P_UTILISATEUR, "dateOuverture", "dateFermeture", "etat", "adresseIp", "version");
    }

    @Test
    void back_webSession_jamaisExposee_surLesLectures() throws Exception {
        String secret = "secret-" + jeton;
        session(DATE).avec(WEB_SESSION, secret).avec(UTILISATEUR, "Karim " + jeton).inserer();

        String liste = corps(lire(URL, P_IP, jeton));
        String utilisateurs = corps(lire(URL_UTILISATEURS));

        assertAll(
                () -> assertThat(liste).contains(jeton).doesNotContain(secret),
                () -> assertThat(utilisateurs).contains("Karim " + jeton).doesNotContain(secret));
    }

    @Test
    void back_ligne_valeursDesColonnes() throws Exception {
        UUID session = session(DATE)
                .avec(UTILISATEUR, "Karim Résidence")
                .avec(FIN, DATE.plusHours(2))
                .avec(ETAT, FERMEE)
                .avec(VERSION, "Desktop: Version 4.5.57.0")
                .inserer();

        lire(URL, P_IP, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(session.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_UTILISATEUR).value("Karim Résidence"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "dateOuverture", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + "dateFermeture", startsWith("2026-03-10T11:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + "etat").value("FERMEE"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "adresseIp").value(ip))
                .andExpect(jsonPath(PREMIERE_LIGNE + "version").value("Desktop: Version 4.5.57.0"));
    }

    @Test
    void back_sessionJamaisFermee_dateFermetureVide() throws Exception {
        session(DATE).inserer();

        lire(URL, P_IP, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "dateFermeture").doesNotExist());
    }

    // ------------------------------------------------------------------ section Back : pagination

    @Test
    void back_pagination_parDefautPage0Taille50() throws Exception {
        lire(URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void back_pagination_totalDuFiltre_etPageDemandee() throws Exception {
        session(DATE).inserer();
        UUID milieu = session(DATE.minusDays(1)).inserer();
        session(DATE.minusDays(2)).inserer();

        lire(URL, P_IP, jeton, P_PAGE, "1", P_SIZE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(milieu.toString()));
    }

    // ------------------------------------------------------------------ section Back : utilisateurs

    @Test
    void back_utilisateurs_valeursDistinctesTriees() throws Exception {
        session(DATE).avec(UTILISATEUR, jeton + " Zineb").inserer();
        session(DATE).avec(UTILISATEUR, jeton + " Amine").inserer();
        session(DATE).avec(UTILISATEUR, jeton + " Malika").inserer();
        session(DATE).avec(UTILISATEUR, jeton + " Amine").inserer();

        List<String> valeurs = JsonPath.read(corps(lire(URL_UTILISATEURS)), "$");

        assertThat(valeurs.stream().filter(v -> v != null && v.startsWith(jeton)).toList())
                .containsExactly(jeton + " Amine", jeton + " Malika", jeton + " Zineb");
    }

    @Test
    void back_utilisateurs_sansNiNullNiVide() throws Exception {
        session(DATE).sans(UTILISATEUR).inserer();
        session(DATE).avec(UTILISATEUR, "").inserer();
        session(DATE).avec(UTILISATEUR, jeton).inserer();

        List<String> valeurs = JsonPath.read(corps(lire(URL_UTILISATEURS)), "$");

        assertThat(valeurs).contains(jeton).doesNotContainNull().doesNotContain("");
    }
}
