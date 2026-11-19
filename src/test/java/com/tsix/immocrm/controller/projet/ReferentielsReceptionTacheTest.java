package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotAvenants;
import com.tsix.immocrm.fixtures.FixtureLotSousFenetres;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReferentielsPlanification;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.projet.AvenantsLotSupport.REMBOURSEMENTS;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.DROIT_NATURES_RECEPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.DROIT_TYPES_RECEPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.DROIT_TYPES_TACHE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.NATURES_RECEPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.TYPES_RECEPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsPlanification.TYPES_TACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * N9a, référentiels Types réception, Natures réception, Type de tâche sur le moteur {@code /api/referentiels-admin}
 * (FEATURE-n9-planification.md R1, R2, R3, D4, D5, D7 et « Réponses du manager » 2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielsReceptionTacheTest {

    private static final String MOTEUR = "/api/referentiels-admin";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String SORTING = "sorting";
    private static final String DETAIL = "$.detail";
    private static final String DATE_DEF = "$.dateReceptionDefinitive";
    private static final String CODE_EXISTANT = "Ce code existe déjà.";
    private static final String SUPPRESSION_IMPOSSIBLE = "Suppression impossible : ";
    private static final String NATURE_PAR_ID = "SELECT CAST(IsFinal AS int) FROM PrjMarketReceptionKind WHERE PKey = ?";
    private static final LocalDate JUIN = LocalDate.of(2026, 6, 30);
    private static final LocalDate AOUT = LocalDate.of(2026, 8, 15);
    private static final LocalDate SEPTEMBRE = LocalDate.of(2026, 9, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielsPlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotSousFenetres sf;
    @Autowired private FixtureLotAvenants av;

    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        fixture.tables();
        sf.tables();
        av.tables();
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("N9P-" + jeton, parite.entiteJuridique("N9E-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "N9S-" + jeton);
        admin = connecte(parite.administrateur("n9a.adm." + jeton, projet), "Administrateur");
    }

    // ------------------------------------------------------------------ outils

    private Reponse appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(corps.json()), qui);
    }

    private static String table(String slug) {
        return MOTEUR + "/" + slug;
    }

    private static String ligne(String slug, UUID id) {
        return table(slug) + "/" + id;
    }

    private static String tableSql(String slug) {
        return switch (slug) {
            case TYPES_RECEPTION -> "PrjMarketReceptionType";
            case NATURES_RECEPTION -> "PrjMarketReceptionKind";
            default -> "CfgGanttTaskType";
        };
    }

    private CorpsJson saisie(String code) {
        return corps().avec(SORTING, 1).avec(CODE, code).avec(DESCRIPTION, "Libellé " + code);
    }

    /** Ligne existante de ce référentiel, écrite en SQL natif. */
    private UUID existante(String slug, String code) {
        return switch (slug) {
            case TYPES_RECEPTION -> sf.typeReception(code);
            case NATURES_RECEPTION -> sf.genreReception(code, false);
            default -> fixture.typeTache(code);
        };
    }

    private int lignesEnBase(String slug, UUID id) {
        return fixture.compter("SELECT COUNT(*) FROM " + tableSql(slug) + " WHERE PKey = ?", id);
    }

    private UUID lot() {
        rang++;
        UUID lot = parite.lot(projet, "N9L" + rang + "-" + jeton, ENTREPRENEURS, statut);
        av.finDuLot(lot, null);
        return lot;
    }

    private void reception(UUID lot, LocalDate date, UUID type, UUID nature) {
        rang++;
        sf.reception(lot, rang, "N9R" + rang + "-" + jeton, date, false, "Chantier", type, nature);
    }

    /** Nom du fil de la case « Est une réception définitive » : le seul champ BOOLEEN du descripteur (spec « API »). */
    private String champDefinitive() throws Exception {
        Reponse r = appeler(get(MOTEUR), admin);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<String> noms = r.ids("$[?(@.slug == '" + NATURES_RECEPTION + "')].champs[?(@.type == 'BOOLEEN')].nom");
        assertThat(noms).as("champ booléen IsFinal du descripteur natures-reception").hasSize(1);
        return noms.get(0);
    }

    private RequestPostProcessor compte(String suffixe, String... droits) {
        return connecte(parite.avecDroits("n9a." + suffixe + "." + jeton, List.of(droits)), "Commercial");
    }

    static Stream<String> slugs() {
        return Stream.of(TYPES_RECEPTION, NATURES_RECEPTION, TYPES_TACHE);
    }

    // ------------------------------------------------------------------ D4 : descripteurs du moteur, famille PROJET

    @Test
    void d4_troisDescripteurs_familleProjet_natureAvecUnChampBooleen() throws Exception {
        Reponse r = appeler(get(MOTEUR), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            for (String slug : List.of(TYPES_RECEPTION, NATURES_RECEPTION, TYPES_TACHE)) {
                List<String> familles = r.ids("$[?(@.slug == '" + slug + "')].famille");
                s.assertThat(familles).as(slug).containsExactly("PROJET");
            }
            s.assertThat(r.ids("$[?(@.slug == '" + NATURES_RECEPTION + "')].champs[?(@.type == 'BOOLEEN')].nom"))
                    .as("IsFinal").hasSize(1);
        });
    }

    // ------------------------------------------------------------------ R1 : création, obligatoires, unicité

    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r1_creation_relue_et_ecrite(String slug) throws Exception {
        String code = "C" + jeton;

        Reponse r = envoyer(post(table(slug)), saisie(code), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(201);
            s.assertThat(r.valeur("$." + CODE)).isEqualTo(code);
            s.assertThat(r.valeur("$." + DESCRIPTION)).isEqualTo("Libellé " + code);
            s.assertThat(lignesEnBase(slug, UUID.fromString(String.valueOf(r.valeur("$.id"))))).isEqualTo(1);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r1_codeEnDouble_409(String slug) throws Exception {
        String code = "D" + jeton;
        existante(slug, code);

        Reponse r = envoyer(post(table(slug)), saisie(code), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(String.valueOf(r.valeur(DETAIL))).contains(CODE_EXISTANT);
    }

    static Stream<Arguments> obligatoires() {
        return slugs().flatMap(slug -> Stream.of(Arguments.of(slug, CODE), Arguments.of(slug, DESCRIPTION)));
    }

    @ParameterizedTest(name = "{0} sans {1}")
    @MethodSource("obligatoires")
    void r1_champObligatoireManquant_400(String slug, String champ) throws Exception {
        Reponse r = envoyer(post(table(slug)), saisie("O" + jeton).sans(champ), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R2 / D7 : suppression refusée si citée

    @Test
    void r2_typeReception_citeParUneReception_409() throws Exception {
        UUID type = sf.typeReception("T" + jeton);
        reception(lot(), JUIN, type, sf.genreReception("G" + jeton, true));

        Reponse r = appeler(delete(ligne(TYPES_RECEPTION, type)), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(String.valueOf(r.valeur(DETAIL))).startsWith(SUPPRESSION_IMPOSSIBLE).contains("1");
        assertThat(lignesEnBase(TYPES_RECEPTION, type)).isEqualTo(1);
    }

    // R2 + R3 : la nature définitive utilisée ne se supprime pas.
    @Test
    void r2_r3_natureReception_citeeParUneReception_409() throws Exception {
        UUID nature = sf.genreReception("G" + jeton, true);
        reception(lot(), JUIN, sf.typeReception("T" + jeton), nature);

        Reponse r = appeler(delete(ligne(NATURES_RECEPTION, nature)), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(String.valueOf(r.valeur(DETAIL))).startsWith(SUPPRESSION_IMPOSSIBLE).contains("1");
        assertThat(lignesEnBase(NATURES_RECEPTION, nature)).isEqualTo(1);
    }

    @Test
    void r2_typeTache_citeParUneTache_409() throws Exception {
        UUID type = fixture.typeTache("K" + jeton);
        fixture.tacheDeType(type, "1");

        Reponse r = appeler(delete(ligne(TYPES_TACHE, type)), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(String.valueOf(r.valeur(DETAIL))).startsWith(SUPPRESSION_IMPOSSIBLE).contains("1");
        assertThat(lignesEnBase(TYPES_TACHE, type)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r2_ligneNonCitee_supprimee_204(String slug) throws Exception {
        UUID id = existante(slug, "S" + jeton);

        Reponse r = appeler(delete(ligne(slug, id)), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(lignesEnBase(slug, id)).isZero();
    }

    // ------------------------------------------------------------------ R3 : IsFinal éditable, plusieurs natures définitives

    // Réponse du manager 2 : deux natures définitives acceptées ; la date = dernière réception de l'une ou l'autre.
    @Test
    void r3_deuxNaturesDefinitives_creees_dateDefinitiveEstLaDerniereDesDeux() throws Exception {
        String definitive = champDefinitive();
        UUID type = sf.typeReception("T" + jeton);
        Reponse a = envoyer(post(table(NATURES_RECEPTION)), saisie("A" + jeton).avec(definitive, true), admin);
        Reponse b = envoyer(post(table(NATURES_RECEPTION)), saisie("B" + jeton).avec(definitive, true), admin);
        assertThat(a.statut()).as(a.corps()).isEqualTo(201);
        assertThat(b.statut()).as(b.corps()).isEqualTo(201);
        UUID natureA = UUID.fromString(String.valueOf(a.valeur("$.id")));
        UUID natureB = UUID.fromString(String.valueOf(b.valeur("$.id")));
        UUID lot = lot();
        reception(lot, JUIN, type, natureA);
        reception(lot, AOUT, type, natureB);
        reception(lot, SEPTEMBRE, type, sf.genreReception("P" + jeton, false));

        Reponse r = appeler(get(AppelsProjet.LOTS + "/" + lot + "/" + REMBOURSEMENTS), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fixture.compter(NATURE_PAR_ID, natureA)).isEqualTo(1);
            s.assertThat(fixture.compter(NATURE_PAR_ID, natureB)).isEqualTo(1);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.valeur(DATE_DEF)).as(r.corps()).isEqualTo("2026-08-15");
        });
    }

    @Test
    void r3_retirerIsFinal_faitDisparaitreLaDateDefinitive() throws Exception {
        String definitive = champDefinitive();
        UUID nature = sf.genreReception("DEF" + jeton, true);
        UUID lot = lot();
        reception(lot, JUIN, sf.typeReception("T" + jeton), nature);
        Reponse avant = appeler(get(AppelsProjet.LOTS + "/" + lot + "/" + REMBOURSEMENTS), admin);
        assertThat(avant.valeur(DATE_DEF)).as(avant.corps()).isEqualTo("2026-06-30");

        Reponse modifie = envoyer(patch(ligne(NATURES_RECEPTION, nature)), corps().avec(definitive, false), admin);
        Reponse apres = appeler(get(AppelsProjet.LOTS + "/" + lot + "/" + REMBOURSEMENTS), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modifie.statut()).as(modifie.corps()).isEqualTo(200);
            s.assertThat(modifie.valeur("$." + definitive)).isEqualTo(false);
            s.assertThat(fixture.compter(NATURE_PAR_ID, nature)).isZero();
            s.assertThat(apres.valeur(DATE_DEF)).as(apres.corps()).isNull();
        });
    }

    // ------------------------------------------------------------------ D5 : droits OMRAN par écran

    static Stream<Arguments> droits() {
        return Stream.of(Arguments.of(TYPES_RECEPTION, DROIT_TYPES_RECEPTION), Arguments.of(NATURES_RECEPTION, DROIT_NATURES_RECEPTION),
                Arguments.of(TYPES_TACHE, DROIT_TYPES_TACHE));
    }

    @ParameterizedTest(name = "{0} avec {1}")
    @MethodSource("droits")
    void d5_avecLeDroit_nonAdministrateur_litCreeSupprime(String slug, String droit) throws Exception {
        RequestPostProcessor avecDroit = compte("a", droit);
        UUID id = existante(slug, "X" + jeton);

        Reponse lecture = appeler(get(table(slug)), avecDroit);
        Reponse creation = envoyer(post(table(slug)), saisie("Y" + jeton), avecDroit);
        Reponse suppression = appeler(delete(ligne(slug, id)), avecDroit);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lecture.statut()).as(lecture.corps()).isEqualTo(200);
            s.assertThat(creation.statut()).as(creation.corps()).isEqualTo(201);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(204);
        });
    }

    @ParameterizedTest(name = "{0} sans {1}")
    @MethodSource("droits")
    void d5_sansLeDroit_403_lectureEcritureSuppression(String slug, String droit) throws Exception {
        RequestPostProcessor sansDroit = compte("s");
        UUID id = existante(slug, "Z" + jeton);
        Reponse temoin = appeler(get(table(slug)), admin);

        Reponse lecture = appeler(get(table(slug)), sansDroit);
        Reponse creation = envoyer(post(table(slug)), saisie("W" + jeton), sansDroit);
        Reponse modification = envoyer(patch(ligne(slug, id)), corps().avec(DESCRIPTION, "Renommé"), sansDroit);
        Reponse suppression = appeler(delete(ligne(slug, id)), sansDroit);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin " + droit).isEqualTo(200);
            s.assertThat(lecture.statut()).isEqualTo(403);
            s.assertThat(creation.statut()).isEqualTo(403);
            s.assertThat(modification.statut()).isEqualTo(403);
            s.assertThat(suppression.statut()).isEqualTo(403);
            s.assertThat(lignesEnBase(slug, id)).isEqualTo(1);
        });
    }

    @ParameterizedTest(name = "droit {0}")
    @ValueSource(strings = {DROIT_TYPES_RECEPTION, DROIT_NATURES_RECEPTION, DROIT_TYPES_TACHE})
    void d5_leDroitDUnEcran_nOuvrePasLesDeuxAutres(String droit) throws Exception {
        RequestPostProcessor unSeul = compte("u", droit);

        SoftAssertions s = new SoftAssertions();
        for (Arguments a : droits().toList()) {
            String slug = (String) a.get()[0];
            int attendu = droit.equals(a.get()[1]) ? 200 : 403;
            s.assertThat(appeler(get(table(slug)), unSeul).statut()).as(slug).isEqualTo(attendu);
        }
        s.assertAll();
    }
}
