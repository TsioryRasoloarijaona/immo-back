package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_NATURES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_SOUS_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue N8 (règle 6) : routes D6 non couvertes par les tests verrouillés et non-régression des autres référentiels. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NomenclatureDroitsRevueTest {

    private static final String NATURES_TIERS = "natures-tiers";
    private static final String CORPS_FAMILLE = "{\"description\":\"Renommée\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private UUID famille;
    private UUID article;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        UUID categorie = fixture.categorie(jeton("CAT"));
        famille = fixture.famille(jeton("FAM"), categorie);
        UUID sousFamille = fixture.sousFamille(jeton("SF"), famille, categorie);
        article = fixture.article(jeton("ART"), "Article revue", "U", sousFamille, categorie);
    }

    @Test
    void famille_patchEtDelete_sansDroit_403_ligneIntacte() throws Exception {
        RequestPostProcessor sansDroit = fixture.avecDroits();
        mockMvc.perform(patch(LIGNE, FAMILLES, famille).with(sansDroit).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_FAMILLE)).andExpect(status().isForbidden());
        mockMvc.perform(delete(LIGNE, FAMILLES, famille).with(sansDroit)).andExpect(status().isForbidden());
        mockMvc.perform(get(LIGNE, FAMILLES, famille).with(sansDroit)).andExpect(status().isForbidden());
        assertThat(fixture.compter("SELECT COUNT(*) FROM BuyItemFamily WHERE Pkey = :id AND Description <> 'Renommée'",
                famille)).isEqualTo(1);
    }

    @Test
    void droitDUnAutreReferentiel_nOuvrePasLesFamilles() throws Exception {
        RequestPostProcessor autre = fixture.avecDroits(DROIT_SOUS_FAMILLES, DROIT_NATURES);
        mockMvc.perform(get(TABLE, FAMILLES).with(autre)).andExpect(status().isForbidden());
        mockMvc.perform(patch(LIGNE, FAMILLES, famille).with(autre).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_FAMILLE)).andExpect(status().isForbidden());
        mockMvc.perform(delete(LIGNE, FAMILLES, famille).with(autre)).andExpect(status().isForbidden());
    }

    @Test
    void referentielSansDroit_resteAdministrateurEnEcriture_lectureLibre() throws Exception {
        RequestPostProcessor acheteur = fixture.avecDroits(DROIT_FAMILLES, DROIT_SOUS_FAMILLES, DROIT_NATURES);
        mockMvc.perform(get(TABLE, NATURES_TIERS).with(acheteur)).andExpect(status().isOk());
        mockMvc.perform(post(TABLE, NATURES_TIERS).with(acheteur).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/referentiels-admin").with(acheteur))
                .andExpect(jsonPath("$[?(@.slug == '" + NATURES_TIERS + "')].peutEcrire").value(false))
                .andExpect(jsonPath("$[?(@.slug == '" + FAMILLES + "')].peutEcrire").value(true));
    }

    @Test
    void articles_lectureFicheEtSousFamilles_sansDroitListe_403() throws Exception {
        RequestPostProcessor sansListe = fixture.avecDroits(DROIT_ARTICLES_NOUVEAU);
        mockMvc.perform(get(ARTICLE, article).with(sansListe)).andExpect(status().isForbidden());
        mockMvc.perform(get(ARTICLES + "/sous-familles").with(sansListe)).andExpect(status().isForbidden());
        mockMvc.perform(patch(ARTICLE, article).with(sansListe).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"X\"}")).andExpect(status().isForbidden());
    }

    @Test
    void articles_patch_sansToolNew_403_etDrapeauxFaux() throws Exception {
        RequestPostProcessor lecteur = fixture.avecDroits(DROIT_ARTICLES);
        mockMvc.perform(patch(ARTICLE, article).with(lecteur).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"X\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get(ARTICLES).with(lecteur))
                .andExpect(jsonPath("$.peutEcrire").value(false))
                .andExpect(jsonPath("$.peutSupprimer").value(false))
                .andExpect(jsonPath("$.peutExporter").value(false));
        assertThat(fixture.compter("SELECT COUNT(*) FROM BuyItem WHERE PKey = :id AND Description = 'Article revue'",
                article)).isEqualTo(1);
    }
}
