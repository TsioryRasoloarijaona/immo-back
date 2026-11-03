package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers;
import com.tsix.immocrm.fixtures.FixtureReferentielsTiers.ListeTiers;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin.TableTache;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ORIENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RACINE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.L_TYPES_TIERS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.PAYS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsTiers.TYPES_TIERS;
import static com.tsix.immocrm.fixtures.FixtureTachesAdmin.TYPES_TACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-admin-referentiels-tiers.md, règles 6 (Types de tiers en lecture seule : 409 documenté), 7 (écritures réservées
 * à l'Administrateur, lecture ouverte, témoins 2xx) et 8 (Patrimoine et Tâches inchangés : code modifiable, pas de
 * lecture seule).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminTiersSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsTiers fixture;
    @Autowired private FixtureTachesAdmin taches;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    static Stream<String> slugs() {
        return FixtureReferentielsTiers.slugs();
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private int lignesDeCode(ListeTiers l, String code) {
        return fixture.compter("SELECT COUNT(*) FROM " + l.table() + " WHERE Code = N'" + code + "'");
    }

    private <T> List<T> descripteur(String slug, String suite) throws Exception {
        String json = mockMvc.perform(get(RACINE).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$[?(@.slug=='" + slug + "')]" + suite);
    }

    // ------------------------------------------------------------------ règle 7 : écritures réservées

    @Test
    void r7_creation_403_sansAdministrateur_rienEcrit_temoin201() throws Exception {
        String refuse = "COM-" + jeton;

        mockMvc.perform(post(TABLE, PAYS).with(commercial(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(L_PAYS.saisie(refuse).json()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(TABLE, PAYS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(L_PAYS.saisie("ADM-" + jeton).json()))
                .andExpect(status().isCreated());

        assertAll(
                () -> assertThat(lignesDeCode(L_PAYS, refuse)).isZero(),
                () -> assertThat(lignesDeCode(L_PAYS, "ADM-" + jeton)).isEqualTo(1));
    }

    @Test
    void r7_modification_403_sansAdministrateur_ligneInchangee_temoin200() throws Exception {
        String code = "COP-" + jeton;
        UUID id = fixture.pays(code);

        mockMvc.perform(patch(LIGNE, PAYS, id).with(commercial(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Refuse").json()))
                .andExpect(status().isForbidden());
        String apresRefus = fixture.texte(L_PAYS, id, "Description");
        mockMvc.perform(patch(LIGNE, PAYS, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Admis " + jeton).json()))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(apresRefus).isEqualTo("Libelle " + code),
                () -> assertThat(fixture.texte(L_PAYS, id, "Description")).isEqualTo("Admis " + jeton));
    }

    @Test
    void r7_suppression_403_sansAdministrateur_ligneConservee_temoin204() throws Exception {
        UUID id = fixture.pays("COD-" + jeton);

        mockMvc.perform(delete(LIGNE, PAYS, id).with(commercial(admin))).andExpect(status().isForbidden());
        int apresRefus = fixture.lignes(L_PAYS, id);
        mockMvc.perform(delete(LIGNE, PAYS, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(apresRefus).isEqualTo(1),
                () -> assertThat(fixture.lignes(L_PAYS, id)).isZero());
    }

    /** Décision 5 : la lecture garde la règle du moteur (ouverte à tout compte connecté). */
    @ParameterizedTest(name = "{0}")
    @MethodSource("slugs")
    void r7_lecture_200_sansAdministrateur(String slug) throws Exception {
        mockMvc.perform(get(TABLE, slug).with(commercial(admin))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 6 : Types de tiers en lecture seule

    @Test
    void r6_typesTiers_lecture_200_listeEtLigne() throws Exception {
        UUID id = fixture.typeTiers("TT-" + jeton);

        mockMvc.perform(get(TABLE, TYPES_TIERS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].code").value("TT-" + jeton));
        mockMvc.perform(get(LIGNE, TYPES_TIERS, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TT-" + jeton));
    }

    @Test
    void r6_typesTiers_creation_409_rienEcrit() throws Exception {
        String code = "TTC-" + jeton;

        mockMvc.perform(post(TABLE, TYPES_TIERS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(L_TYPES_TIERS.saisie(code).json()))
                .andExpect(status().isConflict());

        assertThat(lignesDeCode(L_TYPES_TIERS, code)).isZero();
    }

    /** Spec : le 409 tombe avant toute lecture du corps (un corps vide n'est pas un 400). */
    @Test
    void r6_typesTiers_creation_409_avantLectureDuCorps() throws Exception {
        mockMvc.perform(post(TABLE, TYPES_TIERS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void r6_typesTiers_modification_409_ligneInchangee() throws Exception {
        String code = "TTM-" + jeton;
        UUID id = fixture.typeTiers(code);

        mockMvc.perform(patch(LIGNE, TYPES_TIERS, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Nouveau " + jeton).json()))
                .andExpect(status().isConflict());

        assertThat(fixture.texte(L_TYPES_TIERS, id, "Description")).isEqualTo("Libelle " + code);
    }

    @Test
    void r6_typesTiers_suppression_409_ligneConservee() throws Exception {
        UUID id = fixture.typeTiers("TTS-" + jeton);

        mockMvc.perform(delete(LIGNE, TYPES_TIERS, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.lignes(L_TYPES_TIERS, id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règle 8 : Patrimoine et Tâches inchangés

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ORIENTATIONS, TYPES_TACHE})
    void r8_descripteurExistant_codeModifiable_pasEnLectureSeule(String slug) throws Exception {
        taches.assurerTables();

        List<Boolean> lectureSeule = descripteur(slug, ".lectureSeule");
        List<Boolean> codeModifiable = descripteur(slug, ".champs[?(@.nom=='" + CODE + "')].modifiable");

        assertAll(
                () -> assertThat(lectureSeule).containsExactly(false),
                () -> assertThat(codeModifiable).containsExactly(true));
    }

    @Test
    void r8_patrimoine_modificationDuCode_200() throws Exception {
        String json = mockMvc.perform(post(TABLE, ORIENTATIONS).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(SORTING, 1).avec(CODE, "OR-" + jeton).avec(DESCRIPTION, "x").json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        mockMvc.perform(patch(LIGNE, ORIENTATIONS, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "OR2-" + jeton).json()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OR2-" + jeton));
    }

    @Test
    void r8_taches_modificationDuCode_200() throws Exception {
        taches.assurerTables();
        TableTache types = FixtureTachesAdmin.tables().filter(t -> !t.avecTri()).findFirst().orElseThrow();
        UUID id = taches.ligne(types, "TY-" + jeton);

        mockMvc.perform(patch(LIGNE, TYPES_TACHE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "TY2-" + jeton).json()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TY2-" + jeton));
    }
}
