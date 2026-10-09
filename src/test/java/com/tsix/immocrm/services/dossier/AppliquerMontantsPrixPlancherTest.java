package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Calcul partagé par la création de dossier et la simulation du schéma (FEATURE-lot4 §1). */
class AppliquerMontantsPrixPlancherTest {

    private static StkItem article(String prix, String plancher) {
        StkItem item = new StkItem();
        item.setVat(new BigDecimal("0.20"));
        item.setComSalePrice(new BigDecimal(prix));
        item.setComSalePriceHT(BigDecimal.ZERO);
        item.setComSalePriceFloor(new BigDecimal(plancher));
        return item;
    }

    @Test
    void prixCommercialNul_lePlancherEstLaBase_etLaRemiseNEstPasNegative() {
        ComFolder folder = new ComFolder();

        DossierService.appliquerMontants(folder, article("0", "900000"), List.of(),
                new BigDecimal("900000"), null, BigDecimal.ZERO);

        assertThat(folder.getItemPrice()).isEqualByComparingTo("900000");
        assertThat(folder.getPriceBeforeDiscount()).isEqualByComparingTo("900000");
        assertThat(folder.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void prixNegocieSousLePlancher_remisePositive() {
        ComFolder folder = new ComFolder();

        DossierService.appliquerMontants(folder, article("0", "900000"), List.of(),
                new BigDecimal("850000"), null, BigDecimal.ZERO);

        // DiscountAmount est HT (convention OMRAN) : la remise TTC est l'écart des prix TTC.
        assertThat(remiseTTC(folder)).isEqualByComparingTo("50000").isNotNegative();
        assertThat(folder.getDiscountAmount()).isEqualByComparingTo("41666.666667");
    }

    private static BigDecimal remiseTTC(ComFolder folder) {
        return folder.getPriceBeforeDiscount().subtract(folder.getPriceAfterDiscount());
    }

    @Test
    void prixEtPlancherNuls_laBaseEstLePrixNegocie_remiseNulle() {
        ComFolder folder = new ComFolder();

        DossierService.appliquerMontants(folder, article("0", "0"), List.of(),
                new BigDecimal("700000"), null, BigDecimal.ZERO);

        assertThat(folder.getPriceBeforeDiscount()).isEqualByComparingTo("700000");
        assertThat(folder.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void prixImpose_avecRemiseExplicite_remplaceLePlancher() {
        ComFolder folder = new ComFolder();

        DossierService.appliquerMontants(folder, article("0", "900000"), List.of(),
                new BigDecimal("1000000"), BigDecimal.TEN, BigDecimal.ZERO);

        assertThat(folder.getPriceBeforeDiscount()).isEqualByComparingTo("1000000");
        assertThat(remiseTTC(folder)).isEqualByComparingTo("100000");
        assertThat(folder.getDiscount()).isEqualByComparingTo("0.1");
    }

    @Test
    void sousArticleAPrixCommercialNul_compteAuPrixPlancher() {
        ComFolder folder = new ComFolder();
        StkItem annexe = article("0", "120000");

        DossierService.appliquerMontants(folder, article("1000000", "0"), List.of(annexe),
                new BigDecimal("1120000"), null, BigDecimal.ZERO);

        assertThat(folder.getUnderItemsPrice()).isEqualByComparingTo("120000");
        assertThat(remiseTTC(folder)).isEqualByComparingTo("0");
    }
}
