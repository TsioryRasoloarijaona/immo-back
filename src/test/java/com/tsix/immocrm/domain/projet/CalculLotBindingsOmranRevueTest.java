package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesLot.Somme;
import com.tsix.immocrm.dto.projet.ColonneCoutDto;
import com.tsix.immocrm.dto.projet.ColonneSuiviDto;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static java.math.BigDecimal.ZERO;

/**
 * Revue L2b : cases dont le binding OMRAN ({@code Yes.Prj.Front.Market/MarketMonitoring.cs}) diffère de la colonne prise par
 * {@link CalculLot}. Le spec (E3) dit « le binding du .cs fait foi » ; E2 renvoie aux bindings exacts :2118-2771.
 */
class CalculLotBindingsOmranRevueTest {

    private static CalculLot calcul(Map<Somme, BigDecimal> sommes) {
        EntreesLot.Drapeaux d = new EntreesLot.Drapeaux(false, false, false, false, false, false);
        EntreesLot.Taux t = new EntreesLot.Taux(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
        return new CalculLot(new EntreesLot(d, t, sommes));
    }

    private static Map<Somme, BigDecimal> sommes(Object... cleValeur) {
        Map<Somme, BigDecimal> m = new EnumMap<>(Somme.class);
        for (int i = 0; i < cleValeur.length; i += 2) {
            m.put((Somme) cleValeur[i], new BigDecimal((String) cleValeur[i + 1]));
        }
        return m;
    }

    // MarketMonitoring.cs:2609/2619 lient « Coût total » du coût réel à PositionTotalHT/TTC = InvoiceCostHT − RefundsTotalHT
    // (appro, fluctuations et avenants facturés compris), pas à PositionHT (« Solde HT »).
    @Test
    void coutReel_coutTotal_estPositionTotal_approFluctuationsEtAvenantsCompris() {
        CalculLot c = calcul(sommes(Somme.FAC_HT, "10000", Somme.FAC_TTC, "12000",
                Somme.DEC_FACT_STOCK_HT, "1000", Somme.DEC_FACT_STOCK, "1200",
                Somme.DEC_FACT_FLUCT_HT, "100", Somme.DEC_FACT_FLUCT, "120",
                Somme.FAV_HT, "500", Somme.FAV_TTC, "600"));

        ColonneCoutDto reel = c.coutReel();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(reel.total().ht()).as("PositionTotalHT = 10 000 + 1 000 + 100 + 500").isEqualByComparingTo("11600");
            s.assertThat(reel.total().ttc()).as("PositionTotalTTC = 12 000 + 1 200 + 120 + 600").isEqualByComparingTo("13920");
            s.assertThat(reel.netAPayer()).as("net = total TTC (sans retenue)").isEqualByComparingTo(reel.total().ttc());
        });
    }

    // MarketMonitoring.cs:3701-3751 lient la 1re case des colonnes Décomptes / Factures à PrjWorkingOutTotalLines* et
    // InvoiceTotalLines* (hors dépassements, hors travaux supp. facturés) : sinon dépassements et extras sont comptés deux fois.
    @Test
    void suivi_montantDesColonnes_horsDepassementsEtTravauxSupp_commeLeBindingOmran() {
        CalculLot c = calcul(sommes(Somme.DEC_HT, "1000", Somme.DEC_TTC, "1200", Somme.DEP_HT, "100", Somme.DEP_TTC, "120",
                Somme.FAC_HT, "1000", Somme.FAC_TTC, "1200", Somme.DEP_FACT_HT, "100", Somme.DEP_FACT_TTC, "120",
                Somme.SUPP_FACT_HT, "50", Somme.SUPP_FACT_TTC, "60"));

        ColonneSuiviDto dec = c.suivi().colonnes().decomptes();
        ColonneSuiviDto fac = c.suivi().colonnes().factures();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(dec.montantHT()).as("PrjWorkingOutTotalLinesHT = 1 000 − 100").isEqualByComparingTo("900");
            s.assertThat(dec.montantTTC()).as("PrjWorkingOutTotalLinesTTC = 1 200 − 120").isEqualByComparingTo("1080");
            s.assertThat(fac.montantHT()).as("InvoiceTotalLinesHT = 1 000 − 100 − 50").isEqualByComparingTo("850");
            s.assertThat(fac.montantTTC()).as("InvoiceTotalLinesTTC = 1 200 − 120 − 60").isEqualByComparingTo("1020");
            s.assertThat(fac.montantTTC().add(fac.approTTC()).add(fac.depassementsTTC()).add(fac.travauxSuppTTC())
                    .add(fac.fluctuationsTTC())).as("les lignes de la colonne somment au Total TTC")
                    .isEqualByComparingTo(fac.totalTTC());
        });
    }
}
