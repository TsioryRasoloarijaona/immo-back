package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.IMPRIMER;
import static com.tsix.immocrm.fixtures.FixtureCommissions.NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureCommissions.SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue A4 : D1 (pas de double Concrétisation) et D2 (seul un tiers agence compte) — FEATURE-agences-a4-commission.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionsAgenceRevueTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;
    @Autowired private FixtureAgences agences;

    private Contexte c;
    private UUID trancheId;
    private RequestPostProcessor gestionnaire;
    private UUID vendeur;
    private Dossier dossier;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMR");
        trancheId = c.tranche().getId();
        gestionnaire = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER).qui();
        vendeur = fixture.personne("VR-" + c.jeton(), false);
        fixture.regle(trancheId, "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("100"),
                "SaleCommissionExternBasedOnAmount", true, "SaleCommissionAmountExtern", new BigDecimal("1000"));
        dossier = fixture.dossier(c, "DR", vendeur, PRIX, AVANCE);
    }

    private ResultActions generer() throws Exception {
        return mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(dossier.id())).with(gestionnaire))
                .andExpect(status().isOk());
    }

    /** D1 : Concrétisation au nom du vendeur, puis vendeur lié à une agence → pas de 2ᵉ ligne. */
    @Test
    void d1_concretisationDuVendeur_puisLienAgence_aucuneSecondeLigne() throws Exception {
        generer();
        agences.lier(agences.agence("AGR-" + c.jeton()), vendeur);

        generer();

        assertThat(fixture.lignesDuDossier(dossier.id(), CONCRETISATION)).hasSize(1);
    }

    /** D2 : lien vers un tiers qui n'est pas une agence → ignoré, bénéficiaire = vendeur, pas de bloc Extern. */
    @Test
    void d2_lienVersTiersNonAgence_ignore() throws Exception {
        agences.lier(fixture.personne("NA-" + c.jeton(), false), vendeur);

        generer();
        ResultActions r = mockMvc.perform(get(DOSSIERS, trancheId).with(gestionnaire)).andExpect(status().isOk());

        assertThat(fixture.lignesDuDossier(dossier.id(), CONCRETISATION)).hasSize(1).allSatisfy(l -> {
            assertThat(guid(l.get("CfgEmployeeId"))).isEqualTo(guid(vendeur));
            assertThat(((Number) l.get("CommissionAmount")).intValue()).isEqualTo(100);
        });
        List<Object> beneficiaires = lire(r, "$[?(@.dossierId == '" + dossier.id() + "')].beneficiaireAgence");
        assertThat(beneficiaires).containsExactly(false);    }
}
