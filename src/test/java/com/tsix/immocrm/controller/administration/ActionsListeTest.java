package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureActions;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureActions.ACTION;
import static com.tsix.immocrm.fixtures.FixtureActions.CODE;
import static com.tsix.immocrm.fixtures.FixtureActions.CREATEUR;
import static com.tsix.immocrm.fixtures.FixtureActions.CREE_LE;
import static com.tsix.immocrm.fixtures.FixtureActions.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureActions.MODIFICATEUR;
import static com.tsix.immocrm.fixtures.FixtureActions.MODIFIE_LE;
import static com.tsix.immocrm.fixtures.FixtureActions.PKEY;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Journal des actions — FEATURE-admin-actions.md, règles 1 à 7 et section Back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsListeTest {

    private static final String URL = "/api/administration/actions";
    private static final String P_RECHERCHE = "recherche";
    private static final String P_CREATION_DEBUT = "creationDebut";
    private static final String P_CREATION_FIN = "creationFin";
    private static final String P_MODIFICATION_DEBUT = "modificationDebut";
    private static final String P_MODIFICATION_FIN = "modificationFin";
    private static final String P_PAGE = "page";
    private static final String P_SIZE = "size";
    private static final String TOTAL = "$.totalElements";
    private static final String PREMIERE_LIGNE = "$.content[0].";
    private static final String CHAMP_ACTION = "action";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";
    private static final String CHAMP_CREE_PAR = "creePar";
    private static final String CHAMP_CREE_LE = "creeLe";
    private static final String CHAMP_MODIFIE_PAR = "modifiePar";
    private static final String CHAMP_MODIFIE_LE = "modifieLe";
    private static final String JOUR = "2026-03-10";
    private static final String LENDEMAIN = "2026-03-11";
    private static final String TELEPHONE_AVEC_ESPACE = "Téléphone Sortant ";
    private static final String TELEPHONE_SANS_ESPACE = "TéléphoneSortant";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);
    private static final LocalDateTime MINUIT = LocalDateTime.of(2026, 3, 10, 0, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureActions fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurerVue();
        admin = patrimoine.administrateur();
        jeton = jeton();
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

    /** Identifiants du fil en minuscules : le spec ne fixe pas la casse du {@code PKey}. */
    private List<String> ids(String... parametres) throws Exception {
        List<String> lus = JsonPath.read(corps(lire(parametres)), "$.content[*].id");
        return lus.stream().map(id -> id.toLowerCase(Locale.ROOT)).toList();
    }

    private FixtureActions.Ligne ligne(LocalDateTime creeLe) {
        return fixture.ligne(jeton, creeLe);
    }

    private FixtureActions.Ligne ligneSansMarque(LocalDateTime creeLe) {
        return ligne(creeLe).avec(CODE, "AUTRE");
    }

    private static String pkey(String id) {
        return id.substring(0, id.indexOf(':'));
    }

    private static String libelle(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    // ------------------------------------------------------------------ règle 1 : tri

    @Test
    void r1_triParDateDeCreationDecroissante_nonParModification() throws Exception {
        String ancienne = ligne(DATE.minusDays(2)).avec(MODIFIE_LE, DATE.plusDays(30)).inserer();
        String recente = ligne(DATE).avec(MODIFIE_LE, DATE.minusDays(30)).inserer();
        String milieu = ligne(DATE.minusDays(1)).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(recente, milieu, ancienne);
    }

    @Test
    void r1_dateDeCreationNulle_enDernier() throws Exception {
        String sansDate = ligne(DATE).sans(CREE_LE).inserer();
        String ancienne = ligne(DATE.minusYears(10)).inserer();
        String recente = ligne(DATE).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(recente, ancienne, sansDate);
    }

    // Le sens du départage n'est pas écrit au spec : ordre par PKey puis ActionLabel, l'un et l'autre monotones.
    @Test
    void r1_aDateEgale_ordreParPKeyPuisActionLabel_stableEntreLesPages() throws Exception {
        UUID commune = UUID.randomUUID();
        for (String libelle : List.of("BETA", "ALPHA", "GAMMA")) {
            ligne(DATE).avec(PKEY, commune).avec(ACTION, libelle).inserer();
        }
        ligne(DATE).inserer();
        ligne(DATE).inserer();
        List<String> parPKey = fixture.pkeysTriees(jeton);
        List<String> inverse = parPKey.reversed();

        List<String> pages = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            pages.addAll(ids(P_RECHERCHE, jeton, P_SIZE, "2", P_PAGE, String.valueOf(page)));
        }
        List<String> uneSeulePage = ids(P_RECHERCHE, jeton);
        List<String> libellesCommuns = pages.stream()
                .filter(id -> pkey(id).equals(commune.toString()))
                .map(ActionsListeTest::libelle)
                .toList();

        assertAll(
                () -> assertThat(pages).hasSize(5).doesNotHaveDuplicates().isEqualTo(uneSeulePage),
                () -> assertThat(pages.stream().map(ActionsListeTest::pkey).toList()).isIn(parPKey, inverse),
                () -> assertThat(libellesCommuns).isIn(List.of("alpha", "beta", "gamma"), List.of("gamma", "beta", "alpha")));
    }

    // ------------------------------------------------------------------ règle 2 : doublons de PKey

    @Test
    void r2_memePKey_labelsDifferents_deuxLignes_deuxIds() throws Exception {
        UUID commune = UUID.randomUUID();
        String source = ligne(DATE).avec(PKEY, commune).avec(ACTION, "SOURCE DE CONTACT").inserer();
        String support = ligne(DATE).avec(PKEY, commune).avec(ACTION, "SUPPORT SOURCE DE CONTACT").inserer();

        String reponse = corps(lire(P_RECHERCHE, jeton));
        List<String> actions = JsonPath.read(reponse, "$.content[*]." + CHAMP_ACTION);

        assertAll(
                () -> assertThat(ids(P_RECHERCHE, jeton)).containsExactlyInAnyOrder(source, support),
                () -> assertThat(actions).containsExactlyInAnyOrder("SOURCE DE CONTACT", "SUPPORT SOURCE DE CONTACT"));
    }

    @Test
    void r2_totalCompteLesDeuxLignes_chacuneSurSaPage() throws Exception {
        UUID commune = UUID.randomUUID();
        String une = ligne(DATE).avec(PKEY, commune).avec(ACTION, "SOURCE DE CONTACT").inserer();
        String autre = ligne(DATE).avec(PKEY, commune).avec(ACTION, "SUPPORT SOURCE DE CONTACT").inserer();

        lire(P_RECHERCHE, jeton, P_SIZE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(2));

        List<String> lus = new ArrayList<>(ids(P_RECHERCHE, jeton, P_SIZE, "1"));
        lus.addAll(ids(P_RECHERCHE, jeton, P_SIZE, "1", P_PAGE, "1"));
        assertThat(lus).containsExactlyInAnyOrder(une, autre);
    }

    // ------------------------------------------------------------------ règle 3 : recherche

    // Seule la colonne visée est remplie : une concaténation sans ISNULL perdrait la ligne.
    @ParameterizedTest
    @ValueSource(strings = {CODE, DESCRIPTION, ACTION, CREATEUR, MODIFICATEUR})
    void r3_recherche_contientSurChacuneDesCinqColonnes(String colonne) throws Exception {
        String retenue = fixture.ligneVide().avec(colonne, "avant-" + jeton + "-apres").inserer();
        fixture.ligneVide().avec(colonne, "avant-apres").inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(retenue);
    }

    @ParameterizedTest
    @CsvSource({
            "Téléphone Sortant,Téléphone Sortant",
            "Téléphone Sortant,TéléphoneSortant",
            "TéléphoneSortant,Téléphone Sortant",
            "TéléphoneSortant,TéléphoneSortant",
            "Téléphone Sortant,Télé phone  Sortant"})
    void r3_espacesIgnoresDesDeuxCotes(String enBase, String saisi) throws Exception {
        String retenue = ligne(DATE).avec(ACTION, enBase + " " + jeton).inserer();
        ligne(DATE).avec(ACTION, "Téléphone Entrant " + jeton).inserer();

        assertThat(ids(P_RECHERCHE, saisi + " " + jeton)).containsExactly(retenue);
    }

    @Test
    void r3_rechercheCompacteTrouve_lesDeuxEcritures() throws Exception {
        String avecEspace = ligne(DATE).avec(DESCRIPTION, TELEPHONE_AVEC_ESPACE + jeton).inserer();
        String sansEspace = ligne(DATE).avec(DESCRIPTION, TELEPHONE_SANS_ESPACE + jeton).inserer();

        assertThat(ids(P_RECHERCHE, TELEPHONE_SANS_ESPACE + jeton)).containsExactlyInAnyOrder(avecEspace, sansEspace);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a%b", "a_b", "a[b", "a[x]b"})
    void r3_caracteresJokersEchappes(String motif) throws Exception {
        String exacte = fixture.ligneVide().avec(CODE, jeton + motif).inserer();
        fixture.ligneVide().avec(CODE, jeton + "axb").inserer();

        assertThat(ids(P_RECHERCHE, jeton + motif)).containsExactly(exacte);
    }

    // La période isole la ligne, dont aucune colonne ne contient d'espace.
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void r3_rechercheVideOuBlanche_pasDeFiltre(String vide) throws Exception {
        LocalDateTime jourIsole = LocalDateTime.of(2003, 5, 6, 7, 8);
        String sansTexte = fixture.ligneVide().avec(CREE_LE, jourIsole).inserer();

        List<String> lus = ids(P_RECHERCHE, vide, P_CREATION_DEBUT, "2003-05-06", P_CREATION_FIN, "2003-05-06");

        assertThat(lus).contains(sansTexte);
    }

    // ------------------------------------------------------------------ règle 4 : périodes

    static Stream<Arguments> bornesDuJour() {
        return Stream.of(
                Arguments.of(MINUIT.minusSeconds(1), false),
                Arguments.of(MINUIT, true),
                Arguments.of(MINUIT.plusHours(23).plusMinutes(59), true),
                Arguments.of(MINUIT.plusDays(1).minusNanos(3_000_000), true),
                Arguments.of(MINUIT.plusDays(1), false));
    }

    @ParameterizedTest
    @MethodSource("bornesDuJour")
    void r4_creation_debutInclus_finInclusJusquAuDernierInstant(LocalDateTime creeLe, boolean attendu) throws Exception {
        String ligne = ligne(creeLe).inserer();

        List<String> lus = ids(P_RECHERCHE, jeton, P_CREATION_DEBUT, JOUR, P_CREATION_FIN, JOUR);

        assertThat(lus).isEqualTo(attendu ? List.of(ligne) : List.of());
    }

    @ParameterizedTest
    @MethodSource("bornesDuJour")
    void r4_modification_debutInclus_finInclusJusquAuDernierInstant(LocalDateTime modifieLe, boolean attendu) throws Exception {
        String ligne = ligne(DATE.minusYears(1)).avec(MODIFIE_LE, modifieLe).inserer();

        List<String> lus = ids(P_RECHERCHE, jeton, P_MODIFICATION_DEBUT, JOUR, P_MODIFICATION_FIN, JOUR);

        assertThat(lus).isEqualTo(attendu ? List.of(ligne) : List.of());
    }

    @ParameterizedTest
    @CsvSource({"creationDebut,true", "creationFin,false", "modificationDebut,true", "modificationFin,false"})
    void r4_borneSeule_bornesDUnSeulCote(String borne, boolean debut) throws Exception {
        LocalDateTime dehors = debut ? DATE.minusDays(2) : DATE.plusDays(2);
        String retenue = ligne(DATE).avec(MODIFIE_LE, DATE).inserer();
        ligne(dehors).avec(MODIFIE_LE, dehors).inserer();

        assertThat(ids(P_RECHERCHE, jeton, borne, JOUR)).containsExactly(retenue);
    }

    @Test
    void r4_periodesCumulables() throws Exception {
        String retenue = ligne(DATE).avec(MODIFIE_LE, DATE.plusDays(1)).inserer();
        ligne(DATE).avec(MODIFIE_LE, DATE.plusDays(5)).inserer();
        ligne(DATE.minusDays(5)).avec(MODIFIE_LE, DATE.plusDays(1)).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_CREATION_DEBUT, JOUR, P_CREATION_FIN, JOUR,
                P_MODIFICATION_DEBUT, LENDEMAIN, P_MODIFICATION_FIN, LENDEMAIN)).containsExactly(retenue);
    }

    @ParameterizedTest
    @CsvSource({
            "creationDebut,2020-01-01,UserCreateDateTime",
            "creationFin,2030-01-01,UserCreateDateTime",
            "modificationDebut,2020-01-01,UserUpdateDateTime",
            "modificationFin,2030-01-01,UserUpdateDateTime"})
    void r4_ligneSansDate_excludeDesQuUneBorneDeSaPeriode(String borne, String valeur, String colonne) throws Exception {
        String datee = ligne(DATE).inserer();
        ligne(DATE).sans(colonne).inserer();

        assertThat(ids(P_RECHERCHE, jeton, borne, valeur)).containsExactly(datee);
    }

    @Test
    void r4_ligneSansDate_gardeeParLaPeriodeDeLAutreColonne() throws Exception {
        String sansModification = ligne(DATE).sans(MODIFIE_LE).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_CREATION_DEBUT, JOUR, P_CREATION_FIN, JOUR))
                .containsExactly(sansModification);
    }

    // Décision du manager : aucune période préréglée, l'écran s'ouvre sur tout.
    @Test
    void decision_sansDate_aucuneBorneParDefaut() throws Exception {
        String ancienne = ligne(LocalDateTime.of(2001, 1, 2, 3, 4)).inserer();
        String sansDate = ligne(DATE).sans(CREE_LE).sans(MODIFIE_LE).inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(ancienne, sansDate);
    }

    // ------------------------------------------------------------------ règle 5 : périodes inversées

    @Test
    void r5_creationDebutApresFin_400() throws Exception {
        lire(P_CREATION_DEBUT, LENDEMAIN, P_CREATION_FIN, JOUR).andExpect(status().isBadRequest());
    }

    @Test
    void r5_modificationDebutApresFin_400() throws Exception {
        lire(P_MODIFICATION_DEBUT, LENDEMAIN, P_MODIFICATION_FIN, JOUR).andExpect(status().isBadRequest());
    }

    @Test
    void r5_memeJour_200() throws Exception {
        lire(P_CREATION_DEBUT, JOUR, P_CREATION_FIN, JOUR, P_MODIFICATION_DEBUT, JOUR, P_MODIFICATION_FIN, JOUR)
                .andExpect(status().isOk());
    }

    @Test
    void r5_chaquePeriodeSeJugeSeule_debutCreationApresFinModification_200() throws Exception {
        lire(P_CREATION_DEBUT, LENDEMAIN, P_MODIFICATION_FIN, JOUR).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 6 : pagination

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1000001"})
    void r6_pageHorsBornes_400(String page) throws Exception {
        lire(P_PAGE, page).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "201"})
    void r6_sizeHorsBornes_400(String size) throws Exception {
        lire(P_SIZE, size).andExpect(status().isBadRequest());
    }

    @Test
    void r6_pageMaximale_200_contenuVide() throws Exception {
        lire(P_PAGE, "1000000", P_SIZE, "200")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1_000_000))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 200})
    void r6_sizeAuxBornes_200(int size) throws Exception {
        lire(P_SIZE, String.valueOf(size))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(size));
    }

    // ------------------------------------------------------------------ règle 7 : valeurs vides

    @Test
    void r7_colonnesNulles_rendues_sansPlantage() throws Exception {
        fixture.ligneVide().avec(ACTION, "VIDE " + jeton).inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_ACTION).value("VIDE " + jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CODE).doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_DESCRIPTION).doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CREE_PAR).doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CREE_LE).doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_MODIFIE_PAR).doesNotExist())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_MODIFIE_LE).doesNotExist());
    }

    @Test
    void r7_chainesVides_renduesTellesQuelles() throws Exception {
        ligne(DATE).avec(DESCRIPTION, "").avec(CREATEUR, "").avec(MODIFICATEUR, "").inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_DESCRIPTION).value(""))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CREE_PAR).value(""))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_MODIFIE_PAR).value(""));
    }

    // ------------------------------------------------------------------ section Back : champs du fil

    @Test
    void back_ligne_exposeExactementLesChampsDuSpec() throws Exception {
        ligne(DATE).inserer();

        Map<String, Object> ligne = JsonPath.read(corps(lire(P_RECHERCHE, jeton)), "$.content[0]");

        assertThat(ligne).containsOnlyKeys("id", CHAMP_CODE, CHAMP_ACTION, CHAMP_DESCRIPTION,
                CHAMP_CREE_PAR, CHAMP_CREE_LE, CHAMP_MODIFIE_PAR, CHAMP_MODIFIE_LE);
    }

    @Test
    void back_ligne_valeursDesColonnes() throws Exception {
        UUID pkey = UUID.randomUUID();
        ligne(DATE)
                .avec(PKEY, pkey)
                .avec(ACTION, "PROSPECTION TÉLÉPHONE")
                .avec(DESCRIPTION, "Rappel de Mme Résidence")
                .avec(CREATEUR, "Karim")
                .avec(MODIFICATEUR, "Salma")
                .avec(MODIFIE_LE, DATE.plusHours(2))
                .inserer();

        String id = JsonPath.read(corps(lire(P_RECHERCHE, jeton)), PREMIERE_LIGNE + "id");

        assertThat(id).isEqualToIgnoringCase(pkey + ":PROSPECTION TÉLÉPHONE");
        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CODE).value("ACT-" + jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_ACTION).value("PROSPECTION TÉLÉPHONE"))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_DESCRIPTION).value("Rappel de Mme Résidence"))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CREE_PAR).value("Karim"))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_CREE_LE, startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_MODIFIE_PAR).value("Salma"))
                .andExpect(jsonPath(PREMIERE_LIGNE + CHAMP_MODIFIE_LE, startsWith("2026-03-10T11:15:30")));
    }

    // ------------------------------------------------------------------ section Back : pagination

    @Test
    void back_pagination_parDefautPage0Taille50() throws Exception {
        lire()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void back_pagination_totalDuFiltre_etPageDemandee() throws Exception {
        ligne(DATE).inserer();
        String milieu = ligne(DATE.minusDays(1)).inserer();
        ligne(DATE.minusDays(2)).inserer();
        ligneSansMarque(DATE).inserer();

        lire(P_RECHERCHE, jeton, P_PAGE, "1", P_SIZE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(3));

        assertThat(ids(P_RECHERCHE, jeton, P_PAGE, "1", P_SIZE, "1")).containsExactly(milieu);
    }

    @Test
    void back_rechercheEtPeriodes_cumulees() throws Exception {
        String retenue = ligne(DATE).inserer();
        ligne(DATE.minusDays(3)).inserer();
        ligneSansMarque(DATE).inserer();

        assertThat(ids(P_RECHERCHE, jeton, P_CREATION_DEBUT, JOUR, P_CREATION_FIN, JOUR)).containsExactly(retenue);
    }
}
