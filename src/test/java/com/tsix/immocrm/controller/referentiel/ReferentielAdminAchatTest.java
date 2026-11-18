package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CODE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_NATURES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_SOUS_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.NATURES_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.SORTING;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.SOUS_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.TYPE_CLIENT;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.TYPE_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Familles, sous-familles d'achat et natures fournisseur sur le moteur {@code /api/referentiels-admin}
 * (FEATURE-n8-nomenclature.md R1, R2, R5, D3, D4, D6). Slugs : {@code familles-achat}, {@code sous-familles-achat},
 * {@code natures-fournisseur}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminAchatTest {

    private static final String DETAIL = "$.detail";
    private static final String CODE_EXISTANT = "Ce code existe déjà.";
    private static final String SUPPRESSION_IMPOSSIBLE = "Suppression impossible : ";
    private static final String UTILISE_PAR_UN = "est utilisé par 1 ";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private RequestPostProcessor admin;
    private UUID categorie;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        admin = fixture.administrateur();
        categorie = fixture.categorie(jeton("CAT"));
    }

    // ------------------------------------------------------------------ outils

    private ResultActions poster(RequestPostProcessor qui, String slug, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, slug).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions poster(String slug, CorpsJson corps) throws Exception {
        return poster(admin, slug, corps);
    }

    private ResultActions supprimer(String slug, UUID id) throws Exception {
        return mockMvc.perform(delete(LIGNE, slug, id).with(admin));
    }

    private CorpsJson famille(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Famille " + code).avec(CATEGORIE, categorie);
    }

    private CorpsJson sousFamille(String code, UUID famille) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Sous-famille " + code).avec(CATEGORIE, categorie)
                .avec(FAMILLE, famille);
    }

    private static CorpsJson nature(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Nature " + code);
    }

    /** Corps valide de création pour ce slug (parents créés en base). */
    private CorpsJson saisieValide(String slug) {
        String code = jeton("N8");
        return switch (slug) {
            case FAMILLES -> famille(code);
            case SOUS_FAMILLES -> sousFamille(code, fixture.famille(jeton("FAM"), categorie));
            default -> nature(code);
        };
    }

    private List<String> ids(ResultActions reponse) throws Exception {
        String json = reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$[*].id");
    }

    // ------------------------------------------------------------------ R1 création et unicité

    @Test
    void r1_famille_creee_puis_relue() throws Exception {
        String code = jeton("FAM");

        poster(FAMILLES, famille(code).avec("notes", "Note " + code))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.description").value("Famille " + code))
                .andExpect(jsonPath("$." + CATEGORIE).value(categorie.toString()))
                .andExpect(jsonPath("$.notes").value("Note " + code));
    }

    @Test
    void r1_sousFamille_creee_avec_sa_famille() throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);
        String code = jeton("SF");

        poster(SOUS_FAMILLES, sousFamille(code, familleId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$." + FAMILLE).value(familleId.toString()));
    }

    @Test
    void r1_famille_codeEnDouble_409() throws Exception {
        String code = jeton("FAM");
        fixture.famille(code, categorie);

        poster(FAMILLES, famille(code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(CODE_EXISTANT)));
    }

    @Test
    void r1_sousFamille_codeEnDouble_409() throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);
        String code = jeton("SF");
        fixture.sousFamille(code, familleId, categorie);

        poster(SOUS_FAMILLES, sousFamille(code, familleId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(CODE_EXISTANT)));
    }

    @Test
    void r1_natureFournisseur_codeEnDouble_parmiLesFournisseurs_409() throws Exception {
        String code = jeton("NAT");
        fixture.nature(code, TYPE_FOURNISSEUR, 0);

        poster(NATURES_FOURNISSEUR, nature(code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(CODE_EXISTANT)));
    }

    @Test
    void r1_natureFournisseur_memeCode_qu_une_nature_client_acceptee() throws Exception {
        String code = jeton("NAT");
        fixture.nature(code, TYPE_CLIENT, 0);

        poster(NATURES_FOURNISSEUR, nature(code)).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ R2 obligatoires

    @ParameterizedTest(name = "familles sans {0}")
    @ValueSource(strings = {CODE, DESCRIPTION, CATEGORIE})
    void r2_famille_champObligatoireManquant_400(String champ) throws Exception {
        poster(FAMILLES, famille(jeton("FAM")).sans(champ)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "sous-familles sans {0}")
    @ValueSource(strings = {CODE, DESCRIPTION, CATEGORIE, FAMILLE})
    void r2_sousFamille_champObligatoireManquant_400(String champ) throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);

        poster(SOUS_FAMILLES, sousFamille(jeton("SF"), familleId).sans(champ)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R5 O.D.T. par défaut

    static Stream<Arguments> triParDefaut() {
        return Stream.of(Arguments.of(FAMILLES, 1), Arguments.of(SOUS_FAMILLES, 1), Arguments.of(NATURES_FOURNISSEUR, 0));
    }

    @ParameterizedTest(name = "{0} : O.D.T. {1}")
    @MethodSource("triParDefaut")
    void r5_sorting_parDefaut(String slug, int attendu) throws Exception {
        poster(slug, saisieValide(slug).sans(SORTING))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$." + SORTING).value(attendu));
    }

    // ------------------------------------------------------------------ D3 suppression refusée

    @Test
    void d3_famille_citeeParUneSousFamille_409() throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);
        fixture.sousFamille(jeton("SF"), familleId, categorie);

        supprimer(FAMILLES, familleId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(startsWith(SUPPRESSION_IMPOSSIBLE)))
                .andExpect(jsonPath(DETAIL).value(containsString(UTILISE_PAR_UN)));
        assertThat(fixture.compter("SELECT COUNT(*) FROM BuyItemFamily WHERE Pkey = :id", familleId)).isEqualTo(1);
    }

    @Test
    void d3_sousFamille_citeeParUnArticle_409() throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);
        UUID sousFamilleId = fixture.sousFamille(jeton("SF"), familleId, categorie);
        fixture.article(jeton("ART"), "Article", "U", sousFamilleId, categorie);

        supprimer(SOUS_FAMILLES, sousFamilleId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(startsWith(SUPPRESSION_IMPOSSIBLE)))
                .andExpect(jsonPath(DETAIL).value(containsString(UTILISE_PAR_UN)));
    }

    @Test
    void d3_natureFournisseur_citeeParUnTiers_409() throws Exception {
        UUID natureId = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);
        fixture.tiersDeNature(jeton("FOU"), natureId);

        supprimer(NATURES_FOURNISSEUR, natureId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(startsWith(SUPPRESSION_IMPOSSIBLE)))
                .andExpect(jsonPath(DETAIL).value(containsString(UTILISE_PAR_UN)));
    }

    @Test
    void d3_lignesNonCitees_supprimees_204() throws Exception {
        UUID familleId = fixture.famille(jeton("FAM"), categorie);
        UUID sousFamilleId = fixture.sousFamille(jeton("SF"), fixture.famille(jeton("FAM"), categorie), categorie);
        UUID natureId = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);

        supprimer(FAMILLES, familleId).andExpect(status().isNoContent());
        supprimer(SOUS_FAMILLES, sousFamilleId).andExpect(status().isNoContent());
        supprimer(NATURES_FOURNISSEUR, natureId).andExpect(status().isNoContent());
        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTierKind WHERE PKey = :id", natureId)).isZero();
    }

    // ------------------------------------------------------------------ D4 natures du type Supplier seulement

    @Test
    void d4_liste_natures_ne_renvoie_que_le_type_fournisseur() throws Exception {
        UUID fournisseur = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);
        UUID client = fixture.nature(jeton("NAT"), TYPE_CLIENT, 0);

        List<String> ids = ids(mockMvc.perform(get(TABLE, NATURES_FOURNISSEUR).with(admin)).andExpect(status().isOk()));

        assertThat(ids).contains(fournisseur.toString()).doesNotContain(client.toString());
        long horsFournisseur = ids.stream().map(UUID::fromString)
                .filter(id -> fixture.compter("SELECT COUNT(*) FROM CfgTierKind WHERE PKey = :id AND CfgTierTypeId = '"
                        + TYPE_FOURNISSEUR + "'", id) == 0)
                .count();
        assertThat(horsFournisseur).isZero();
    }

    @Test
    void d4_nature_creee_porte_le_type_fournisseur() throws Exception {
        String json = poster(NATURES_FOURNISSEUR, nature(jeton("NAT")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTierKind WHERE PKey = :id AND CfgTierTypeId = '"
                + TYPE_FOURNISSEUR + "'", id)).isEqualTo(1);
    }

    @Test
    void d4_nature_d_un_autre_type_non_modifiable_ici_404() throws Exception {
        UUID fournisseur = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);
        UUID client = fixture.nature(jeton("NAT"), TYPE_CLIENT, 0);
        CorpsJson renomme = corps().avec(DESCRIPTION, "Renommée");

        mockMvc.perform(patch(LIGNE, NATURES_FOURNISSEUR, fournisseur).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(renomme.json()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(LIGNE, NATURES_FOURNISSEUR, client).with(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(renomme.json()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ D6 droits OMRAN

    static Stream<Arguments> droits() {
        return Stream.of(Arguments.of(FAMILLES, DROIT_FAMILLES), Arguments.of(SOUS_FAMILLES, DROIT_SOUS_FAMILLES),
                Arguments.of(NATURES_FOURNISSEUR, DROIT_NATURES));
    }

    @ParameterizedTest(name = "{0} sans {1} : 403")
    @MethodSource("droits")
    void d6_sansLeDroit_403_en_lecture_et_en_ecriture(String slug, String droit) throws Exception {
        RequestPostProcessor sansDroit = fixture.avecDroits();

        mockMvc.perform(get(TABLE, slug).with(sansDroit)).andExpect(status().isForbidden());
        poster(sansDroit, slug, saisieValide(slug)).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} avec {1} : lecture et création")
    @MethodSource("droits")
    void d6_avecLeDroit_nonAdministrateur_lit_et_cree(String slug, String droit) throws Exception {
        RequestPostProcessor avecDroit = fixture.avecDroits(droit);

        mockMvc.perform(get(TABLE, slug).with(avecDroit)).andExpect(status().isOk());
        poster(avecDroit, slug, saisieValide(slug)).andExpect(status().isCreated());
    }
}
