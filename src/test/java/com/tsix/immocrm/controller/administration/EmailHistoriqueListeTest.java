package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEmailHistorique;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.AUTOMATIQUE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.CORPS;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.COPIE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.COPIE_CACHEE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.DESTINATAIRES;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.ENVOYE;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.ERREUR;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.EXPEDITEUR;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.LIBELLE_OBJET;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.NOM_EXPEDITEUR;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.OBJET;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.UTILISATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Liste du journal des e-mails — FEATURE-admin-email-historique.md, règles 1 à 5 et décisions du manager. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailHistoriqueListeTest {

    private static final String URL = "/api/administration/emails";
    private static final String URL_UTILISATEURS = URL + "/utilisateurs";
    private static final String P_TEXTE = "texte";
    private static final String P_DEBUT = "dateDebut";
    private static final String P_FIN = "dateFin";
    private static final String P_ENVOYE = "envoye";
    private static final String P_UTILISATEUR = "utilisateur";
    private static final String P_PAGE = "page";
    private static final String P_SIZE = "size";
    private static final String TOTAL = "$.totalElements";
    private static final String PREMIERE_LIGNE = "$.content[0].";
    private static final String JOUR = "2026-03-10";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureEmailHistorique fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurerTable();
        admin = patrimoine.administrateur();
        jeton = jeton();
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

    private UUID avecObjet(LocalDateTime date) {
        return fixture.email(date).avec(OBJET, jeton).inserer();
    }

    // ------------------------------------------------------------------ règle 1 : tri

    @Test
    void r1_triParDateDecroissante() throws Exception {
        UUID ancien = avecObjet(DATE.minusDays(2));
        UUID recent = avecObjet(DATE);
        UUID milieu = avecObjet(DATE.minusDays(1));

        assertThat(ids(P_TEXTE, jeton)).containsExactly(recent.toString(), milieu.toString(), ancien.toString());
    }

    // Le sens du départage n'est pas écrit au spec : seul l'ordre par PKey, sur toutes les pages, est figé.
    @Test
    void r1_aDateEgale_ordreParPKey_stableEntreLesPages() throws Exception {
        for (int i = 0; i < 5; i++) {
            avecObjet(DATE);
        }
        List<String> parPKey = fixture.idsParPKey(jeton);
        List<String> inverse = new ArrayList<>(parPKey);
        Collections.reverse(inverse);

        List<String> lus = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            lus.addAll(ids(P_TEXTE, jeton, P_SIZE, "2", P_PAGE, String.valueOf(page)));
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

    // Décision du manager : MailDate >= début et < fin + 1 jour.
    @ParameterizedTest
    @MethodSource("bornesDuJour")
    void r2_periode_debutInclus_finInclusJusquAuDernierInstant(LocalDateTime date, boolean attendu) throws Exception {
        UUID email = avecObjet(date);

        List<String> lus = ids(P_TEXTE, jeton, P_DEBUT, JOUR, P_FIN, JOUR);

        assertThat(lus).isEqualTo(attendu ? List.of(email.toString()) : List.of());
    }

    @Test
    void r2_sansDate_aucuneBorneParDefaut() throws Exception {
        UUID ancien = avecObjet(LocalDateTime.of(2001, 1, 2, 3, 4));

        assertThat(ids(P_TEXTE, jeton)).containsExactly(ancien.toString());
    }

    @Test
    void r2_dateDebutSeule_bornesParLeBasSeulement() throws Exception {
        UUID apres = avecObjet(DATE);
        avecObjet(DATE.minusDays(1));

        assertThat(ids(P_TEXTE, jeton, P_DEBUT, JOUR)).containsExactly(apres.toString());
    }

    @Test
    void r2_dateFinSeule_bornesParLeHautSeulement() throws Exception {
        UUID avant = avecObjet(DATE);
        avecObjet(DATE.plusDays(1));

        assertThat(ids(P_TEXTE, jeton, P_FIN, JOUR)).containsExactly(avant.toString());
    }

    // ------------------------------------------------------------------ règle 3 : recherche

    @ParameterizedTest
    @ValueSource(strings = {UTILISATEUR, OBJET, NOM_EXPEDITEUR, DESTINATAIRES, EXPEDITEUR, LIBELLE_OBJET})
    void r3_texte_chercheChaqueColonne_sansTenirCompteDeLaCasse(String colonne) throws Exception {
        UUID email = fixture.email(DATE).avec(colonne, "avant " + jeton + " apres").inserer();

        assertThat(ids(P_TEXTE, jeton.toLowerCase(Locale.ROOT))).containsExactly(email.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {COPIE, COPIE_CACHEE, CORPS})
    void r3_texte_neChercheQueLesColonnesDeLaDecision(String colonne) throws Exception {
        fixture.email(DATE).avec(colonne, jeton).inserer();
        UUID temoin = avecObjet(DATE);

        assertThat(ids(P_TEXTE, jeton)).containsExactly(temoin.toString());
    }

    // ------------------------------------------------------------------ règle 4 : envoyé ou en échec

    @Test
    void r4_envoyeFaux_neRendQueLesEchecs_avecErreur() throws Exception {
        avecObjet(DATE);
        UUID echec = fixture.email(DATE).avec(OBJET, jeton).avec(ENVOYE, false).avec(ERREUR, "Boite pleine").inserer();

        lire(URL, P_TEXTE, jeton, P_ENVOYE, "false")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(echec.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_ENVOYE).value(false))
                .andExpect(jsonPath(PREMIERE_LIGNE + "erreur").value("Boite pleine"));
    }

    @Test
    void r4_envoyeVrai_neRendQueLesEnvoyes() throws Exception {
        UUID envoye = avecObjet(DATE);
        fixture.email(DATE).avec(OBJET, jeton).avec(ENVOYE, false).inserer();

        assertThat(ids(P_TEXTE, jeton, P_ENVOYE, "true")).containsExactly(envoye.toString());
    }

    @Test
    void r4_sansFiltreEnvoye_rendLesDeux() throws Exception {
        UUID envoye = avecObjet(DATE);
        UUID echec = fixture.email(DATE).avec(OBJET, jeton).avec(ENVOYE, false).inserer();

        assertThat(ids(P_TEXTE, jeton)).containsExactlyInAnyOrder(envoye.toString(), echec.toString());
    }

    // ------------------------------------------------------------------ décision : filtre utilisateur

    @Test
    void decision_utilisateur_egaliteStricteSurAdmUserDescription() throws Exception {
        String nom = "Karim " + jeton;
        UUID sien = fixture.email(DATE).avec(UTILISATEUR, nom).inserer();
        fixture.email(DATE).avec(UTILISATEUR, nom + " bis").inserer();

        assertThat(ids(P_UTILISATEUR, nom)).containsExactly(sien.toString());
    }

    @Test
    void decision_filtresCumules() throws Exception {
        String nom = "Karim Filtres";
        UUID retenu = fixture.email(DATE).avec(UTILISATEUR, nom).avec(OBJET, jeton).avec(ENVOYE, false).inserer();
        fixture.email(DATE).avec(UTILISATEUR, nom).avec(OBJET, jeton).inserer();
        fixture.email(DATE.minusDays(1)).avec(UTILISATEUR, nom).avec(OBJET, jeton).avec(ENVOYE, false).inserer();
        fixture.email(DATE).avec(UTILISATEUR, nom).avec(ENVOYE, false).avec(OBJET, "autre").inserer();

        assertThat(ids(P_UTILISATEUR, nom, P_TEXTE, jeton, P_ENVOYE, "false", P_DEBUT, JOUR, P_FIN, JOUR))
                .containsExactly(retenu.toString());
    }

    @Test
    void decision_utilisateurs_valeursDistinctesTriees() throws Exception {
        fixture.email(DATE).avec(UTILISATEUR, jeton + " Zineb").inserer();
        fixture.email(DATE).avec(UTILISATEUR, jeton + " Amine").inserer();
        fixture.email(DATE).avec(UTILISATEUR, jeton + " Malika").inserer();
        fixture.email(DATE).avec(UTILISATEUR, jeton + " Amine").inserer();

        List<String> valeurs = JsonPath.read(corps(lire(URL_UTILISATEURS)), "$");

        assertThat(valeurs.stream().filter(v -> v.startsWith(jeton)).toList())
                .containsExactly(jeton + " Amine", jeton + " Malika", jeton + " Zineb");
    }

    // ------------------------------------------------------------------ règle 5 : refus

    @Test
    void r5_dateDebutApresDateFin_400() throws Exception {
        lire(URL, P_DEBUT, "2026-03-11", P_FIN, JOUR).andExpect(status().isBadRequest());
    }

    @Test
    void r5_pageNegative_400() throws Exception {
        lire(URL, P_PAGE, "-1").andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "201"})
    void r5_sizeHorsBornes_400(String size) throws Exception {
        lire(URL, P_SIZE, size).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 200})
    void r5_sizeAuxBornes_200(int size) throws Exception {
        lire(URL, P_SIZE, String.valueOf(size))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(size));
    }

    // ------------------------------------------------------------------ fil

    @Test
    void pagination_parDefautPage0Taille50() throws Exception {
        lire(URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void pagination_totalDuFiltre_etPageDemandee() throws Exception {
        avecObjet(DATE);
        UUID milieu = avecObjet(DATE.minusDays(1));
        avecObjet(DATE.minusDays(2));

        lire(URL, P_TEXTE, jeton, P_PAGE, "1", P_SIZE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(milieu.toString()));
    }

    @Test
    void ligne_expose_lesChampsDuSpec() throws Exception {
        UUID email = fixture.email(DATE)
                .avec(UTILISATEUR, "Karim Résidence")
                .avec(EXPEDITEUR, "ventes@ayad.test")
                .avec(DESTINATAIRES, "client@ayad.test")
                .avec(OBJET, jeton)
                .avec(LIBELLE_OBJET, "Dossier 2026/001")
                .avec(AUTOMATIQUE, true)
                .avec(ENVOYE, false)
                .avec(ERREUR, "Serveur injoignable")
                .inserer();

        lire(URL, P_TEXTE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(email.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + "date", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_UTILISATEUR).value("Karim Résidence"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "expediteur").value("ventes@ayad.test"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "destinataires").value("client@ayad.test"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "objet").value(jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + "automatique").value(true))
                .andExpect(jsonPath(PREMIERE_LIGNE + P_ENVOYE).value(false))
                .andExpect(jsonPath(PREMIERE_LIGNE + "erreur").value("Serveur injoignable"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "libelleObjet").value("Dossier 2026/001"));
    }

    @Test
    void ligne_neContientPasLesChampsDeLaFiche() throws Exception {
        fixture.email(DATE).avec(OBJET, jeton).avec(COPIE, "c@ayad.test").avec(CORPS, "Bonjour").inserer();

        lire(URL, P_TEXTE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "corps").doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + "copie").doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + "copieCachee").doesNotExist());
    }

    @Test
    void ligne_colonnesDeTexteVides_resteDansLaListe() throws Exception {
        String nom = "Karim " + jeton;
        UUID email = fixture.email(DATE).avec(UTILISATEUR, nom).inserer();

        assertThat(ids(P_UTILISATEUR, nom)).containsExactly(email.toString());
    }
}
