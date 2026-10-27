package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.TRANCHES;
import static com.tsix.immocrm.fixtures.ApiCommissions.document;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.montant;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.ApiCommissions.tranche;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.IMPRIMER;
import static com.tsix.immocrm.fixtures.FixtureCommissions.NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureCommissions.SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilité (règle 6) — FEATURE-vente-commissions.md §3.1 C8 : toute lecture ou écriture passe par la tranche visible
 * (société active) et le périmètre commercial ; tranche, dossier ou ligne hors périmètre → 404, jamais 403 ; §8.1
 * « tranche d'une société inactive → absente de la liste et 404 », « périmètre commercial restreint », « ligneId d'une
 * tranche invisible → 404 ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionVisibiliteTest {

    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");
    private static final String TRANCHE_IDS = "$[*].trancheId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private UUID commercial;
    private Compte gestionnaire;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMV");
        commercial = fixture.personne("COM-" + c.jeton(), false);
        gestionnaire = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER);
    }

    private List<String> tranchesVisibles(RequestPostProcessor qui) throws Exception {
        return lire(mockMvc.perform(get(TRANCHES).with(qui)).andExpect(status().isOk()), TRANCHE_IDS);
    }

    /** Lectures et écritures portant sur la tranche, un dossier et une ligne de {@code cachee}. */
    private Map<String, MockHttpServletRequestBuilder> routesDe(CfgTranche cachee) {
        fixture.regleMontant(cachee.getId(), new BigDecimal("100"));
        Dossier d = fixture.dossier(c, cachee, "DC", commercial, new BigDecimal("775000"), JOUR);
        UUID ligne = fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, JOUR, null);
        Map<String, MockHttpServletRequestBuilder> m = new LinkedHashMap<>();
        m.put("tranche", tranche(cachee.getId()));
        m.put("dossiers", get(DOSSIERS, cachee.getId()));
        m.put("lignes", get(LIGNES, cachee.getId()));
        m.put("factures", get(FACTURES, cachee.getId()));
        m.put("generation", generation(cachee.getId(), T_CONCRETISATION, List.of(d.id())));
        m.put("montant", montant(ligne, new BigDecimal("250")));
        m.put("suppression", suppression(List.of(ligne)));
        m.put("document a payer", document(List.of(ligne)));
        m.put("document de la ligne", get(DOCUMENT_DE_LIGNE, ligne));
        return m;
    }

    private void verifier404(Map<String, MockHttpServletRequestBuilder> routes, RequestPostProcessor qui)
            throws Exception {
        SoftAssertions soft = new SoftAssertions();
        for (Map.Entry<String, MockHttpServletRequestBuilder> e : routes.entrySet()) {
            soft.assertThat(mockMvc.perform(e.getValue().with(qui)).andReturn().getResponse().getStatus())
                    .as(e.getKey()).isEqualTo(404);
        }
        soft.assertAll();
    }

    private static List<String> lignesDe(ResultActions r) throws Exception {
        return lire(r, "$[*].id");
    }

    // ------------------------------------------------------------------ société inactive

    @Test
    void c8_trancheDUneSocieteInactive_absenteDeLaListe() throws Exception {
        CfgTranche inactive = fixture.autreTranche(c, "INA", false);

        assertThat(tranchesVisibles(gestionnaire.qui())).contains(c.tranche().getId().toString())
                .doesNotContain(inactive.getId().toString());
    }

    @Test
    void c8_trancheDUneSocieteInactive_404PartoutJamais403_rienEcrit() throws Exception {
        CfgTranche inactive = fixture.autreTranche(c, "INA", false);
        Map<String, MockHttpServletRequestBuilder> routes = routesDe(inactive);

        verifier404(routes, gestionnaire.qui());

        assertThat(fixture.lignes("SELECT * FROM ComCommissionLine WHERE CfgTrancheId = :id", inactive.getId()))
                .singleElement()
                .satisfies(l -> assertAll(
                        () -> assertThat(decimal(l.get("CommissionAmount"))).isEqualByComparingTo(TROIS_CENTS),
                        () -> assertThat(l.get("BuyDocumentId")).isNull()));
    }

    // ------------------------------------------------------------------ périmètre commercial

    @Test
    void c8_perimetreRestreint_trancheHorsPerimetre_absenteEt404() throws Exception {
        fixture.affecter(gestionnaire, c.projet());
        CfgTranche horsPerimetre = fixture.autreTranche(c, "HP", true);
        Map<String, MockHttpServletRequestBuilder> routes = routesDe(horsPerimetre);

        assertThat(tranchesVisibles(gestionnaire.qui())).contains(c.tranche().getId().toString())
                .doesNotContain(horsPerimetre.getId().toString());
        verifier404(routes, gestionnaire.qui());
        assertThat(fixture.lignes("SELECT * FROM ComCommissionLine WHERE CfgTrancheId = :id", horsPerimetre.getId()))
                .hasSize(1);
    }

    @Test
    void c8_perimetreRestreint_trancheDuPerimetre_200() throws Exception {
        fixture.affecter(gestionnaire, c.projet());

        mockMvc.perform(tranche(c.tranche().getId()).with(gestionnaire.qui())).andExpect(status().isOk());
    }

    /** Les lignes d'une tranche visible ne contiennent pas celles d'une autre tranche. */
    @Test
    void c8_lignesDeLaTranche_seulementLesSiennes() throws Exception {
        Dossier ici = fixture.dossier(c, "DI", commercial, new BigDecimal("775000"), JOUR);
        UUID ligneIci = fixture.ligne(ici, CONCRETISATION, commercial, TROIS_CENTS, JOUR, null);
        CfgTranche autre = fixture.autreTranche(c, "AU", true);
        Dossier ailleurs = fixture.dossier(c, autre, "DA", commercial, new BigDecimal("775000"), JOUR);
        UUID ligneAilleurs = fixture.ligne(ailleurs, CONCRETISATION, commercial, TROIS_CENTS, JOUR, null);

        ResultActions r = mockMvc.perform(get(LIGNES, c.tranche().getId()).with(gestionnaire.qui()))
                .andExpect(status().isOk());

        assertThat(lignesDe(r)).contains(ligneIci.toString()).doesNotContain(ligneAilleurs.toString());
    }

    @Test
    void c8_trancheInconnue_404() throws Exception {
        mockMvc.perform(tranche(UUID.randomUUID()).with(gestionnaire.qui())).andExpect(status().isNotFound());
    }
}
