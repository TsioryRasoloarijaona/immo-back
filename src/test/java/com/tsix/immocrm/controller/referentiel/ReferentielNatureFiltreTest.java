package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Paramètre {@code nature} de categories-bien / types-bien (formulaire terrain). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielNatureFiltreTest {

    private static final String CATEGORIES = "/api/referentiels/categories-bien";
    private static final String TYPES = "/api/referentiels/types-bien";
    private static final String NATURE = "nature";
    private static final String CODE_NATURE = "TER-NAT-FILTRE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private StkItemCategory categorieTerrain;
    private StkItemCategory categorieBat;
    private StkItemType typeTerrain;
    private StkItemType typeBat;

    @BeforeEach
    void setUp() {
        StkKind bat = fixture.natureBat();
        StkKind terrain = fixture.nature(UUID.randomUUID(), CODE_NATURE);
        categorieTerrain = fixture.categorie("CAT-NAT-FILTRE-TER", terrain, false);
        categorieBat = fixture.categorie("CAT-NAT-FILTRE-BAT", bat, false);
        typeTerrain = fixture.type("TYP-NAT-FILTRE-TER", terrain, false);
        typeBat = fixture.type("TYP-NAT-FILTRE-BAT", bat, false);
    }

    private static String ligne(UUID id) {
        return "$[?(@.id=='" + id + "')]";
    }

    @Test
    void categories_nature_neGardeQueLaNature() throws Exception {
        mockMvc.perform(get(CATEGORIES).param(NATURE, CODE_NATURE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(categorieTerrain.getId())).exists())
                .andExpect(jsonPath(ligne(categorieBat.getId())).doesNotExist());
    }

    @Test
    void types_nature_neGardeQueLaNature() throws Exception {
        mockMvc.perform(get(TYPES).param(NATURE, CODE_NATURE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(typeTerrain.getId())).exists())
                .andExpect(jsonPath(ligne(typeBat.getId())).doesNotExist());
    }

    @Test
    void sansNature_toutesLesReferences() throws Exception {
        mockMvc.perform(get(CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(categorieTerrain.getId())).exists())
                .andExpect(jsonPath(ligne(categorieBat.getId())).exists());
        mockMvc.perform(get(TYPES))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(typeTerrain.getId())).exists())
                .andExpect(jsonPath(ligne(typeBat.getId())).exists());
    }

    @Test
    void natureInconnue_400() throws Exception {
        mockMvc.perform(get(CATEGORIES).param(NATURE, "NAT-INCONNUE")).andExpect(status().isBadRequest());
        mockMvc.perform(get(TYPES).param(NATURE, "NAT-INCONNUE")).andExpect(status().isBadRequest());
    }
}
