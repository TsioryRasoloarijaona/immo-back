package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_PRM_FOLDER_SALE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RACINE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEUILS_VERSEMENT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPES_PRESENTATION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Compléments au patron {@code /api/referentiels-admin} (FEATURE-patrimoine-referentiels.md §1, §8 bis) : contenances
 * imbriquées d'une présentation, listes de choix, champ inconnu, filtre global / tranche des seuils.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminComplementsTest {

    private static final String CONTENANCES = "$.contenances";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
        tranche = patrimoine.tranche("TC-" + jeton, patrimoine.projet("PRJ-TC-" + jeton,
                patrimoine.entite("EJ-TC-" + jeton, true)));
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, String json) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String contenance(UUID id, int nombre) {
        return "{\"contenanceId\":\"%s\",\"nombre\":%d,\"option1\":\"Vue mer\",\"avecOption1\":true}".formatted(id, nombre);
    }

    @Test
    void presentation_contenancesImbriquees_creeesPuisRemplaceesAuPatch() throws Exception {
        UUID ct1 = fixture.contenance("CT1-" + jeton);
        UUID ct2 = fixture.contenance("CT2-" + jeton);
        String corps = "{\"code\":\"PRC-%s\",\"description\":\"x\",\"trancheId\":\"%s\",\"contenances\":[%s,%s]}"
                .formatted(jeton, tranche.getId(), contenance(ct1, 2), contenance(ct2, 1));

        String json = ecrire(post(TABLE, TYPES_PRESENTATION), corps).andExpect(status().isCreated())
                .andExpect(jsonPath(CONTENANCES + ".length()").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        ecrire(patch(LIGNE, TYPES_PRESENTATION, id), "{\"contenances\":[%s]}".formatted(contenance(ct2, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CONTENANCES + ".length()").value(1))
                .andExpect(jsonPath(CONTENANCES + "[0].contenanceId").value(ct2.toString()))
                .andExpect(jsonPath(CONTENANCES + "[0].nombre").value(3));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationTypeContenance WHERE StkPresentationTypeId = :id",
                id)).isEqualTo(1);
    }

    @Test
    void presentation_contenanceSansContenanceId_400_rienEcrit() throws Exception {
        String corps = "{\"code\":\"PRX-%s\",\"description\":\"x\",\"trancheId\":\"%s\",\"contenances\":[{\"nombre\":2}]}"
                .formatted(jeton, tranche.getId());

        ecrire(post(TABLE, TYPES_PRESENTATION), corps).andExpect(status().isBadRequest());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationType WHERE StkHierarchyId = :id", tranche.getId()))
                .isZero();
    }

    @Test
    void champInconnu_400() throws Exception {
        ecrire(post(TABLE, "orientations"), "{\"code\":\"X-%s\",\"description\":\"x\",\"sorting\":1,\"couleur\":\"Gold\"}"
                .formatted(jeton)).andExpect(status().isBadRequest());
    }

    @Test
    void choix_statutsVente_seulementLesStatutsDesDossiersDeVente() throws Exception {
        fixture.parametrageSeuils();
        UUID vente = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-V-" + jeton);
        UUID prm = fixture.statut(ENTITE_PRM_FOLDER_SALE, "TST-P-" + jeton);

        mockMvc.perform(get(RACINE + "/choix/statuts-vente").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + vente + "')]").exists())
                .andExpect(jsonPath("$[?(@.id=='" + prm + "')]").doesNotExist());
    }

    @Test
    void choix_listeInconnue_404() throws Exception {
        mockMvc.perform(get(RACINE + "/choix/inconnue").with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void descripteurs_presentation_champsDesContenances() throws Exception {
        mockMvc.perform(get(RACINE).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='types-presentation')].champs[?(@.nom=='contenances')].champs[*].nom")
                        .value(hasItems("contenanceId", "nombre", "quantiteOption3")))
                .andExpect(jsonPath("$[?(@.slug=='seuils-versement')].champs[?(@.nom=='operateur')].options[*].valeur")
                        .value(contains(0, 1, 2)));
    }

    @Test
    void seuils_filtreGlobalEtFiltreTranche() throws Exception {
        fixture.parametrageSeuils();
        UUID statut = fixture.statut(ENTITE_DOSSIER_VENTE, "TST-F-" + jeton);
        BigDecimal ratio = new BigDecimal("0.4");
        UUID global = fixture.seuilGlobal(ENTITE_DOSSIER_VENTE, statut, 1, ratio);
        UUID deTranche = fixture.seuilDeTranche(tranche.getId(), statut, 1, ratio);

        List<Map<String, Object>> globaux = JsonPath.read(mockMvc.perform(get(TABLE, SEUILS_VERSEMENT).with(connecte(admin))
                .param("global", "true")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$");
        List<Map<String, Object>> surcharges = JsonPath.read(mockMvc.perform(get(TABLE, SEUILS_VERSEMENT)
                        .with(connecte(admin)).param("trancheId", tranche.getId().toString())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$");

        assertThat(globaux).extracting(l -> l.get("id")).contains(global.toString()).doesNotContain(deTranche.toString());
        assertThat(surcharges).extracting(l -> l.get("id")).containsExactly(deTranche.toString());
    }
}
