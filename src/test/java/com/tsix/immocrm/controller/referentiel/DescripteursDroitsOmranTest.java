package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_FAMILLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLES;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Droits d'écriture exposés par le descripteur : même règle que le chemin d'écriture (FEATURE-n8-nomenclature.md D6). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DescripteursDroitsOmranTest {

    private static final String FAMILLES_ACHAT = "$[?(@.slug == '" + FAMILLES + "')]";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    @BeforeEach
    void setUp() {
        fixture.preparer();
    }

    private ResultActions descripteurs(RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get("/api/referentiels-admin").with(qui)).andExpect(status().isOk());
    }

    @Test
    void sansLeDroit_peutEcrireEtPeutSupprimerFaux() throws Exception {
        descripteurs(fixture.avecDroits())
                .andExpect(jsonPath(FAMILLES_ACHAT + ".peutEcrire").value(false))
                .andExpect(jsonPath(FAMILLES_ACHAT + ".peutSupprimer").value(false));
    }

    @Test
    void avecLeDroit_peutEcrireEtPeutSupprimerVrais() throws Exception {
        descripteurs(fixture.avecDroits(DROIT_FAMILLES))
                .andExpect(jsonPath(FAMILLES_ACHAT + ".peutEcrire").value(true))
                .andExpect(jsonPath(FAMILLES_ACHAT + ".peutSupprimer").value(true));
    }
}
