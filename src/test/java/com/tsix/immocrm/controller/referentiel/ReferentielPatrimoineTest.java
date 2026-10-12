package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §6 et §11. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielPatrimoineTest {

    private static final String CODES = "$[*].code";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private StkItemCategory categorieArticle;
    private StkItemCategory categorieSousArticle;
    private StkItemCategory categorieTerrain;
    private StkItemType typeArticle;
    private StkItemType typeSousArticle;

    @BeforeEach
    void setUp() {
        StkKind batiment = fixture.natureBatiment();
        StkKind terrain = fixture.nature(UUID.randomUUID(), "TER-REF-PAT");
        categorieArticle = fixture.categorie("CAT-REF-APP", batiment, false);
        categorieSousArticle = fixture.categorie("CAT-REF-PAR", batiment, true);
        categorieTerrain = fixture.categorie("CAT-REF-TER", terrain, false);
        typeArticle = fixture.type("TYP-REF-T3", batiment, false);
        typeSousArticle = fixture.type("TYP-REF-PRIVE", batiment, true);
    }

    @Test
    void immeubles_triesParOrdreDAffichage() throws Exception {
        fixture.immeuble("IMM-REF-B", 2);
        fixture.immeuble("IMM-REF-A", 1);

        mockMvc.perform(get("/api/referentiels/immeubles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code =~ /IMM-REF-.*/)].code", contains("IMM-REF-A", "IMM-REF-B")));
    }

    @Test
    void categories_pourArticle_natureBatimentHorsSousArticles() throws Exception {
        mockMvc.perform(get("/api/referentiels/categories-bien").param("pourSousArticle", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(categorieArticle.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(categorieSousArticle.getCode()))))
                .andExpect(jsonPath(CODES, not(hasItem(categorieTerrain.getCode()))));
    }

    @Test
    void categories_pourSousArticle_seulementForUnderItem() throws Exception {
        mockMvc.perform(get("/api/referentiels/categories-bien").param("pourSousArticle", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(categorieSousArticle.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(categorieArticle.getCode()))));
    }

    @Test
    void categories_sansParametre_inchangees() throws Exception {
        mockMvc.perform(get("/api/referentiels/categories-bien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItems(categorieArticle.getCode(), categorieSousArticle.getCode(),
                        categorieTerrain.getCode())));
    }

    @Test
    void types_filtresCommeLesCategories() throws Exception {
        mockMvc.perform(get("/api/referentiels/types-bien").param("pourSousArticle", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(typeSousArticle.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(typeArticle.getCode()))));
        mockMvc.perform(get("/api/referentiels/types-bien"))
                .andExpect(jsonPath(CODES, hasItems(typeSousArticle.getCode(), typeArticle.getCode())));
    }

    @Test
    void vocations_pourArticle_sansProjet() throws Exception {
        StkVocation vente = fixture.vocation("VENTE");
        StkVocation projet = fixture.vocation("PROJET");

        mockMvc.perform(get("/api/referentiels/vocations").param("pourArticle", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(vente.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(projet.getCode()))));
        mockMvc.perform(get("/api/referentiels/vocations"))
                .andExpect(jsonPath(CODES, hasItems(vente.getCode(), projet.getCode())));
    }
}
