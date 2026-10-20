package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Retrait du lot T6 « Libres / Réservés / Vendus » (FEATURE-etat-des-ventes-parite.md §1, §5.1) : routes en 404,
 * paramètres {@code famille} / {@code etat} sans effet (inconnus de Spring, jamais 400), catalogue sans ces colonnes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesRetraitVendusLibresTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String TOTAL_ELEMENTS = "$.totalElements";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;

    private CfgTranche tranche;

    /** Un libre, un réservé, un parking autonome : chaque famille et chaque état aurait filtré quelque chose. */
    @BeforeEach
    void setUp() {
        String prefixe = FixtureEtatVentes.prefixe("EDVT");
        tranche = fixture.tranche("TR-" + prefixe, fixture.projet("PRJ-" + prefixe, true), true);
        StkItemCategory appartement = fixture.categorie("APP-" + prefixe);
        StkItemType type = fixture.type("T3-" + prefixe);
        fixture.article(prefixe + "-A1", tranche, appartement, type);
        fixture.vendre(fixture.article(prefixe + "-A2", tranche, appartement, type), "DOS-" + prefixe + "-A2",
                fixture.statut(StatutDossier.RESERVATION), null, LocalDateTime.of(2025, 3, 15, 10, 0));
        fixture.parkingAutonome(prefixe + "-P1", tranche, fixture.categorie("PAR-" + prefixe),
                fixture.type("PK-" + prefixe));
        fixture.relire();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param(TRANCHE_ID, tranche.getId().toString());
    }

    private long total(MockHttpServletRequestBuilder requete, String chemin) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(corps, chemin)).longValue();
    }

    @Test
    void r1_syntheseVendusLibres_404() throws Exception {
        mockMvc.perform(get(URL + "/synthese-vendus-libres").param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void r1_exportVendusLibres_404() throws Exception {
        mockMvc.perform(get(URL + "/synthese-vendus-libres/export").param("format", "xlsx"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPARTEMENT", "PARKING", "MAGASIN", "AUTRE"})
    void r1_familleIgnoree_listeEtTotaux(String famille) throws Exception {
        long sansFiltre = total(liste(), TOTAL_ELEMENTS);

        assertThat(new long[] {
                total(liste().param("famille", famille), TOTAL_ELEMENTS),
                total(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("famille", famille), "$.nombreTotal")})
                .containsExactly(sansFiltre, sansFiltre);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIBRE", "RESERVE", "VENDU"})
    void r1_etatIgnore_listeEtTotaux(String etat) throws Exception {
        long sansFiltre = total(liste(), TOTAL_ELEMENTS);

        assertThat(new long[] {
                total(liste().param("etat", etat), TOTAL_ELEMENTS),
                total(get(URL + "/totaux-colonnes").param(TRANCHE_ID, tranche.getId().toString())
                        .param("etat", etat), "$.nombreTotal")})
                .containsExactly(sansFiltre, sansFiltre);
    }

    @Test
    void r1_catalogue_sansColonneFamilleNiEtat() throws Exception {
        String corps = mockMvc.perform(get("/api/referentiels/colonnes/etat-des-ventes"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<String> noms = JsonPath.read(corps, "$[*].nom");

        assertThat(noms).isNotEmpty().doesNotContain("famille", "etat", "etatVente");
    }
}
