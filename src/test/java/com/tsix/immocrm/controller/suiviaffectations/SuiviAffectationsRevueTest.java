package com.tsix.immocrm.controller.suiviaffectations;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.fixtures.FixtureSuiviAffectations.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue de FEATURE-suivi-affectations.md : tri §5 sur les colonnes tiers (par nom, lignes sans tiers gardées) et
 * refus R5 / §11 (sens et catégorie inconnus). Six pièces à tiers nommés F…A, insérés dans l'ordre inverse, + une
 * pièce sans tiers ni banque.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviAffectationsRevueTest {

    private static final String URL = "/api/suivi-affectations";
    private static final int NOMBRE_LIGNES = 7;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviAffectations fixture;

    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        FixtureEtatVentes ev = fixture.etatVentes();
        String prefixe = FixtureEtatVentes.prefixe("SAR");
        tranche = ev.tranche("TR-" + prefixe, ev.projet("PRJ-" + prefixe, true), true);
        ComFolder dossier = ev.vendre(ev.article(prefixe + "-A1", tranche, ev.categorie("APP-" + prefixe),
                        ev.type("T3-" + prefixe)), "DOS-" + prefixe, ev.statut(StatutDossier.RESERVATION), null, null);
        ComSettlementCategory avance = fixture.categorie("AV-" + prefixe);

        for (String lettre : List.of("F", "E", "D", "C", "B", "A")) {
            CfgTier tiers = fixture.banque(lettre + "-TIERS-" + prefixe);
            StlSettlement piece = fixture.piece("P " + lettre + prefixe, true, IN_WORK,
                    LocalDateTime.of(2025, 3, 10, 0, 0), null, BigDecimal.TEN);
            piece.setTier(tiers);
            piece.setCfgBank(tiers);
            fixture.enregistrer(piece);
            fixture.ligne(piece, fixture.echeance(dossier, avance, BigDecimal.TEN), BigDecimal.TEN);
        }
        StlSettlement sansTiers = fixture.piece("P Z" + prefixe, true, IN_WORK,
                LocalDateTime.of(2025, 3, 10, 0, 0), null, BigDecimal.ONE);
        fixture.ligne(sansTiers, fixture.echeance(dossier, avance, BigDecimal.ONE), BigDecimal.ONE);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder liste() {
        return get(URL).param("trancheId", tranche.getId().toString()).param("size", "50");
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** §5 : tiersNom est « triable » — l'ordre doit être celui des noms affichés, pas des UUID. */
    @Test
    void tri_tiersNom_parNomAffiche_lignesSansTiersGardees() throws Exception {
        String json = corps(liste().param("sort", "tiersNom,asc").param("colonnes", "id,tiersNom"));

        List<String> noms = JsonPath.read(json, "$.content[*].tiersNom");
        List<String> renseignes = noms.stream().filter(Objects::nonNull).toList();
        assertThat(noms).hasSize(NOMBRE_LIGNES);
        assertThat(renseignes).hasSize(NOMBRE_LIGNES - 1).isSorted();
    }

    @Test
    void tri_banqueNom_parNomAffiche() throws Exception {
        String json = corps(liste().param("sort", "banqueNom,desc").param("colonnes", "id,banqueNom"));

        List<String> noms = JsonPath.read(json, "$.content[*].banqueNom");
        List<String> renseignes = noms.stream().filter(Objects::nonNull).toList();
        assertThat(renseignes).hasSize(NOMBRE_LIGNES - 1)
                .isSortedAccordingTo(java.util.Comparator.<String>naturalOrder().reversed());
    }

    /** §11 : sens inconnu → 400. */
    @Test
    void sensInconnu_rend400() throws Exception {
        mockMvc.perform(liste().param("sens", "LES_DEUX")).andExpect(status().isBadRequest());
    }

    /** §11 : categorieId accepte un id ou SANS_CATEGORIE ; autre chose → 400. */
    @Test
    void categorieNiIdNiSansCategorie_rend400() throws Exception {
        mockMvc.perform(liste().param("categorieId", "AVANCE")).andExpect(status().isBadRequest());
    }
}
