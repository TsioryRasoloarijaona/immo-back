package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFichesVerrouillees;
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
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.ENTITE;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.ETAT;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.LIBELLE;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.NON_VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.VERROUILLEE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureSessions.UTILISATEUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste des fiches verrouillées — FEATURE-admin-fiches-verrouillees.md, règles 1 à 6 et section Back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichesVerrouilleesListeTest {

    private static final String URL = "/api/administration/fiches-verrouillees";
    private static final String P_ETAT = "etat";
    private static final String P_DEBUT = "dateDebut";
    private static final String P_FIN = "dateFin";
    private static final String P_RECHERCHE = "recherche";
    private static final String P_PAGE = "page";
    private static final String P_SIZE = "size";
    private static final String E_VERROUILLEE = "VERROUILLEE";
    private static final String E_NON_VERROUILLEE = "NON_VERROUILLEE";
    private static final String UTILISATEUR_FIL = "utilisateur";
    private static final String TOTAL = "$.totalElements";
    private static final String PREMIERE_LIGNE = "$.content[0].";
    private static final String JOUR = "2026-03-10";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions sessions;
    @Autowired private FixtureFichesVerrouillees fixture;

    private AdmUser admin;
    private String jeton;
    private UUID session;

    @BeforeEach
    void setUp() {
        sessions.assurerTables();
        fixture.assurerTable();
        admin = patrimoine.administrateur();
        jeton = jeton();
        session = sessions.session(admin, DATE).inserer();
    }

    private ResultActions lire(String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(URL).with(connecte(admin));
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private static String corps(ResultActions reponse) throws Exception {
        return reponse.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> ids(String... parametres) throws Exception {
        return JsonPath.read(corps(lire(parametres)), "$.content[*].id");
    }

    private FixtureFichesVerrouillees.Ligne verrou(LocalDateTime date) {
        return fixture.verrou(session, jeton, date);
    }

    private UUID verrouAvecEtat(int etat) {
        return verrou(DATE).avec(ETAT, etat).inserer();
    }

    /** Session d'un autre nom : {@code UserDesc} est porté par la session, pas par le verrou. */
    private UUID sessionDe(String utilisateur) {
        return sessions.session(admin, DATE).avec(UTILISATEUR, utilisateur).inserer();
    }

    /** Ligne dont seule la colonne visée contient la valeur. */
    private UUID ligneOu(String colonne, String valeur) {
        FixtureFichesVerrouillees.Ligne ligne = fixture.verrou(session, "sans-marqueur", DATE);
        if (UTILISATEUR.equals(colonne)) {
            return ligne.avec(FixtureFichesVerrouillees.SESSION, sessionDe(valeur)).inserer();
        }
        return ligne.avec(colonne, valeur).inserer();
    }

    // ------------------------------------------------------------------ règle 1 : tri

    @Test
    void r1_triParLockDateDecroissante() throws Exception {
        UUID ancienne = verrou(DATE.minusDays(2)).inserer();
        UUID recente = verrou(DATE).inserer();
        UUID milieu = verrou(DATE.minusDays(1)).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(recente.toString(), milieu.toString(), ancienne.toString());
    }

    // Le sens du départage n'est pas écrit au spec : seul l'ordre par PKey, sur toutes les pages, est figé.
    @Test
    void r1_aDateEgale_ordreParPKey_stableEntreLesPages() throws Exception {
        for (int i = 0; i < 5; i++) {
            verrou(DATE).inserer();
        }
        List<String> parPKey = fixture.idsParPKey(jeton);
        List<String> inverse = new ArrayList<>(parPKey);
        Collections.reverse(inverse);

        List<String> lus = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            lus.addAll(ids(P_RECHERCHE, jeton, P_SIZE, "2", P_PAGE, String.valueOf(page)));
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
    void r2_periode_debutInclus_finInclusJusquAuDernierInstant(LocalDateTime date, boolean attendu) throws Exception {
        UUID verrou = verrou(date).inserer();

        List<String> lus = ids(P_RECHERCHE, jeton, P_DEBUT, JOUR, P_FIN, JOUR);

        assertThat(lus).isEqualTo(attendu ? List.of(verrou.toString()) : List.of());
    }

    @Test
    void r2_sansDate_aucuneBorneParDefaut() throws Exception {
        UUID ancienne = verrou(LocalDateTime.of(2001, 1, 2, 3, 4)).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(ancienne.toString());
    }

    @Test
    void r2_dateDebutSeule_bornesParLeBasSeulement() throws Exception {
        UUID apres = verrou(DATE).inserer();
        verrou(DATE.minusDays(1)).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_DEBUT, JOUR)).containsExactly(apres.toString());
    }

    @Test
    void r2_dateFinSeule_bornesParLeHautSeulement() throws Exception {
        UUID avant = verrou(DATE).inserer();
        verrou(DATE.plusDays(1)).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_FIN, JOUR)).containsExactly(avant.toString());
    }

    @Test
    void r2_dateDebutApresDateFin_400() throws Exception {
        lire(P_DEBUT, "2026-03-11", P_FIN, JOUR).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ règle 3 : état

    @Test
    void r3_etatVerrouillee_seulementState1() throws Exception {
        UUID verrouillee = verrouAvecEtat(VERROUILLEE);
        verrouAvecEtat(NON_VERROUILLEE);
        verrouAvecEtat(0);

        assertThat(ids(P_RECHERCHE, jeton, P_ETAT, E_VERROUILLEE)).containsExactly(verrouillee.toString());
    }

    @Test
    void r3_etatNonVerrouillee_toutSaufState1() throws Exception {
        verrouAvecEtat(VERROUILLEE);
        UUID liberee = verrouAvecEtat(NON_VERROUILLEE);
        UUID zero = verrouAvecEtat(0);
        UUID trois = verrouAvecEtat(3);

        assertThat(ids(P_RECHERCHE, jeton, P_ETAT, E_NON_VERROUILLEE))
                .containsExactlyInAnyOrder(liberee.toString(), zero.toString(), trois.toString());
    }

    @Test
    void r3_etatAbsent_toutes() throws Exception {
        UUID verrouillee = verrouAvecEtat(VERROUILLEE);
        UUID liberee = verrouAvecEtat(NON_VERROUILLEE);

        assertThat(ids(P_RECHERCHE, jeton)).containsExactlyInAnyOrder(verrouillee.toString(), liberee.toString());
    }

    @ParameterizedTest
    @CsvSource({"1,VERROUILLEE", "2,NON_VERROUILLEE", "0,NON_VERROUILLEE", "3,NON_VERROUILLEE", "8,NON_VERROUILLEE"})
    void r3_etatSurLeFil_selonState(int state, String attendu) throws Exception {
        verrouAvecEtat(state);

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_ETAT).value(attendu));
    }

    // ------------------------------------------------------------------ règle 4 : recherche

    @ParameterizedTest
    @ValueSource(strings = {ENTITE, LIBELLE, UTILISATEUR})
    void r4_recherche_contientSurLaColonne(String colonne) throws Exception {
        UUID retenue = ligneOu(colonne, "avant " + jeton + " apres");
        ligneOu(colonne, "avant apres");

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(retenue.toString());
    }

    static Stream<Arguments> jokersParColonne() {
        return Stream.of(ENTITE, LIBELLE, UTILISATEUR).flatMap(colonne -> Stream.of("a%b", "a_b", "a[b", "a[x]b")
                .map(motif -> Arguments.of(colonne, motif)));
    }

    @ParameterizedTest
    @MethodSource("jokersParColonne")
    void r4_caracteresJokersEchappes(String colonne, String motif) throws Exception {
        UUID exacte = ligneOu(colonne, jeton + motif);
        ligneOu(colonne, jeton + "axb");

        assertThat(ids(P_RECHERCHE, jeton + motif)).containsExactly(exacte.toString());
    }

    // Le jour rare isole le test sans la recherche : la recherche blanche ne doit rien retirer.
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void r4_rechercheVideOuBlanche_pasDeFiltre(String vide) throws Exception {
        LocalDateTime rare = LocalDateTime.of(2003, 5, 6, 7, 8);
        UUID une = verrou(rare).inserer();
        UUID autre = verrou(rare).avec(LIBELLE, "Autre fiche").inserer();

        assertThat(ids(P_DEBUT, "2003-05-06", P_FIN, "2003-05-06", P_RECHERCHE, vide))
                .contains(une.toString(), autre.toString());
    }

    // ------------------------------------------------------------------ règle 5 : session absente

    // Aucune session à cette PKey : ci/schema/160 n'a pas de FK, pour que ce cas reste testable.
    @Test
    void r5_sessionAbsente_ligneListee_utilisateurNull() throws Exception {
        UUID orpheline = verrou(DATE).avec(FixtureFichesVerrouillees.SESSION, UUID.randomUUID()).inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(orpheline.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + UTILISATEUR_FIL).doesNotExist());
    }

    @Test
    void r5_sessionAbsente_trouveeParSonLibelle_etSansRecherche() throws Exception {
        UUID orpheline = verrou(DATE).avec(FixtureFichesVerrouillees.SESSION, UUID.randomUUID()).inserer();
        UUID avecSession = verrou(DATE).inserer();

        assertThat(ids(P_RECHERCHE, "Fiche " + jeton))
                .containsExactlyInAnyOrder(orpheline.toString(), avecSession.toString());
        assertThat(ids(P_DEBUT, JOUR, P_FIN, JOUR)).contains(orpheline.toString(), avecSession.toString());
    }

    // ------------------------------------------------------------------ règle 6 : refus de pagination

    @Test
    void r6_pageNegative_400() throws Exception {
        lire(P_PAGE, "-1").andExpect(status().isBadRequest());
    }

    @Test
    void r6_pageAuDelaDuPlafond_400() throws Exception {
        lire(P_PAGE, "1000001").andExpect(status().isBadRequest());
    }

    @Test
    void r6_pageAuPlafond_200() throws Exception {
        lire(P_PAGE, "1000000").andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1_000_000));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "201"})
    void r6_sizeHorsBornes_400(String size) throws Exception {
        lire(P_SIZE, size).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 200})
    void r6_sizeAuxBornes_200(int size) throws Exception {
        lire(P_SIZE, String.valueOf(size))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(size));
    }

    // ------------------------------------------------------------------ section Back : champs et pagination

    @Test
    void back_ligne_exposeExactementLesChampsDuSpec() throws Exception {
        verrou(DATE).inserer();

        Map<String, Object> ligne = JsonPath.read(corps(lire(P_RECHERCHE, jeton)), "$.content[0]");

        assertThat(ligne).containsOnlyKeys("id", "entite", "libelle", "dateVerrouillage", UTILISATEUR_FIL, P_ETAT);
    }

    @Test
    void back_ligne_valeursDesColonnes() throws Exception {
        UUID verrou = fixture.verrou(sessionDe("Karim Résidence"), jeton, DATE).avec(ENTITE, "COMFOLDER").inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(verrou.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + "entite").value("COMFOLDER"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "libelle").value("Fiche " + jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + "dateVerrouillage", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + UTILISATEUR_FIL).value("Karim Résidence"))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_ETAT).value(E_VERROUILLEE));
    }

    @Test
    void back_filtresCumules() throws Exception {
        UUID retenue = verrou(DATE).inserer();
        verrou(DATE).avec(ETAT, NON_VERROUILLEE).inserer();
        verrou(DATE.minusDays(1)).inserer();
        verrou(DATE).avec(LIBELLE, "Autre fiche").inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_ETAT, E_VERROUILLEE, P_DEBUT, JOUR, P_FIN, JOUR))
                .containsExactly(retenue.toString());
    }

    @Test
    void back_pagination_parDefautPage0Taille50() throws Exception {
        lire()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void back_pagination_totalDuFiltre_etPageDemandee() throws Exception {
        verrou(DATE).inserer();
        UUID milieu = verrou(DATE.minusDays(1)).inserer();
        verrou(DATE.minusDays(2)).inserer();

        lire(P_RECHERCHE, jeton, P_PAGE, "1", P_SIZE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(milieu.toString()));
    }
}
