package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stk.StkItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PrixArticleTest {

    private static StkItem article(String prix, String prixHT, String plancher, String plancherHT) {
        StkItem item = new StkItem();
        item.setComSalePrice(prix != null ? new BigDecimal(prix) : null);
        item.setComSalePriceHT(prixHT != null ? new BigDecimal(prixHT) : null);
        item.setComSalePriceFloor(plancher != null ? new BigDecimal(plancher) : null);
        item.setComSalePriceFloorHT(plancherHT != null ? new BigDecimal(plancherHT) : null);
        return item;
    }

    @Test
    void prixCommercialPositif_estLaReference_memeAvecUnPlancher() {
        StkItem item = article("1200000", "1000000", "900000", "750000");

        assertThat(PrixArticle.prixDeReference(item)).hasValueSatisfying(prix -> {
            assertThat(prix.ttc()).isEqualByComparingTo("1200000");
            assertThat(prix.ht(new BigDecimal("0.20"))).isEqualByComparingTo("1000000");
            assertThat(prix.estPlancher()).isFalse();
        });
    }

    @Test
    void prixCommercialNul_plancherPositif_lePlancherEstLaReference() {
        StkItem item = article("0", "0", "900000", "750000");

        assertThat(PrixArticle.prixDeReference(item)).hasValueSatisfying(prix -> {
            assertThat(prix.ttc()).isEqualByComparingTo("900000");
            assertThat(prix.ht(new BigDecimal("0.20"))).isEqualByComparingTo("750000");
            assertThat(prix.estPlancher()).isTrue();
        });
    }

    @Test
    void prixEtPlancherNuls_aucuneReference_etZeroPourLesSousArticles() {
        StkItem item = article("0", "0", "0", "0");

        assertThat(PrixArticle.prixDeReference(item)).isEmpty();
        assertThat(PrixArticle.ttcOuZero(item)).isEqualByComparingTo("0");
    }

    @Test
    void plancherSansHT_htDeduitDeLaTva() {
        StkItem item = article("0", "0", "1200000", null);

        assertThat(PrixArticle.htOuZero(item, new BigDecimal("0.20"))).isEqualByComparingTo("1000000");
    }
}
