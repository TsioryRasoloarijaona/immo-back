package com.tsix.immocrm.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue FEATURE-filtres-lies.md : la liaison par record garde la liaison des anciens {@code @RequestParam}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FiltresLiesRevueTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String ENCAISSEMENTS = "/api/encaissements";
    private static final String TOTAL = "$.totalElements";

    @Autowired
    private MockMvc mockMvc;

    private long total(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, TOTAL)).longValue();
    }

    @Test
    void parametresVides_valentAbsents_dossiers() throws Exception {
        long sans = total(get(DOSSIERS));
        assertThat(total(get(DOSSIERS).param("inclureSortis", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("statut", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("commercial", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("trancheId", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("dateDebut", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("champDate", ""))).isEqualTo(sans);
        assertThat(total(get(DOSSIERS).param("lignes", ""))).isEqualTo(sans);
    }

    @Test
    void parametresVides_valentAbsents_encaissements() throws Exception {
        long sans = total(get(ENCAISSEMENTS));
        assertThat(total(get(ENCAISSEMENTS).param("etat", ""))).isEqualTo(sans);
        assertThat(total(get(ENCAISSEMENTS).param("entiteId", ""))).isEqualTo(sans);
        assertThat(total(get(ENCAISSEMENTS).param("montantDe", ""))).isEqualTo(sans);
        assertThat(total(get(ENCAISSEMENTS).param("comptabilise", ""))).isEqualTo(sans);
        assertThat(total(get(ENCAISSEMENTS).param("dateFin", ""))).isEqualTo(sans);
    }

    @Test
    void inclureSortisTrue_elargitLaListe() throws Exception {
        assertThat(total(get(DOSSIERS).param("inclureSortis", "true")))
                .isGreaterThanOrEqualTo(total(get(DOSSIERS)));
    }

    @Test
    void valeurIllisible_rend400AvecLeNomDuParametre_commeAvant() throws Exception {
        mockMvc.perform(get(ENCAISSEMENTS).param("champDate", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("champDate")));
        mockMvc.perform(get(DOSSIERS).param("commercial", "pas-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("commercial")));
        mockMvc.perform(get(DOSSIERS).param("inclureSortis", "peutetre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("inclureSortis")));
    }

    /** /groupes n'avait pas de paramètre {@code lignes} : une valeur quelconque était ignorée. */
    @Test
    void groupes_lignesInconnu_ignoreCommeAvant() throws Exception {
        mockMvc.perform(get(DOSSIERS + "/groupes").param("groupBy", "statut").param("lignes", "FOO"))
                .andExpect(status().isOk());
    }

    @Test
    void groupes_lignesLibres_forceDossiers() throws Exception {
        String sans = mockMvc.perform(get(DOSSIERS + "/groupes").param("groupBy", "statut"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String avec = mockMvc.perform(get(DOSSIERS + "/groupes").param("groupBy", "statut").param("lignes", "LIBRES"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(avec).isEqualTo(sans);
    }

    /** Spring 6.2+ lie aussi les en-têtes dans un @ModelAttribute : un en-tête ne doit pas filtrer. */
    @Test
    void enTeteHomonyme_neFiltrePas() throws Exception {
        long sans = total(get(ENCAISSEMENTS));
        assertThat(total(get(ENCAISSEMENTS).header("reference", "ZZZ-AUCUNE-REFERENCE"))).isEqualTo(sans);
        long sansDossiers = total(get(DOSSIERS));
        assertThat(total(get(DOSSIERS).header("search", "ZZZ-AUCUN-DOSSIER"))).isEqualTo(sansDossiers);
    }
}
