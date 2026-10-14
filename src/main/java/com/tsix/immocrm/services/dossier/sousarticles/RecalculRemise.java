package com.tsix.immocrm.services.dossier.sousarticles;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

/**
 * {@code Sale.cs:RecalcDiscount} après l'ajout ou le retrait d'un sous-article : le montant de remise HT est conservé,
 * le taux devient montant / prix HT avant remise, et s'applique à l'article (prix stocké, pas surface × prix m²) et à
 * chaque sous-article. Voir FEATURE-vente-echeancier.md §2 (sous-articles).
 */
public final class RecalculRemise {

    private static final int ECHELLE = 6;
    /** Le taux reste plein pendant le calcul ; arrondi à 6 il décalerait le prix après remise. */
    private static final int ECHELLE_TAUX = 16;

    private RecalculRemise() {
    }

    public static void appliquer(ComFolder f, List<ComFolderItem> sousArticles) {
        BigDecimal tva = zero(f.getVATRatio());
        BigDecimal articleHT = zero(f.getItemPriceHT());
        BigDecimal sousHT = BigDecimal.ZERO;
        BigDecimal sousTTC = BigDecimal.ZERO;
        BigDecimal surfaces = BigDecimal.ZERO;
        for (ComFolderItem s : sousArticles) {
            BigDecimal ht = zero(s.getPriceBeforeDiscountHT());
            sousHT = sousHT.add(ht);
            sousTTC = sousTTC.add(ht.multiply(unPlus(s.getVATRatio())));
            surfaces = surfaces.add(zero(s.getArea()));
        }
        BigDecimal avantHT = articleHT.add(sousHT);
        BigDecimal remiseHT = zero(f.getDiscountAmount());
        BigDecimal taux = avantHT.signum() == 0
                ? zero(f.getDiscount())
                : remiseHT.divide(avantHT, ECHELLE_TAUX, RoundingMode.HALF_UP);
        BigDecimal resteATaux = BigDecimal.ONE.subtract(taux);

        BigDecimal sousApresHT = BigDecimal.ZERO;
        BigDecimal sousApresTTC = BigDecimal.ZERO;
        for (ComFolderItem s : sousArticles) {
            BigDecimal ht = zero(s.getPriceBeforeDiscountHT());
            BigDecimal apresHT = ht.multiply(resteATaux);
            BigDecimal apresTTC = apresHT.multiply(unPlus(s.getVATRatio()));
            s.setPriceBeforeDiscount(r(ht.multiply(unPlus(s.getVATRatio()))));
            s.setDiscount(r(taux));
            s.setPriceHT(r(apresHT));
            s.setPrice(r(apresTTC));
            sousApresHT = sousApresHT.add(apresHT);
            sousApresTTC = sousApresTTC.add(apresTTC);
        }
        BigDecimal articleApresHT = articleHT.multiply(resteATaux);
        BigDecimal articleApresTTC = articleApresHT.multiply(BigDecimal.ONE.add(tva));
        BigDecimal venteHT = articleApresHT.add(sousApresHT);
        BigDecimal venteTTC = articleApresTTC.add(sousApresTTC);
        boolean avecModifications = Boolean.TRUE.equals(f.getPriceWithModification());

        f.setUnderItemsPriceHT(r(sousHT));
        f.setUnderItemsPrice(r(sousTTC));
        f.setPriceBeforeDiscountHT(r(avantHT));
        f.setPriceBeforeDiscount(r(zero(f.getItemPrice()).add(sousTTC)));
        f.setDiscount(ConventionRemise.ratioDuHT(remiseHT, avantHT));
        f.setItemPriceAfterHT(r(articleApresHT));
        f.setItemPriceAfter(r(articleApresTTC));
        f.setUnderItemsPriceAfterHT(r(sousApresHT));
        f.setUnderItemsPriceAfter(r(sousApresTTC));
        f.setPriceAfterDiscountHT(r(venteHT));
        f.setPriceAfterDiscount(r(venteTTC));
        f.setPriceHT(r(avecModifications ? venteHT.add(zero(f.getModificationsAmountHT())) : venteHT));
        f.setPrice(r(avecModifications ? venteTTC.add(zero(f.getModificationsAmount())) : venteTTC));
        f.setGlobalArea(r(zero(f.getArea()).add(surfaces)));
    }

    private static BigDecimal unPlus(BigDecimal tva) {
        return BigDecimal.ONE.add(zero(tva));
    }

    private static BigDecimal zero(BigDecimal v) {
        return requireNonNullElse(v, BigDecimal.ZERO);
    }

    private static BigDecimal r(BigDecimal v) {
        return v.setScale(ECHELLE, RoundingMode.HALF_UP);
    }
}
