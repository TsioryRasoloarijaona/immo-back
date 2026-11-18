package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_NATURES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.NATURES_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.SOUS_FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.TYPE_CLIENT;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.TYPE_FOURNISSEUR;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue N8 : PATCH article (R3 réponse 4, toolNew), portée des natures fournisseur en lecture et suppression, R7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NomenclatureAchatRevueTest {

    private static final String R3_MESSAGE = "La sous famille sélectionnée n'appartient pas à la famille sélectionnée !";
    private static final String COMPTE_NATURE = "SELECT COUNT(*) FROM CfgTierKind WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private RequestPostProcessor admin;
    private UUID categorie;
    private UUID familleA;
    private UUID sousFamilleA;
    private UUID familleB;
    private UUID sousFamilleB;
    private String codeFamilleB;
    private UUID article;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        admin = fixture.administrateur();
        categorie = fixture.categorie(jeton("CAT"));
        familleA = fixture.famille(jeton("FAM"), categorie);
        sousFamilleA = fixture.sousFamille(jeton("SF"), familleA, categorie);
        codeFamilleB = jeton("FAMB");
        familleB = fixture.famille(codeFamilleB, categorie);
        sousFamilleB = fixture.sousFamille(jeton("SF"), familleB, categorie);
        article = fixture.article(jeton("ART"), "Article revue", "U", sousFamilleA, categorie);
    }

    private ResultActions patcher(RequestPostProcessor qui, String route, CorpsJson corps, Object... ids)
            throws Exception {
        return mockMvc.perform(patch(route, ids).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    // ------------------------------------------------------------------ articles : PATCH

    @Test
    void r3_patch_familleSeule_etrangere_a_la_sousFamille_actuelle_400() throws Exception {
        patcher(admin, ARTICLE, corps().avec(FAMILLE, familleB), article)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(R3_MESSAGE));
    }

    @Test
    void r3_patch_sousFamilleSeule_derive_la_nouvelle_famille() throws Exception {
        patcher(admin, ARTICLE, corps().avec(SOUS_FAMILLE, sousFamilleB), article)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familleId").value(familleB.toString()))
                .andExpect(jsonPath("$.sousFamilleId").value(sousFamilleB.toString()));
    }

    @Test
    void d6_patch_sans_toolNew_403() throws Exception {
        patcher(fixture.avecDroits(DROIT_ARTICLES), ARTICLE, corps().avec(DESCRIPTION, "X"), article)
                .andExpect(status().isForbidden());
    }

    @Test
    void r7_recherche_casse_ignoree_sur_le_code_famille() throws Exception {
        String motif = codeFamilleB.toLowerCase(Locale.ROOT);
        mockMvc.perform(get(ARTICLES).with(admin).param("search", motif))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[?(@.id == '" + article + "')]").isEmpty());
        UUID dansB = fixture.article(jeton("ART"), "Article B", "U", sousFamilleB, categorie);
        mockMvc.perform(get(ARTICLES).with(admin).param("search", motif))
                .andExpect(jsonPath("$.articles[?(@.id == '" + dansB + "')]").isNotEmpty());
    }

    // ------------------------------------------------------------------ natures fournisseur : portée

    @Test
    void d4_nature_d_un_autre_type_ni_lue_ni_supprimee_404() throws Exception {
        UUID client = fixture.nature(jeton("NAT"), TYPE_CLIENT, 0);

        mockMvc.perform(get(LIGNE, NATURES_FOURNISSEUR, client).with(admin)).andExpect(status().isNotFound());
        mockMvc.perform(delete(LIGNE, NATURES_FOURNISSEUR, client).with(admin)).andExpect(status().isNotFound());
        assertThat(fixture.compter(COMPTE_NATURE, client)).isEqualTo(1);
    }

    @Test
    void d6_nature_patch_et_delete_sans_droit_403_et_conservee() throws Exception {
        UUID fournisseur = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);
        RequestPostProcessor sansDroit = fixture.avecDroits();

        patcher(sansDroit, LIGNE, corps().avec(DESCRIPTION, "X"), NATURES_FOURNISSEUR, fournisseur)
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(LIGNE, NATURES_FOURNISSEUR, fournisseur).with(sansDroit))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(LIGNE, NATURES_FOURNISSEUR, fournisseur).with(sansDroit))
                .andExpect(status().isForbidden());
        assertThat(fixture.compter(COMPTE_NATURE, fournisseur)).isEqualTo(1);
    }

    @Test
    void d6_nature_avec_le_droit_supprime_une_nature_non_citee_204() throws Exception {
        UUID fournisseur = fixture.nature(jeton("NAT"), TYPE_FOURNISSEUR, 0);

        mockMvc.perform(delete(LIGNE, NATURES_FOURNISSEUR, fournisseur).with(fixture.avecDroits(DROIT_NATURES)))
                .andExpect(status().isNoContent());
        assertThat(fixture.compter(COMPTE_NATURE, fournisseur)).isZero();
    }

    @Test
    void slug_inconnu_404_avant_le_droit() throws Exception {
        mockMvc.perform(get("/api/referentiels-admin/{table}", "inconnu-n8").with(fixture.avecDroits()))
                .andExpect(status().isNotFound());
    }
}
