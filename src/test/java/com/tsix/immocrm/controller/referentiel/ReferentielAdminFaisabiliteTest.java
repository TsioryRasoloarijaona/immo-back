package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuration des études (patron P6 {@code /api/referentiels-admin/{table}}) — FEATURE-patrimoine-faisabilite.md §3 :
 * {@code unites-de-base} ({@code StkFSBaseUnit}, sans colonnes d'audit) et {@code fonctions-specifiques}
 * ({@code SpecificFunction}) ; unicité, requis, suppression refusée d'une ligne utilisée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminFaisabiliteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin referentiels;
    @Autowired private FixtureFaisabilite fixture;

    private AdmUser admin;
    private ApiFaisabilite api;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        jeton = jeton();
    }

    private ResultActions poster(String slug, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(REFERENTIELS, slug).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creer(String slug, CorpsJson corps) throws Exception {
        String json = poster(slug, corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private static CorpsJson corpsUnite(String code) {
        return corps().avec(SORTING, 2).avec(CODE, code).avec(DESCRIPTION, "Libelle " + code).avec(EST_SURFACE, true);
    }

    private static CorpsJson corpsFonction(String code) {
        return corps().avec(SORTING, 2).avec(CODE, code).avec(DESCRIPTION, "Libelle " + code).avec(EST_TOTAL, false)
                .avec(SURFACE_VENDABLE, true).avec(F_PLANCHER_COUVERT, false).avec(SURFACE_NETTE, true)
                .avec(SURFACE_BRUTE, false).avec(F_EMPRISE_AU_SOL, true);
    }

    private CorpsJson corpsDe(String slug, String code) {
        return SLUG_UNITES_BASE.equals(slug) ? corpsUnite(code) : corpsFonction(code);
    }

    // ------------------------------------------------------------------ unités de base

    /** {@code StkFSBaseUnit} n'a aucune colonne d'audit : la création passe ({@code tracer} teste isWritableProperty). */
    @Test
    void r3_uniteDeBase_creation_201_colonnesEcrites() throws Exception {
        UUID fonction = creer(SLUG_FONCTIONS, corpsFonction("FU-" + jeton));
        StkItemCategory categorie = patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false);

        UUID id = creer(SLUG_UNITES_BASE, corpsUnite("UB-" + jeton).avec(FONCTION_SPECIFIQUE_ID, fonction)
                .avec(CATEGORIE_ARTICLE_ID, categorie.getId()));

        assertThat(referentiels.compter("SELECT COUNT(*) FROM StkFSBaseUnit WHERE PKey = :id AND Sorting = 2 AND Code = 'UB-"
                + jeton + "' AND Description = 'Libelle UB-" + jeton + "' AND IsArea = 1 AND SpecificFunctionId = '" + fonction
                + "' AND StkItemCategoryId = '" + categorie.getId() + "'", id)).isEqualTo(1);
    }

    @Test
    void r3_fonctionSpecifique_creation_201_sixDrapeauxEcrits() throws Exception {
        UUID id = creer(SLUG_FONCTIONS, corpsFonction("FS-" + jeton));

        assertThat(referentiels.compter("SELECT COUNT(*) FROM SpecificFunction WHERE PKey = :id AND Sorting = 2 AND Code = N'FS-"
                + jeton + "' AND IsTotal = 0 AND SaleAbleArea = 1 AND CoveredArea = 0 AND AreaNet = 1 AND AreaBrut = 0"
                + " AND AreaCos = 1", id)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SLUG_UNITES_BASE, SLUG_FONCTIONS})
    void r3_unicite_409_codeDejaPris(String slug) throws Exception {
        creer(slug, corpsDe(slug, "DBL-" + jeton));

        poster(slug, corpsDe(slug, "DBL-" + jeton)).andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "{0} sans {1}")
    @CsvSource({
            "unites-de-base, sorting", "unites-de-base, code", "unites-de-base, description",
            "fonctions-specifiques, sorting", "fonctions-specifiques, code", "fonctions-specifiques, description"})
    void r3_requis_400(String slug, String champ) throws Exception {
        poster(slug, corpsDe(slug, "REQ-" + jeton).sans(champ)).andExpect(status().isBadRequest());
    }

    /** §3 : {@code bloquants()} = lignes d'étude (CASCADE en base) → 409 plutôt que d'effacer la grille d'une étude. */
    @Test
    void r3_uniteUtiliseeParUneEtude_suppression_409_conservee() throws Exception {
        UUID unite = api.unite("USE-" + jeton, false, null);
        UUID etude = api.etude(yestest(FixtureFaisabilite.fiche("USE-" + jeton,
                patrimoine.entite("ENT-" + jeton, true).getId())));
        api.unites(etude, lignes(ligneUniteCorps(1, unite, d("10")))).andExpect(status().isOk());

        mockMvc.perform(delete(REFERENTIEL, SLUG_UNITES_BASE, unite).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(referentiels.lignes("StkFSBaseUnit", unite)).isEqualTo(1);
    }

    /** §3 : FK NO_ACTION StkFSBaseUnit → SpecificFunction : 409 (en CI seulement, yesRemsTest n'a pas de FK). */
    @Test
    void r3_fonctionUtiliseeParUneUnite_suppression_409_conservee() throws Exception {
        UUID fonction = creer(SLUG_FONCTIONS, corpsFonction("FUS-" + jeton));
        creer(SLUG_UNITES_BASE, corpsUnite("UFS-" + jeton).avec(FONCTION_SPECIFIQUE_ID, fonction));

        mockMvc.perform(delete(REFERENTIEL, SLUG_FONCTIONS, fonction).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(referentiels.lignes("SpecificFunction", fonction)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SLUG_UNITES_BASE, SLUG_FONCTIONS})
    void r3_suppression_204_ligneLibre(String slug) throws Exception {
        UUID id = creer(slug, corpsDe(slug, "DEL-" + jeton));

        mockMvc.perform(delete(REFERENTIEL, slug, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(referentiels.lignes(SLUG_UNITES_BASE.equals(slug) ? "StkFSBaseUnit" : "SpecificFunction", id)).isZero();
    }

    /** §3 / §4 : famille « Autres », servies par les descripteurs du patron P6. */
    @Test
    void r3_descripteurs_listentLesDeuxSlugs() throws Exception {
        mockMvc.perform(get(FixtureReferentielsAdmin.RACINE).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"" + SLUG_UNITES_BASE + "\"")))
                .andExpect(content().string(containsString("\"" + SLUG_FONCTIONS + "\"")));
    }
}
