package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Revue règle 6 de {@code PrjMarketTresorie} : {@code OfferedAmountToPay} (PrjMarketMonitoringView l.1612) part de
 * {@code TT.OfferedPriceTTC} = {@code YY.MarketLinesTotalTTC} (l.2179) ; la bascule « annulé/résilié = facturé − avoirs »
 * n'existe que sur la colonne de tête {@code OfferedPriceTTC} (l.132), pas sur {@code DueAmountToPay}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetEtatMarchesRevueTest {

    private static final String ETAT = "PrjMarketTresorie";
    private static final String ZERO = "0";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureBilansProjet bilans;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private AppelsRapports appels;
    private Cadre c;
    private RapportsProjetBilanSupport s;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
        rapports.rapport(FixtureRapportsProjet.TYPE_PROJET, ETAT, "BILAN", "PROJET", 7);
        rapports.relire();
        s = new RapportsProjetBilanSupport(c, achat, fixture, parite, bilans, lots, jdbc, em);
        maj("""
                IF COL_LENGTH('dbo.PrjMarket', 'InsuranceDECAmount') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceDECAmount decimal(26, 6) NULL;
                IF COL_LENGTH('dbo.PrjMarket', 'InsuranceTRCAmount') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceTRCAmount decimal(26, 6) NULL;
                IF COL_LENGTH('dbo.PrjMarket', 'WarrantyRatio') IS NULL ALTER TABLE dbo.PrjMarket ADD WarrantyRatio decimal(26, 6) NULL;
                IF COL_LENGTH('dbo.PrjMarket', 'ApplyWithholdingTaxOnAdditionalWork') IS NULL
                    ALTER TABLE dbo.PrjMarket ADD ApplyWithholdingTaxOnAdditionalWork bit NULL;""");
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    /**
     * Lot résilié, bordereau 10 × 100 HT = 1200 TTC, facture de 600 TTC non réglée, aucune retenue. Vue : marche (tête) =
     * facturé − avoirs = 600 ; DueAmountToPay = OfferedAmountToPay = MarketLinesTotalTTC = 1200 → reste 1200.
     */
    @Test
    void lotResilie_resteAPayer_partDuBordereau_pasDuFacture() throws Exception {
        UUID lot = s.lotResilie(c.pa1(), "RV");
        UUID type = achat.typeLot(c.code("TRV"), s.libelle("TRV"));
        maj("UPDATE PrjMarket SET PrjMarketTypeId = ?, StlCategoryId = ?, ProportionRatio = 0, InsuranceDECRatio = 0, "
                        + "InsuranceDECAmount = 0, InsuranceTRCRatio = 0, InsuranceTRCAmount = 0, WarrantyRatio = 0, "
                        + "ApplyWithholdingTaxOnAdditionalWork = 0 WHERE PKey = ?",
                type.toString(), s.categorie("CRV", null, 1).toString(), lot.toString());
        decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
        s.piece(lot, c.pa1(), "RVF", "600", "600", true);

        Map<String, Object> section = premiere(appels.sections(requete(ETAT, c.compte(), c.pa1())));
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, "typeLot", s.libelle("TRV"));
        assertThat(l).hasSize(1);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(montant(l.getFirst(), "marche")).as("marche (tête de vue)")
                .isCloseTo(new BigDecimal("600"), within(PRECISION));
        sa.assertThat(montant(l.getFirst(), "resteAPayer")).as("resteAPayer (DueAmountToPay − payé)")
                .isCloseTo(new BigDecimal("1200"), within(PRECISION));
        sa.assertThat(montant(l.getFirst(), "paye")).as("paye").isCloseTo(new BigDecimal(ZERO), within(PRECISION));
        sa.assertAll();
    }
}
