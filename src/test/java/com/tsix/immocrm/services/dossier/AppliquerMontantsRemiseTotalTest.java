package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/** FEATURE-remise-dossier.md §2 : avec remisePercent, prix = total avant remise, annexes comprises. */
class AppliquerMontantsRemiseTotalTest {

    private static final BigDecimal CINQ = new BigDecimal("5");

    private static StkItem appartement() {
        StkItem item = new StkItem();
        item.setVat(new BigDecimal("0.20"));
        item.setComSalePrice(new BigDecimal("1000000.00"));
        item.setComSalePriceHT(new BigDecimal("833333.333333"));
        return item;
    }

    private static StkItem parking() {
        StkItem item = new StkItem();
        item.setVat(new BigDecimal("0.20"));
        item.setComSalePrice(new BigDecimal("200000.00"));
        item.setComSalePriceHT(new BigDecimal("166666.666667"));
        return item;
    }

    @Test
    void remiseCinqPourcent_total1200000_ventilationTtcEtHtCoherente() {
        ComFolder f = new ComFolder();

        DossierService.appliquerMontants(f, appartement(), List.of(parking()),
                new BigDecimal("1200000"), CINQ, BigDecimal.ZERO);

        assertAll(
                () -> assertThat(f.getItemPrice()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getUnderItemsPrice()).isEqualByComparingTo("200000"),
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("1200000"),
                // Convention OMRAN : DiscountAmount HT, Discount ratio 0–1 du HT.
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.05"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1140000"),
                () -> assertThat(f.getItemPriceHT()).isEqualByComparingTo("833333.333333"),
                () -> assertThat(f.getUnderItemsPriceHT()).isEqualByComparingTo("166666.666667"),
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("950000"),
                () -> assertThat(f.getItemPriceAfter().add(f.getUnderItemsPriceAfter()))
                        .isEqualByComparingTo(f.getPriceAfterDiscount()),
                () -> assertThat(f.getItemPriceAfterHT().add(f.getUnderItemsPriceAfterHT()))
                        .isEqualByComparingTo(f.getPriceAfterDiscountHT()),
                () -> assertThat(f.getTotalToPay()).isEqualByComparingTo("1140000"));
    }

    @Test
    void remise_prixEgalAuxAnnexes_accepte_principalANul() {
        ComFolder f = new ComFolder();

        DossierService.appliquerMontants(f, appartement(), List.of(parking()),
                new BigDecimal("200000"), CINQ, BigDecimal.ZERO);

        assertAll(
                () -> assertThat(f.getItemPrice()).isEqualByComparingTo("0"),
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("200000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("190000"),
                () -> assertThat(f.getItemPriceAfter()).isEqualByComparingTo("0"),
                () -> assertThat(f.getUnderItemsPriceAfter()).isEqualByComparingTo("190000"));
    }

    @Test
    void remise_prixUnCentimeSousLesAnnexes_400() {
        ComFolder f = new ComFolder();
        StkItem principal = appartement();
        List<StkItem> annexes = List.of(parking());
        BigDecimal prix = new BigDecimal("199999.99");

        assertThatThrownBy(() -> DossierService.appliquerMontants(f, principal, annexes, prix, CINQ, BigDecimal.ZERO))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void sansRemise_prixNegocieTotal_inchange() {
        ComFolder f = new ComFolder();

        DossierService.appliquerMontants(f, appartement(), List.of(parking()),
                new BigDecimal("1140000"), null, BigDecimal.ZERO);

        assertAll(
                () -> assertThat(f.getItemPrice()).isEqualByComparingTo("1000000"),
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("1200000"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("50000"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.05"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1140000"));
    }

    @Test
    void prixNegocieAuDessusDuPrixDeReference_remiseNegative_ratioEtMontantHtNegatifs() {
        ComFolder f = new ComFolder();

        DossierService.appliquerMontants(f, appartement(), List.of(), new BigDecimal("1100000"), null, BigDecimal.ZERO);

        assertAll(
                // Sale.cs:RecalcDiscount : taux (tiré des TTC) × HT avant remise.
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("-83333.333333"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("-0.1"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("1100000"));
    }

    @Test
    void remiseSurPrixNul_baseHtNulle_ratioZeroSansDivisionParZero() {
        ComFolder f = new ComFolder();

        DossierService.appliquerMontants(f, appartement(), List.of(), BigDecimal.ZERO, CINQ, BigDecimal.ZERO);

        assertAll(
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("0"));
    }
}
