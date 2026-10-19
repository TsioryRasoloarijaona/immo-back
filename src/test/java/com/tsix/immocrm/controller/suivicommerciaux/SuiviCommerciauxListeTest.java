package com.tsix.immocrm.controller.suivicommerciaux;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.JOUR;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.a;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/suivi-commerciaux} (+ {@code /groupes}, {@code /totaux-colonnes}, catalogue) — FEATURE-suivi-commerciaux.md
 * §3, R9. Jeu d'un commercial propre :
 * <pre>
 *   JOUR   : P1 joint ×2, P2 joint ×1, P3 « Numéro erroné  » (non joint), visite P4   → 5 interactions
 *   JOUR+1 : P1 joint ×1
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxListeTest {

    private static final String URL = "/api/suivi-commerciaux";
    private static final String URL_SYNTHESE = "/api/suivi-commerciaux/synthese";
    private static final String COMMERCIAL = "Commercial";
    private static final String COMMERCIAL_CLE = "commercialCle";
    private static final String COLONNES = "colonnes";
    private static final String TOTAL_ELEMENTS = "$.totalElements";
    private static final int INTERACTIONS_DU_JOUR = 5;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;

    private String prefixe;
    private AdmUser commercial;
    private ComProspection p1;
    private ComProspection p3;
    private ComActionMessage numeroErrone;

    @BeforeEach
    void setUp() {
        prefixe = FixtureSuiviCommerciaux.prefixe("SCL");
        CfgTranche tranche = fixture.tranche("TR-" + prefixe, true);
        commercial = fixture.compte("scl.com." + prefixe, COMMERCIAL);
        ComActionMessageType sortant = fixture.appelSortant();
        ComActionMessageSubCategory rdv = fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS);

        p1 = fixture.prospection(tranche, prefixe + "-P1");
        ComProspection p2 = fixture.prospection(tranche, prefixe + "-P2");
        p3 = fixture.prospection(tranche, prefixe + "-P3");
        ComProspection p4 = fixture.prospection(tranche, prefixe + "-P4");
        fixture.interaction(commercial, p1, sortant, rdv, a(JOUR, 9, 0));
        fixture.interaction(commercial, p1, sortant, rdv, a(JOUR, 15, 0));
        fixture.interaction(commercial, p2, sortant, rdv, a(JOUR, 10, 0));
        numeroErrone = fixture.interaction(commercial, p3, sortant,
                fixture.resultat(FixtureSuiviCommerciaux.NUMERO_ERRONE), a(JOUR, 11, 0));
        fixture.interaction(commercial, p4, fixture.visite(), null, a(JOUR, 16, 0));
        fixture.interaction(commercial, p1, sortant, rdv, a(JOUR.plusDays(1), 9, 0));
        fixture.relire();
    }

    private MockHttpServletRequestBuilder duJour() {
        return get(URL).param(COMMERCIAL_CLE, cle(commercial)).param("jour", JOUR.toString())
                .with(connecte(commercial, COMMERCIAL));
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long totalElements(MockHttpServletRequestBuilder requete) throws Exception {
        return ((Number) JsonPath.read(corps(requete), TOTAL_ELEMENTS)).longValue();
    }

    private static String interaction(ComActionMessage m) {
        return "$.content[?(@.id=='" + m.getId() + "')]";
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void filtreCommercialEtJour() throws Exception {
        assertThat(totalElements(duJour())).isEqualTo(INTERACTIONS_DU_JOUR);
    }

    @Test
    void filtrePeriode() throws Exception {
        long total = totalElements(get(URL).param(COMMERCIAL_CLE, cle(commercial))
                .param("du", JOUR.toString()).param("au", JOUR.plusDays(1).toString())
                .with(connecte(commercial, COMMERCIAL)));

        assertThat(total).isEqualTo(INTERACTIONS_DU_JOUR + 1L);
    }

    @Test
    void filtreJoint() throws Exception {
        assertThat(totalElements(duJour().param("joint", "true"))).isEqualTo(3);
    }

    @Test
    void ligne_porteJourJointEtProspection() throws Exception {
        mockMvc.perform(duJour().param(COLONNES, "id,jour,joint,prospectionId,personneNom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(interaction(numeroErrone) + ".jour").value(JOUR.toString()))
                .andExpect(jsonPath(interaction(numeroErrone) + ".joint").value(false))
                .andExpect(jsonPath(interaction(numeroErrone) + ".prospectionId").value(p3.getId().toString()));
    }

    // ------------------------------------------------------------------ R9 — synthèse = liste

    /** Σ personnes jointes d'une cellule = personnes distinctes de la liste (auteur, jour, joint = true). */
    @Test
    void r9_celluleDeLaSynthese_egaleLaListeFiltree() throws Exception {
        String synthese = corps(get(URL_SYNTHESE).param("du", JOUR.toString()).param("au", JOUR.toString())
                .param("commercialIds", cle(commercial)).with(connecte(commercial, COMMERCIAL)));
        String cellule = "$.lignes[?(@.commercialCle=='" + cle(commercial) + "' && @.jour=='" + JOUR + "')]";
        List<Number> personnes = JsonPath.read(synthese, cellule + ".personnesJointes");
        List<Number> contacts = JsonPath.read(synthese, cellule + ".contactsJoints");

        String liste = corps(duJour().param("joint", "true").param("size", "50")
                .param(COLONNES, "id,personneNom"));
        List<String> noms = JsonPath.read(liste, "$.content[*].personneNom");
        long total = ((Number) JsonPath.read(liste, TOTAL_ELEMENTS)).longValue();

        assertThat(noms).isNotEmpty().doesNotContainNull();
        assertThat(personnes).singleElement()
                .satisfies(n -> assertThat(n.longValue()).isEqualTo(new HashSet<>(noms).size()).isEqualTo(2));
        assertThat(contacts).singleElement()
                .satisfies(n -> assertThat(n.longValue()).isEqualTo(total).isEqualTo(3));
    }

    // ------------------------------------------------------------------ pagination, tri

    @Test
    void pagination() throws Exception {
        mockMvc.perform(duJour().param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(INTERACTIONS_DU_JOUR))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void tri_jourDecroissant() throws Exception {
        mockMvc.perform(get(URL).param(COMMERCIAL_CLE, cle(commercial))
                        .param("du", JOUR.toString()).param("au", JOUR.plusDays(1).toString())
                        .param("sort", "jour,desc").param(COLONNES, "id,jour")
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jour").value(JOUR.plusDays(1).toString()));
    }

    @Test
    void tri_parId_accepte() throws Exception {
        mockMvc.perform(duJour().param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL_ELEMENTS).value(INTERACTIONS_DU_JOUR));
    }

    // ------------------------------------------------------------------ groupes, totaux, colonnes

    @Test
    void groupes_parCommercial_sommeEgaleAuTotal() throws Exception {
        String groupes = corps(get(URL + "/groupes").param(COMMERCIAL_CLE, cle(commercial))
                .param("jour", JOUR.toString()).param("groupBy", "commercialNom")
                .with(connecte(commercial, COMMERCIAL)));

        List<Integer> effectifs = JsonPath.read(groupes, "$.content[*].nombre");
        assertThat(effectifs).isNotEmpty();
        assertThat(effectifs.stream().mapToLong(Integer::longValue).sum()).isEqualTo(INTERACTIONS_DU_JOUR);
    }

    @Test
    void groupes_colonneInconnue_rend400() throws Exception {
        mockMvc.perform(get(URL + "/groupes").param("groupBy", "colonneQuiNExistePas")
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totauxColonnes_nombreTotalSurToutLeFiltre() throws Exception {
        mockMvc.perform(get(URL + "/totaux-colonnes").param(COMMERCIAL_CLE, cle(commercial))
                        .param("jour", JOUR.toString()).param("size", "2")
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(INTERACTIONS_DU_JOUR));
    }

    @Test
    void colonneInconnue_rend400() throws Exception {
        mockMvc.perform(duJour().param(COLONNES, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogueDesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/suivi-commerciaux").with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='personneNom')]").exists())
                .andExpect(jsonPath("$[?(@.nom=='joint')]").exists())
                .andExpect(jsonPath("$[?(@.nom=='commercialNom')]").exists())
                .andExpect(jsonPath("$[?(@.nom=='jour')]").exists());
    }
}
