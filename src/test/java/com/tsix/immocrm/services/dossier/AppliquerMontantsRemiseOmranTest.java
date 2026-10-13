package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Remise d'un dossier dont le lot (TVA 20 %) a un jardin à 0 % : un seul taux, tiré des TTC, appliqué aux HT
 * ({@code Sale.cs:RecalcDiscount}). Cas réel OMRAN : ARS 2025/00349 de yesRemsProd.
 */
class AppliquerMontantsRemiseOmranTest {

    private static final Offset<BigDecimal> ARRONDI_BASE = Offset.offset(new BigDecimal("0.000002"));

    private static StkItem lot() {
        StkItem lot = new StkItem();
        lot.setVat(new BigDecimal("0.2"));
        return lot;
    }

    private static StkItem jardin() {
        StkItem jardin = new StkItem();
        jardin.setVat(BigDecimal.ZERO);
        jardin.setComSalePrice(new BigDecimal("151284.95"));
        jardin.setComSalePriceHT(new BigDecimal("151284.95"));
        return jardin;
    }

    private static ComFolder calculer(String prix) {
        ComFolder f = new ComFolder();
        PrixArticle.PrixDeReference lotSeul = new PrixArticle.PrixDeReference(
                new BigDecimal("799999.999997"), new BigDecimal("666666.666664"), false);
        DossierService.appliquerMontants(f, lot(), lotSeul, List.of(jardin()), new BigDecimal(prix), null, BigDecimal.ZERO);
        return f;
    }

    private static BigDecimal centimes(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    void ars202500349_reproduitLesMontantsDOmran() {
        ComFolder f = calculer("950000");

        assertAll(
                () -> assertThat(f.getPriceBeforeDiscount()).isEqualByComparingTo("951284.949997"),
                () -> assertThat(f.getPriceBeforeDiscountHT()).isEqualByComparingTo("817951.616664"),
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0.001351"),
                () -> assertThat(centimes(f.getDiscountAmount())).isEqualByComparingTo("1104.85"),
                () -> assertThat(f.getDiscountAmount()).isCloseTo(new BigDecimal("1104.849737"), ARRONDI_BASE),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("950000"),
                () -> assertThat(centimes(f.getPriceAfterDiscountHT())).isEqualByComparingTo("816846.77"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isCloseTo(new BigDecimal("816846.766926"), ARRONDI_BASE),
                () -> assertThat(f.getItemPriceAfterHT()).isCloseTo(new BigDecimal("665766.165367"), ARRONDI_BASE),
                () -> assertThat(f.getItemPriceAfter()).isCloseTo(new BigDecimal("798919.398441"), ARRONDI_BASE),
                () -> assertThat(f.getUnderItemsPriceAfterHT()).isCloseTo(new BigDecimal("151080.601559"), ARRONDI_BASE),
                () -> assertThat(f.getUnderItemsPriceAfter()).isCloseTo(new BigDecimal("151080.601559"), ARRONDI_BASE));
    }

    /**
     * ARS 2026/00413 : prix = TTC avant remise, arrondi au centime comme l'écran le saisit ⇒ aucune remise
     * fantôme malgré deux taux de TVA.
     */
    @Test
    void prixEgalAuPrixAvantRemise_aucuneRemise() {
        ComFolder f = calculer("951284.95");

        assertAll(
                () -> assertThat(f.getDiscount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getDiscountAmount()).isEqualByComparingTo("0"),
                () -> assertThat(f.getPriceAfterDiscountHT()).isEqualByComparingTo("817951.616664"),
                () -> assertThat(f.getUnderItemsPriceAfterHT()).isEqualByComparingTo("151284.95"));
    }
}
