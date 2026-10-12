package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stk.StkItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** FEATURE-patrimoine-ecriture.md §5.5 : formules d'OMRAN, échelle 6, HALF_UP. */
class CalculPrixArticleTest {

    private static StkItem article(String surfaceProv, String surfaceDef, String m2ProvHT, String m2DefHT, String tva) {
        StkItem item = new StkItem();
        item.setAreaFloor(new BigDecimal(surfaceProv));
        item.setArea(new BigDecimal(surfaceDef));
        item.setComSaleUnitCostFloorHT(new BigDecimal(m2ProvHT));
        item.setComSaleUnitCostHT(new BigDecimal(m2DefHT));
        item.setVat(new BigDecimal(tva));
        CalculPrixArticle.appliquerPrixArticle(item);
        return item;
    }

    @Test
    void art344_prixDuM2TtcArrondiAvantLaSurface() {
        StkItem item = article("63", "63", "10833.333333", "10833.333333", "0.20");
        CalculPrixArticle.appliquerPrixDeVente(item, List.of());

        assertAll(
                () -> assertThat(item.getComSaleUnitCost()).isEqualByComparingTo("13000"),
                () -> assertThat(item.getComSaleUnitCostFloor()).isEqualByComparingTo("13000"),
                () -> assertThat(item.getSalePrice()).isEqualByComparingTo("819000"),
                () -> assertThat(item.getSalePriceFloor()).isEqualByComparingTo("819000"),
                () -> assertThat(item.getComSalePrice()).isEqualByComparingTo("819000"),
                () -> assertThat(item.getSalePriceHT()).isEqualByComparingTo("682499.999979"),
                () -> assertThat(item.getComSalePriceHT()).isEqualByComparingTo("682499.999979"));
    }

    @Test
    void echelleSixEtArrondiHalfUp() {
        StkItem item = article("1", "1", "0.0000005", "0.0000004", "0");

        assertAll(
                () -> assertThat(item.getComSaleUnitCostFloor()).isEqualTo(new BigDecimal("0.000001")),
                () -> assertThat(item.getComSaleUnitCost()).isEqualTo(new BigDecimal("0.000000")),
                () -> assertThat(item.getSalePrice().scale()).isEqualTo(CalculPrixArticle.ECHELLE));
    }

    @Test
    void tvaNulle_ttcEgalHt_etSurfacesDistinctes() {
        StkItem item = article("60", "65", "10000", "11000", "0");

        assertAll(
                () -> assertThat(item.getComSaleUnitCost()).isEqualByComparingTo("11000"),
                () -> assertThat(item.getSalePriceFloorHT()).isEqualByComparingTo("600000"),
                () -> assertThat(item.getSalePriceFloor()).isEqualByComparingTo("600000"),
                () -> assertThat(item.getSalePriceHT()).isEqualByComparingTo("715000"),
                () -> assertThat(item.getSalePrice()).isEqualByComparingTo("715000"));
    }

    @Test
    void prixDeVente_ajouteLesPrixArticleDesSousArticles() {
        StkItem appartement = article("63", "63", "10000", "10000", "0.20");
        StkItem parking = article("12", "12", "1000", "2000", "0.20");
        StkItem jardin = article("20", "20", "500", "500", "0");

        CalculPrixArticle.appliquerPrixDeVente(appartement, List.of(parking, jardin));

        assertAll(
                () -> assertThat(appartement.getComSalePrice()).isEqualByComparingTo("794800"),
                () -> assertThat(appartement.getComSalePriceHT()).isEqualByComparingTo("664000"),
                () -> assertThat(appartement.getComSalePriceFloor()).isEqualByComparingTo("780400"),
                () -> assertThat(appartement.getComSalePriceFloorHT()).isEqualByComparingTo("652000"));
    }

    @Test
    void sousArticle_prixDeVenteEgalPrixArticle() {
        StkItem parking = article("12", "12", "1000", "2000", "0.20");
        CalculPrixArticle.appliquerPrixDeVente(parking, List.of());

        assertThat(parking.getComSalePrice()).isEqualByComparingTo(parking.getSalePrice());
    }

    @Test
    void valeursAbsentes_traiteesCommeZero() {
        StkItem item = new StkItem();
        CalculPrixArticle.appliquerPrixArticle(item);
        CalculPrixArticle.appliquerPrixDeVente(item, List.of());

        assertThat(item.getComSalePrice()).isEqualByComparingTo("0");
    }
}
