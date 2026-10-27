package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import com.tsix.immocrm.fixtures.FixtureRapports;
import jakarta.persistence.Tuple;
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

import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_DESISTEMENT;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_1;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue adversariale du lot COM (FEATURE-vente-commissions.md §3.1 C3, C6/C8, §6). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionRevueTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final LocalDate FINALISATION = LocalDate.of(2024, 6, 1);
    private static final LocalDate ANNULATION = LocalDate.of(2025, 3, 1);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");
    private static final BigDecimal CINQ_CENTS = new BigDecimal("500");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;
    @Autowired private FixtureRapports rapports;

    private Contexte c;
    private UUID personne;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMR");
        personne = fixture.personne("P-" + c.jeton(), false);
    }

    private static BigDecimal somme(List<Tuple> lignes) {
        return lignes.stream().map(l -> decimal(l.get("CommissionAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * C3 : commercial = gestionnaire (une seule personne, deux origines 300 et 500). La reprise de la plus ancienne
     * (−300) est supprimée puis le désistement regénéré : seule la reprise manquante (−300) doit être recréée,
     * Σ désistements = −800.
     */
    @Test
    void c3_repriseSupprimeeDeLOrigineLaPlusAncienne_estRecreee_sommeEgaleMoinsOrigines() throws Exception {
        RequestPostProcessor qui = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU,
                FixtureCommissions.SUPPRIMER).qui();
        Dossier d = fixture.dossier(c, c.tranche(), "D1", fixture.statutDesiste(c), personne, PRIX, AVANCE);
        fixture.poser(d, "CancelDate", ANNULATION);
        fixture.ligne(d, CONCRETISATION, personne, TROIS_CENTS, AVANCE, null);
        fixture.ligne(d, FINALISATION_1, personne, CINQ_CENTS, FINALISATION, null);
        mockMvc.perform(generation(c.tranche().getId(), T_DESISTEMENT, List.of(d.id())).with(qui))
                .andExpect(status().isOk());
        UUID repriseDe300 = fixture.lignesDuDossier(d.id(), DESISTEMENT).stream()
                .filter(l -> decimal(l.get("CommissionAmount")).compareTo(TROIS_CENTS.negate()) == 0)
                .map(l -> UUID.fromString(guid(l.get("PKey")))).findFirst().orElseThrow();
        mockMvc.perform(suppression(List.of(repriseDe300)).with(qui)).andExpect(status().isNoContent());

        mockMvc.perform(generation(c.tranche().getId(), T_DESISTEMENT, List.of(d.id())).with(qui))
                .andExpect(status().isOk());

        List<Tuple> reprises = fixture.lignesDuDossier(d.id(), DESISTEMENT);
        assertAll(
                () -> assertThat(somme(reprises)).isEqualByComparingTo("-800"),
                () -> assertThat(reprises).extracting(l -> decimal(l.get("CommissionAmount")))
                        .usingElementComparator(BigDecimal::compareTo)
                        .containsExactlyInAnyOrder(TROIS_CENTS.negate(), CINQ_CENTS.negate()));
    }

    /**
     * C8 / C6 : un document couvre une ligne de la tranche du périmètre et une ligne d'une tranche hors périmètre.
     * Le compte restreint lit le document par sa ligne visible et l'onglet Factures : la ligne hors périmètre (id,
     * N° de dossier) ne doit pas apparaître.
     */
    @Test
    void c8_documentMultiTranche_ligneHorsPerimetre_nonExposee() throws Exception {
        Compte restreint = fixture.compte(c.jeton(), FixtureCommissions.ECRAN);
        fixture.affecter(restreint, c.projet());
        CfgTranche horsPerimetre = fixture.autreTranche(c, "HP", true);
        Dossier ici = fixture.dossier(c, "DI", personne, PRIX, AVANCE);
        Dossier ailleurs = fixture.dossier(c, horsPerimetre, "DH", personne, PRIX, AVANCE);
        UUID doc = fixture.document(personne, "DOC-" + c.jeton(), new BigDecimal("400"));
        UUID ligneIci = fixture.ligne(ici, CONCRETISATION, personne, TROIS_CENTS, AVANCE, doc);
        UUID ligneAilleurs = fixture.ligne(ailleurs, CONCRETISATION, personne, new BigDecimal("100"), AVANCE, doc);
        fixture.relire();

        ResultActions parLigne = mockMvc.perform(get(DOCUMENT_DE_LIGNE, ligneIci).with(restreint.qui()))
                .andExpect(status().isOk());
        ResultActions factures = mockMvc.perform(get(FACTURES, c.tranche().getId()).with(restreint.qui()))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat((List<String>) lire(parLigne, "$.lignes[*].ligneId")).contains(ligneIci.toString())
                        .doesNotContain(ligneAilleurs.toString()),
                () -> assertThat(parLigne.andReturn().getResponse().getContentAsString()).doesNotContain(ailleurs.code()),
                () -> assertThat(factures.andReturn().getResponse().getContentAsString())
                        .doesNotContain(ligneAilleurs.toString()).doesNotContain(ailleurs.code()));
    }

    /**
     * §6 : « Écart documenté dans l'avertissement du rapport quand un commercial a des règlements de montants
     * identiques ». Deux règlements de 100 sur le document d'un commercial → un avertissement.
     */
    @Test
    void r34_reglementsDeMontantsIdentiques_avertissement() throws Exception {
        rapports.tables();
        rapports.rapportVente("ComCommissionsByCommercial", "COMMISSIONS DE VENTE PAR COMMERCIAL", "COMMISSION", 10010);
        Dossier d = fixture.dossier(c, "DR", personne, PRIX, AVANCE);
        UUID doc = fixture.document(personne, "DOC-" + c.jeton(), TROIS_CENTS);
        fixture.ligne(d, CONCRETISATION, personne, TROIS_CENTS, LocalDate.of(2025, 1, 1), doc);
        fixture.reglement(doc, new BigDecimal("100"), "IN_WORK");
        fixture.reglement(doc, new BigDecimal("100"), "IN_WORK");
        RequestPostProcessor libre = connecte(rapports.compte("r34r." + c.jeton(),
                rapports.role("Libre R " + c.jeton(), false)));
        rapports.relire();

        ResultActions r = mockMvc.perform(get("/api/rapports/vente/ComCommissionsByCommercial/resultat")
                        .param("du", "2025-01-01").param("au", "2025-06-30")
                        .param("trancheId", c.tranche().getId().toString()).with(libre))
                .andExpect(status().isOk());

        assertThat((List<String>) lire(r, "$.avertissements")).isNotEmpty();
    }
}
