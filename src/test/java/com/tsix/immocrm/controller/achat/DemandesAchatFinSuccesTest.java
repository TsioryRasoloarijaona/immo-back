package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6b R20 : transition Dépouillement → Clôturé (Fin succès) d'une DIA à lot non multi = génération du bordereau définitif du lot
 * ({@code GenerateLinesForMarket} l.2251) ; FEATURE-n6-demandes-achat.md. Le 409 N6a « … non disponible » n'est plus une réponse admise.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatFinSuccesTest {

    private static final String SANS_OFFRE = "Aucune offre n'est sélectionnée !";
    private static final String CODE_INTROUVABLE = "Il y a une erreur au niveaux du bordereau de dépouillement !";
    private static final String N6A_NON_DISPONIBLE = "non disponible";
    private static final String GROUPES_LOT = "SELECT * FROM PrjMarketLineGrouping WHERE PrjMarketId = ? ORDER BY Sorting ASC, Code ASC";
    private static final String LIGNES_LOT = "SELECT * FROM PrjMarketLine WHERE PrjMarketId = ? ORDER BY Code ASC";
    private static final String LIGNE_DIA = "SELECT UnitPriceHT FROM BuySelectionLine WHERE PKey = ?";
    private static final String CODE = "Code";
    private static final String PU_HT = "UnitPriceHT";
    private static final LocalDate DATE_AO = LocalDate.of(2026, 4, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;
    private UUID cloturer;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("FS");
        qLibre = qui(b.libre(c, "n6b.fs.libre." + c.jeton()));
        cloturer = demandes.transition(c.entite(), c.depouil(), c.clot(), "Clôturer-" + c.jeton(), 1);
    }

    /** DIA en Dépouillement sur un lot (bordereau définitif ancien « OLD ») ; bordereau DIA G1 ▸ (A1, G2 ▸ A2) ; offre de T. */
    private record Scenario(UUID dia, UUID lot, UUID tiers, UUID offre, UUID g1, UUID g2, UUID a1, UUID a2, UUID ancienneLigne,
                            UUID reglement) {
    }

    private Scenario scenario(String suffixe, boolean multi, boolean offreSelectionnee) {
        UUID dia = demandes.demande(c, c.code(suffixe), c.depouil());
        UUID lot = b.lotDe(c, dia, "L" + suffixe + "-" + c.jeton(), multi);
        b.groupeDefinitif(lot, "OLDG-" + suffixe);
        UUID ancienne = b.ligneDefinitive(lot, "OLD-" + suffixe);
        UUID categorie = achat.categorie("CAT" + suffixe + "-" + c.jeton());
        UUID g1 = b.groupe(dia, "G1", null, 1);
        UUID g2 = b.groupe(dia, "G2", g1, 2);
        UUID a1 = b.ligne(dia, g1, 1, "A1", "10", categorie);
        UUID a2 = b.ligne(dia, g2, 2, "A2", "20", categorie);
        UUID t = achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe + " " + c.jeton());
        UUID reglement = achat.typeReglement("REG" + suffixe + "-" + c.jeton());
        demandes.poser(DEMANDE, dia, "CfgTierId", t);
        demandes.poser(DEMANDE, dia, "PaymentDelay", 40);
        demandes.poser(DEMANDE, dia, "StlSettlementTypeId", reglement);
        demandes.poser(DEMANDE, dia, "TenderDate", Timestamp.valueOf(DATE_AO.atStartOfDay()));
        UUID offre = b.offre(dia, t, c.code("O" + suffixe), offreSelectionnee);
        b.ligneOffre(offre, 1, "A1", "200", "0.1");
        return new Scenario(dia, lot, t, offre, g1, g2, a1, a2, ancienne, reglement);
    }

    private Scenario scenarioComplet(String suffixe, boolean multi) {
        Scenario s = scenario(suffixe, multi, true);
        b.ligneOffre(s.offre(), 2, "A2", "50", "0");
        return s;
    }

    private static String maj(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase();
    }

    private String statutStocke(UUID dia) {
        return maj(b.lignesApresRefus("SELECT StrStatusId FROM BuySelection WHERE PKey = ?", dia.toString()).getFirst()
                .get("StrStatusId"));
    }

    private List<Object> codesLignesLotApresRefus(UUID lot) {
        return b.lignesApresRefus(LIGNES_LOT, lot.toString()).stream().map(l -> l.get(CODE)).toList();
    }

    // ------------------------------------------------------------------ refus

    @Test
    void r20_sansOffreSelectionnee_409_message_statutEtLotInchanges() throws Exception {
        Scenario s = scenario("SO", false, false);
        b.ligneOffre(s.offre(), 2, "A2", "50", "0");

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.valeur(DETAIL)).isEqualTo(SANS_OFFRE);
            a.assertThat(statutStocke(s.dia())).isEqualTo(maj(c.depouil()));
            a.assertThat(codesLignesLotApresRefus(s.lot())).containsExactly("OLD-SO");
        });
    }

    @Test
    void r20_codeDuBordereauAbsentDeLOffre_409_message_rienNEstEcrit() throws Exception {
        Scenario s = scenario("CA", false, true);

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(r.valeur(DETAIL)).isEqualTo(CODE_INTROUVABLE);
            a.assertThat(statutStocke(s.dia())).isEqualTo(maj(c.depouil()));
            a.assertThat(codesLignesLotApresRefus(s.lot())).containsExactly("OLD-CA");
            a.assertThat(b.lignesApresRefus(LIGNE_DIA, s.a1().toString()).getFirst().get(PU_HT))
                    .satisfies(pu -> assertThat((BigDecimal) pu).isEqualByComparingTo("10"));
        });
    }

    @Test
    void r20_lignesDefinitivesCiteesParUnDecompte_409_avantLaBase_temoinSansDecompte_200() throws Exception {
        Scenario cite = scenarioComplet("DC", false);
        b.decompteCitant(cite.lot(), cite.ancienneLigne(), "DC-" + c.jeton());
        Scenario libre = scenarioComplet("DT", false);

        Reponse refus = appels.executer(qLibre, cite.dia(), cloturer);
        String statutRefus = statutStocke(cite.dia());
        List<Object> lignesRefus = codesLignesLotApresRefus(cite.lot());
        Reponse temoin = appels.executer(qLibre, libre.dia(), cloturer);

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            a.assertThat(refus.corps()).doesNotContain(N6A_NON_DISPONIBLE);
            a.assertThat(statutRefus).isEqualTo(maj(c.depouil()));
            a.assertThat(lignesRefus).containsExactly("OLD-DC");
            a.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ génération

    @Test
    void r20_remplaceLeBordereauDefinitif_groupesAuxPKeyDeLaDia_parentConserve() throws Exception {
        Scenario s = scenarioComplet("GR", false);

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> groupes = b.lignes(GROUPES_LOT, s.lot().toString());
        assertThat(groupes).extracting(g -> maj(g.get("PKey"))).containsExactlyInAnyOrder(maj(s.g1()), maj(s.g2()));
        Map<String, Object> g2 = groupes.stream().filter(g -> maj(g.get("PKey")).equals(maj(s.g2()))).findFirst().orElseThrow();
        assertThat(maj(g2.get("ParentId"))).isEqualTo(maj(s.g1()));
    }

    @Test
    void r20_lignesDuLot_recreeesDepuisLaDia_puHTApresRemiseDeLOffre() throws Exception {
        Scenario s = scenarioComplet("LI", false);

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> lignes = b.lignes(LIGNES_LOT, s.lot().toString());
        assertThat(lignes).extracting(l -> l.get(CODE)).containsExactly("A1", "A2");
        SoftAssertions.assertSoftly(a -> {
            a.assertThat((BigDecimal) lignes.get(0).get(PU_HT)).as("200 × (1 − 0,1)").isEqualByComparingTo("180");
            a.assertThat((BigDecimal) lignes.get(1).get(PU_HT)).isEqualByComparingTo("50");
            a.assertThat(maj(lignes.get(0).get("PrjMarketLineGroupingId"))).isEqualTo(maj(s.g1()));
            a.assertThat(maj(lignes.get(1).get("PrjMarketLineGroupingId"))).isEqualTo(maj(s.g2()));
            a.assertThat((BigDecimal) lignes.get(1).get("Quantity")).isEqualByComparingTo("2");
            a.assertThat((BigDecimal) b.lignes(LIGNE_DIA, s.a1().toString()).getFirst().get(PU_HT)).as("ligne DIA aussi")
                    .isEqualByComparingTo("180");
            a.assertThat((BigDecimal) b.lignes(LIGNE_DIA, s.a2().toString()).getFirst().get(PU_HT)).isEqualByComparingTo("50");
        });
    }

    @Test
    void r20_lot_fournisseurPaiementDelaiEtDatesDeLaDia_statutClos() throws Exception {
        Scenario s = scenarioComplet("EN", false);

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> lot = b.lignes("SELECT * FROM PrjMarket WHERE PKey = ?", s.lot().toString()).getFirst();
        SoftAssertions.assertSoftly(a -> {
            a.assertThat(maj(lot.get("CfgSupplierId"))).isEqualTo(maj(s.tiers()));
            a.assertThat(maj(lot.get("StlSettlementTypeId"))).isEqualTo(maj(s.reglement()));
            a.assertThat(lot).containsEntry("PaymentDelay", 40);
            a.assertThat(((Timestamp) lot.get("TenderDate")).toLocalDateTime().toLocalDate()).isEqualTo(DATE_AO);
            a.assertThat(maj(demandes.demandeEnBase(s.dia()).get("StrStatusId"))).isEqualTo(maj(c.clot()));
        });
    }

    @Test
    void r20_lotMulti_200_bordereauDuLotNonEcrit() throws Exception {
        Scenario s = scenarioComplet("MU", true);

        Reponse r = appels.executer(qLibre, s.dia(), cloturer);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(b.lignes(LIGNES_LOT, s.lot().toString())).extracting(l -> l.get(CODE)).containsExactly("OLD-MU");
    }
}
