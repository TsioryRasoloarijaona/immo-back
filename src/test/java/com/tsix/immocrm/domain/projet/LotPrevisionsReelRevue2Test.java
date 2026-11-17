package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenueStockee;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenuesStockees;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Retenues;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Seconde revue L3a-ter (FEATURE-lot-previsions-reel.md C2-C3) : net « avant » du dernier décompte réel, chiffres à la main. */
class LotPrevisionsReelRevue2Test {

    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final LocalDate DATE = LocalDate.of(2026, 3, 1);
    private static final Retenues LOT_SANS_RETENUE = new Retenues(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
    private static final RetenueStockee AUCUNE = new RetenueStockee(ZERO, ZERO, ZERO, false);

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static DecompteReel reel(String ttc, RetenueStockee avance, RetenueStockee garantie) {
        return new DecompteReel(DATE, d("0.5"), d(ttc), new RetenuesStockees(avance, garantie, AUCUNE, AUCUNE, AUCUNE));
    }

    // C3 / WorkingOut.cs l.494-499 : AdvanceAmountAccumulated est plafonné par SumAdvanceDocument dans les DEUX branches,
    // « basé sur le montant » compris. Réel TTC 1000, avance « sur montant » 300, avances datées ≤ réel = 100 :
    // avance cumulée du réel = min(300, 100) = 100 → net avant 900 ; simulé TTC 500 sans retenue : 1500 − 900 = 600.
    @Test
    void c3_avanceBaseeSurLeMontant_plafonneeParLesAvancesDateesAuDecompte() {
        DecompteSimule s = new DecompteSimule(ZERO, ZERO, LOT_SANS_RETENUE, false);
        s.precedent(reel("1000", new RetenueStockee(ZERO, d("100"), d("300"), true), AUCUNE));

        assertThat(s.net(d("500"))).isCloseTo(d("600"), within(CENTIME));
    }

    // C2, deux réels : taux de garantie 5 % puis 7 % ; le dernier réel garde SON taux sur le TTC cumulé réel
    // (WarrantyAmountAccumulated = 0,07 × 1500 = 105) → net avant 1395 ; simulé 500 au taux du lot 10 % :
    // 2000 − 200 − 1395 = 405.
    @Test
    void c2_deuxReels_tauxDuDernierSurLeCumulReel() {
        Retenues lot = new Retenues(ZERO, d("0.1"), ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
        DecompteSimule s = new DecompteSimule(ZERO, ZERO, lot, false);
        s.precedent(reel("1000", AUCUNE, new RetenueStockee(d("0.05"), ZERO, d("50"), false)));
        s.precedent(reel("500", AUCUNE, new RetenueStockee(d("0.07"), ZERO, d("55"), false)));

        assertThat(s.net(d("500"))).isCloseTo(d("405"), within(CENTIME));
    }
}
