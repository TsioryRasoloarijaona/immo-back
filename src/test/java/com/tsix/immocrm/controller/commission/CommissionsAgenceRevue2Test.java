package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DOSSIERS;
import static com.tsix.immocrm.fixtures.ApiCommissions.LIGNES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.ECRAN;
import static com.tsix.immocrm.fixtures.FixtureCommissions.IMPRIMER;
import static com.tsix.immocrm.fixtures.FixtureCommissions.NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureCommissions.SUPPRIMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue A4, 2ᵉ tour (règle 6) : changement de lien après une Concrétisation d'agence, libellé du bénéficiaire. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionsAgenceRevue2Test {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;
    @Autowired private FixtureAgences agences;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private Contexte c;
    private UUID trancheId;
    private RequestPostProcessor gestionnaire;
    private UUID vendeur;
    private UUID agenceA;
    private UUID lienA;
    private Dossier dossier;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMS");
        trancheId = c.tranche().getId();
        gestionnaire = fixture.compte(c.jeton(), ECRAN, NOUVEAU, SUPPRIMER, IMPRIMER).qui();
        vendeur = fixture.personne("VS-" + c.jeton(), false);
        fixture.regle(trancheId, "SaleCommissionBasedOnAmount", true, "SaleCommissionAmount", new BigDecimal("100"),
                "SaleCommissionExternBasedOnAmount", true, "SaleCommissionAmountExtern", new BigDecimal("1000"));
        agenceA = agences.agence("AGA-" + c.jeton());
        lienA = agences.lier(agenceA, vendeur);
        dossier = fixture.dossier(c, "DS", vendeur, PRIX, AVANCE);
    }

    private void generer() throws Exception {
        mockMvc.perform(generation(trancheId, T_CONCRETISATION, List.of(dossier.id())).with(gestionnaire))
                .andExpect(status().isOk());
    }

    private void delier() {
        em.flush();
        jdbc.update("DELETE FROM CfgTierContact WHERE PKey = ?", lienA.toString());
    }

    /** Concrétisation payée à A, puis le vendeur passe à l'agence B : B ne doit pas être payée une 2ᵉ fois. */
    @Test
    void ligneAgenceA_vendeurPasseAgenceB_aucuneSecondeConcretisation() throws Exception {
        generer();
        delier();
        agences.lier(agences.agence("AGB-" + c.jeton()), vendeur);

        generer();

        assertThat(fixture.lignesDuDossier(dossier.id(), CONCRETISATION)).hasSize(1);
    }

    /** Concrétisation payée à A, puis le vendeur quitte l'agence : le vendeur ne doit pas être payé une 2ᵉ fois. */
    @Test
    void ligneAgenceA_vendeurRevientAuSiege_aucuneSecondeConcretisation() throws Exception {
        generer();
        delier();

        generer();

        assertThat(fixture.lignesDuDossier(dossier.id(), CONCRETISATION)).hasSize(1);
    }

    /** Réponse (a) : nom du bénéficiaire d'une ligne d'agence = libellé A1b (`agence` des dossiers), même si la raison sociale diffère. */
    @Test
    void libelleBeneficiaireLigne_egalLibelleAgenceDesDossiers() throws Exception {
        em.flush();
        jdbc.update("UPDATE CfgTier SET SocialReason = N'Raison sociale distincte' WHERE PKey = ?", agenceA.toString());
        generer();

        List<Object> agence = lire(mockMvc.perform(get(DOSSIERS, trancheId).with(gestionnaire)).andExpect(status().isOk()),
                "$[?(@.dossierId == '" + dossier.id() + "')].agence");
        List<Object> beneficiaire = lire(mockMvc.perform(get(LIGNES, trancheId).with(gestionnaire)).andExpect(status().isOk()),
                "$..[?(@.dossierId == '" + dossier.id() + "')].commercial");

        assertThat(agence).hasSize(1);
        assertThat(beneficiaire).containsExactlyElementsOf(agence);
    }
}
