package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin.TableTache;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin.TraceLigne;
import com.tsix.immocrm.security.AuthenticatedUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RACINE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEARCH;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.AUTEUR_OMRAN;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.NOTES;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.PRIORITES_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TABLE_PRIORITES;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TRI;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TYPES_TACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Type et priorité de tâche sur {@code /api/referentiels-admin} — FEATURE-admin-type-priorite-tache.md, règles 1 à 6
 * et 8. Les lignes préexistantes passent par le SQL de la fixture : un rouge prouve la route, pas la fixture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminTachesTest {

    private static final String ID = "$.id";
    private static final String CHAMPS = "champs";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin referentiels;
    @Autowired private FixtureTachesAdmin fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.assurerTables();
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    static Stream<TableTache> tables() {
        return FixtureTachesAdmin.tables();
    }

    static Stream<Arguments> descripteurs() {
        return Stream.of(
                Arguments.of(TYPES_TACHE, "Types de tâche", List.of(CODE, DESCRIPTION, NOTES)),
                Arguments.of(PRIORITES_TACHE, "Priorités de tâche", List.of(SORTING, CODE, DESCRIPTION, NOTES)));
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private static TableTache priorites() {
        return tables().filter(TableTache::avecTri).findFirst().orElseThrow();
    }

    private static String utf8(ResultActions reponse) throws UnsupportedEncodingException {
        return reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String lireDescripteurs() throws Exception {
        return utf8(mockMvc.perform(get(RACINE).with(connecte(admin))).andExpect(status().isOk()));
    }

    private static String chemin(String slug, String suite) {
        return "$[?(@.slug=='" + slug + "')]" + suite;
    }

    private ResultActions poster(String slug, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, slug).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(String slug, UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(LIGNE, slug, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creer(String slug, CorpsJson corps) throws Exception {
        String json = utf8(poster(slug, corps).andExpect(status().isCreated()));
        return UUID.fromString(JsonPath.read(json, ID));
    }

    private int lignesCompletes(TableTache t, UUID id, String code) {
        return referentiels.compter("SELECT COUNT(*) FROM " + t.table() + " WHERE PKey = :id AND Code = N'" + code
                + "' AND Description = N'Libelle " + code + "' AND Notes = N'Notes " + code + "'", id);
    }

    private ResultActions lireLigne(String slug, UUID id) throws Exception {
        return mockMvc.perform(get(LIGNE, slug, id).with(connecte(admin))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 1 : descripteurs

    @ParameterizedTest(name = "{0}")
    @MethodSource("descripteurs")
    void r1_descripteur_familleTaches_libelleEtChampsDansLOrdre(String slug, String libelle, List<String> champs)
            throws Exception {
        String json = lireDescripteurs();

        List<String> familles = JsonPath.read(json, chemin(slug, ".famille"));
        List<String> libelles = JsonPath.read(json, chemin(slug, ".libelle"));
        List<String> noms = JsonPath.read(json, chemin(slug, "." + CHAMPS + "[*].nom"));
        assertAll(
                () -> assertThat(familles).containsExactly("TACHES"),
                () -> assertThat(libelles).containsExactly(libelle),
                () -> assertThat(noms).isEqualTo(champs));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {TYPES_TACHE, PRIORITES_TACHE})
    void r1_descripteur_cleUnique_codeSeul(String slug) throws Exception {
        List<String> uniques = JsonPath.read(lireDescripteurs(), chemin(slug, "." + CHAMPS + "[?(@.unique==true)].nom"));

        assertThat(uniques).containsExactly(CODE);
    }

    @Test
    void r1_descripteur_sortingRequis_surLesPriorites() throws Exception {
        List<Boolean> requis = JsonPath.read(lireDescripteurs(),
                chemin(PRIORITES_TACHE, "." + CHAMPS + "[?(@.nom=='sorting')].obligatoire"));

        assertThat(requis).containsExactly(true);
    }

    // ------------------------------------------------------------------ règle 2 : tri

    @Test
    void r2_priorites_lignesTrieesParSorting() throws Exception {
        creer(PRIORITES_TACHE, priorites().saisie("A-" + jeton).avec(SORTING, 30));
        creer(PRIORITES_TACHE, priorites().saisie("B-" + jeton).avec(SORTING, 10));
        creer(PRIORITES_TACHE, priorites().saisie("C-" + jeton).avec(SORTING, 20));

        String json = utf8(mockMvc.perform(get(TABLE, PRIORITES_TACHE).with(connecte(admin))).andExpect(status().isOk()));

        List<String> codes = JsonPath.read(json, "$[*].code");
        assertThat(codes.stream().filter(c -> c.endsWith(jeton)).toList())
                .containsExactly("B-" + jeton, "C-" + jeton, "A-" + jeton);
    }

    // ------------------------------------------------------------------ règle 3 : création

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r3_creation_201_ligneRelue(TableTache t) throws Exception {
        String code = "CRE-" + jeton;

        UUID id = creer(t.slug(), t.saisie(code));

        lireLigne(t.slug(), id)
                .andExpect(jsonPath(ID).value(id.toString()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.description").value("Libelle " + code))
                .andExpect(jsonPath("$.notes").value("Notes " + code));
        assertThat(lignesCompletes(t, id, code)).isEqualTo(1);
    }

    @Test
    void r3_creation_201_sortingEcrit() throws Exception {
        UUID id = creer(PRIORITES_TACHE, priorites().saisie("TRI-" + jeton).avec(SORTING, 42));

        assertThat(referentiels.compter("SELECT COUNT(*) FROM " + TABLE_PRIORITES + " WHERE PKey = :id AND Sorting = 42", id))
                .isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r3_creation_400_codeAbsent(TableTache t) throws Exception {
        poster(t.slug(), t.saisie("SC-" + jeton).sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_creation_400_sortingAbsent_surUnePriorite() throws Exception {
        poster(PRIORITES_TACHE, priorites().saisie("ST-" + jeton).sans(SORTING)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r3_creation_409_codeDejaPris(TableTache t) throws Exception {
        creer(t.slug(), t.saisie("DBL-" + jeton));

        poster(t.slug(), t.saisie("DBL-" + jeton)).andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r3_creation_409_casseIgnoree(TableTache t) throws Exception {
        creer(t.slug(), t.saisie("Casse-" + jeton));

        poster(t.slug(), t.saisie("cASSE-" + jeton.toLowerCase(Locale.ROOT))).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ règle 4 : modification partielle

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r4_patch_200_champAbsentInchange(TableTache t) throws Exception {
        String code = "MOD-" + jeton;
        UUID id = creer(t.slug(), t.saisie(code));

        modifier(t.slug(), id, corps().avec(DESCRIPTION, "Nouveau " + jeton)).andExpect(status().isOk());

        assertThat(referentiels.compter("SELECT COUNT(*) FROM " + t.table() + " WHERE PKey = :id AND Code = N'" + code
                + "' AND Notes = N'Notes " + code + "' AND Description = N'Nouveau " + jeton + "'", id)).isEqualTo(1);
    }

    @Test
    void r4_patch_200_sortingInchangeSiAbsent() throws Exception {
        UUID id = creer(PRIORITES_TACHE, priorites().saisie("MTR-" + jeton));

        modifier(PRIORITES_TACHE, id, corps().avec(NOTES, "Autre " + jeton)).andExpect(status().isOk());

        assertThat(referentiels.compter("SELECT COUNT(*) FROM " + TABLE_PRIORITES + " WHERE PKey = :id AND Sorting = "
                + TRI, id)).isEqualTo(1);
    }

    @Test
    void r4_patch_200_sortingSeulModifie() throws Exception {
        String code = "MS-" + jeton;
        UUID id = creer(PRIORITES_TACHE, priorites().saisie(code));

        modifier(PRIORITES_TACHE, id, corps().avec(SORTING, 99)).andExpect(status().isOk());

        assertAll(
                () -> assertThat(lignesCompletes(priorites(), id, code)).isEqualTo(1),
                () -> assertThat(referentiels.compter("SELECT COUNT(*) FROM " + TABLE_PRIORITES
                        + " WHERE PKey = :id AND Sorting = 99", id)).isEqualTo(1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r4_patch_409_codeDUneAutreLigne_ligneConservee(TableTache t) throws Exception {
        creer(t.slug(), t.saisie("PRIS-" + jeton));
        UUID id = creer(t.slug(), t.saisie("LIBRE-" + jeton));

        modifier(t.slug(), id, corps().avec(CODE, "PRIS-" + jeton)).andExpect(status().isConflict());

        assertThat(lignesCompletes(t, id, "LIBRE-" + jeton)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r4_patch_409_casseIgnoree(TableTache t) throws Exception {
        creer(t.slug(), t.saisie("Pris-" + jeton));
        UUID id = creer(t.slug(), t.saisie("LIBRE-" + jeton));

        modifier(t.slug(), id, corps().avec(CODE, "pRIS-" + jeton.toLowerCase(Locale.ROOT)))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ règle 5 : suppression

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r5_suppression_204_sansReference(TableTache t) throws Exception {
        UUID id = fixture.ligne(t, "DEL-" + jeton);

        mockMvc.perform(delete(LIGNE, t.slug(), id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(referentiels.lignes(t.table(), id)).isZero();
    }

    /** FK NO_ACTION de {@code AdmTask}, posée en CI par ci/schema et en local par la fixture. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r5_suppression_409_referenceeParUneTache_conservee(TableTache t) throws Exception {
        UUID id = fixture.ligne(t, "REF-" + jeton);
        fixture.tacheReferencant(t, id);

        mockMvc.perform(delete(LIGNE, t.slug(), id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(referentiels.lignes(t.table(), id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règle 6 : écritures réservées

    /** La relecture Administrateur prouve que le 403 tombe sur un référentiel qui existe. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r6_post_403_sansRoleAdministrateur_rienEcrit(TableTache t) throws Exception {
        String code = "COM-" + jeton;

        mockMvc.perform(post(TABLE, t.slug()).with(commercial(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(t.saisie(code).json()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(TABLE, t.slug()).with(connecte(admin)).param(SEARCH, code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.code=='" + code + "')]").doesNotExist());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r6_patch_403_sansRoleAdministrateur_ligneInchangee(TableTache t) throws Exception {
        String code = "COP-" + jeton;
        UUID id = fixture.ligne(t, code);

        mockMvc.perform(patch(LIGNE, t.slug(), id).with(commercial(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Refuse").json()))
                .andExpect(status().isForbidden());

        lireLigne(t.slug(), id).andExpect(jsonPath("$.description").value("Libelle " + code));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r6_delete_403_sansRoleAdministrateur_ligneConservee(TableTache t) throws Exception {
        UUID id = fixture.ligne(t, "COD-" + jeton);

        mockMvc.perform(delete(LIGNE, t.slug(), id).with(commercial(admin))).andExpect(status().isForbidden());

        lireLigne(t.slug(), id);
    }

    // ------------------------------------------------------------------ règle 8 : colonnes d'audit de la ligne

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r8_creation_renseigneUserCreateEtUserUpdate(TableTache t) throws Exception {
        UUID id = creer(t.slug(), t.saisie("AUD-" + jeton));

        TraceLigne trace = fixture.trace(t, id);

        assertAll(
                () -> assertThat(trace.creePar()).isEqualTo(AUTEUR),
                () -> assertThat(trace.creeLe()).isNotNull(),
                () -> assertThat(trace.modifiePar()).isEqualTo(AUTEUR),
                () -> assertThat(trace.modifieLe()).isNotNull());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void r8_modification_renseigneUserUpdate_creationConservee(TableTache t) throws Exception {
        UUID id = fixture.ligne(t, "AUM-" + jeton);
        Object creeLe = fixture.trace(t, id).creeLe();

        modifier(t.slug(), id, corps().avec(DESCRIPTION, "Trace " + jeton)).andExpect(status().isOk());

        TraceLigne trace = fixture.trace(t, id);
        assertAll(
                () -> assertThat(trace.creePar()).isEqualTo(AUTEUR_OMRAN),
                () -> assertThat(trace.creeLe()).isEqualTo(creeLe),
                () -> assertThat(trace.modifiePar()).isEqualTo(AUTEUR),
                () -> assertThat(trace.modifieLe()).isNotNull());
    }
}
