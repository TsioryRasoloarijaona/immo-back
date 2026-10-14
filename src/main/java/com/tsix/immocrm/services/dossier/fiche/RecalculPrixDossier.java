package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Seul calcul des prix d'un dossier après changement de superficie, de TVA ou de prix HT : le montant de remise
 * est conservé, le taux change ({@code Sale.cs:RecalcDiscount}). Partagé par la réinitialisation et le
 * réajustement des superficies. Voir FEATURE-vente-fiche.md §B5, §B6.
 */
public final class RecalculPrixDossier {

    /** Échelle des colonnes decimal(26,6). */
    private static final int ECHELLE = 6;
    /** Le taux reste plein pendant le calcul ; arrondi à 6 il décalerait le prix après remise. */
    private static final int ECHELLE_TAUX = 16;

    private RecalculPrixDossier() {
    }

    public record SousArticle(UUID id, BigDecimal superficie, BigDecimal tva, BigDecimal prixAvantRemiseHT) {
    }

    public record Saisie(BigDecimal superficie, BigDecimal tva, BigDecimal prixM2HT, BigDecimal remiseMontantHT,
                         boolean avecModifications, BigDecimal modificationsHT, BigDecimal modificationsTTC,
                         List<SousArticle> sousArticles) {
    }

    public record SousArticleCalcule(UUID id, BigDecimal superficie, BigDecimal tva, BigDecimal prixAvantRemiseHT,
                                     BigDecimal prixAvantRemiseTTC, BigDecimal remise, BigDecimal prixHT,
                                     BigDecimal prixTTC) {
    }

    public record Resultat(BigDecimal superficie, BigDecimal superficieGlobale, BigDecimal tva,
                           BigDecimal prixM2HT, BigDecimal prixM2TTC,
                           BigDecimal prixArticleHT, BigDecimal prixArticleTTC,
                           BigDecimal prixSousArticlesHT, BigDecimal prixSousArticlesTTC,
                           BigDecimal totalHT, BigDecimal totalTTC, BigDecimal remiseMontantHT, BigDecimal remise,
                           BigDecimal prixArticleApresRemiseHT, BigDecimal prixArticleApresRemiseTTC,
                           BigDecimal prixSousArticlesApresRemiseHT, BigDecimal prixSousArticlesApresRemiseTTC,
                           BigDecimal prixVenteHT, BigDecimal prixVenteTTC, BigDecimal prixHT, BigDecimal prix,
                           List<SousArticleCalcule> sousArticles) {
    }

    /** Valeurs actuelles du dossier, sous-articles compris ({@code ComFolderItem.IsUnderItem}). */
    public static Saisie depuisDossier(ComFolder f, List<ComFolderItem> lignes) {
        List<SousArticle> sousArticles = sousArticlesDe(lignes).stream()
                .map(i -> new SousArticle(i.getId(), i.getArea(), i.getVATRatio(), i.getPriceBeforeDiscountHT()))
                .toList();
        return new Saisie(f.getArea(), f.getVATRatio(), f.getUnitCostHT(), f.getDiscountAmount(),
                Boolean.TRUE.equals(f.getPriceWithModification()), f.getModificationsAmountHT(),
                f.getModificationsAmount(), sousArticles);
    }

    public static List<ComFolderItem> sousArticlesDe(List<ComFolderItem> lignes) {
        return lignes.stream().filter(i -> Boolean.TRUE.equals(i.getIsUnderItem())).toList();
    }

    public static Resultat calculer(Saisie s) {
        BigDecimal superficie = zero(s.superficie());
        BigDecimal tva = zero(s.tva());
        BigDecimal prixM2HT = zero(s.prixM2HT());
        BigDecimal remiseHT = zero(s.remiseMontantHT());
        BigDecimal unPlusTva = BigDecimal.ONE.add(tva);

        BigDecimal prixArticleHT = superficie.multiply(prixM2HT);
        BigDecimal prixArticleTTC = prixArticleHT.multiply(unPlusTva);
        BigDecimal sousHT = s.sousArticles().stream().map(a -> zero(a.prixAvantRemiseHT()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sousTTC = s.sousArticles().stream()
                .map(a -> zero(a.prixAvantRemiseHT()).multiply(BigDecimal.ONE.add(zero(a.tva()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHT = prixArticleHT.add(sousHT);
        BigDecimal taux = totalHT.signum() == 0 ? BigDecimal.ZERO
                : remiseHT.divide(totalHT, ECHELLE_TAUX, RoundingMode.HALF_UP);
        BigDecimal unMoinsTaux = BigDecimal.ONE.subtract(taux);

        List<SousArticleCalcule> sousCalcules = s.sousArticles().stream()
                .map(a -> sousArticle(a, taux, unMoinsTaux))
                .toList();
        BigDecimal articleApresHT = prixArticleHT.multiply(unMoinsTaux);
        BigDecimal articleApresTTC = articleApresHT.multiply(unPlusTva);
        BigDecimal sousApresHT = sousCalcules.stream().map(SousArticleCalcule::prixHT)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sousApresTTC = sousCalcules.stream().map(SousArticleCalcule::prixTTC)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal venteHT = articleApresHT.add(sousApresHT);
        BigDecimal venteTTC = articleApresTTC.add(sousApresTTC);
        BigDecimal superficieGlobale = superficie.add(s.sousArticles().stream().map(a -> zero(a.superficie()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return new Resultat(r(superficie), r(superficieGlobale), r(tva), r(prixM2HT), r(prixM2HT.multiply(unPlusTva)),
                r(prixArticleHT), r(prixArticleTTC), r(sousHT), r(sousTTC), r(totalHT), r(prixArticleTTC.add(sousTTC)),
                r(remiseHT), ConventionRemise.ratioDuHT(remiseHT, totalHT),
                r(articleApresHT), r(articleApresTTC), r(sousApresHT), r(sousApresTTC), r(venteHT), r(venteTTC),
                r(s.avecModifications() ? venteHT.add(zero(s.modificationsHT())) : venteHT),
                r(s.avecModifications() ? venteTTC.add(zero(s.modificationsTTC())) : venteTTC),
                sousCalcules);
    }

    private static SousArticleCalcule sousArticle(SousArticle a, BigDecimal taux, BigDecimal unMoinsTaux) {
        BigDecimal unPlusTva = BigDecimal.ONE.add(zero(a.tva()));
        BigDecimal avantHT = zero(a.prixAvantRemiseHT());
        BigDecimal apresHT = avantHT.multiply(unMoinsTaux);
        return new SousArticleCalcule(a.id(), a.superficie(), a.tva(), r(avantHT), r(avantHT.multiply(unPlusTva)),
                r(taux), r(apresHT), r(apresHT.multiply(unPlusTva)));
    }

    /** Écrit le résultat sur le dossier et ses sous-articles ; le montant de remise n'est pas touché. */
    public static void appliquer(Resultat r, ComFolder f, Map<UUID, ComFolderItem> sousArticles) {
        f.setArea(r.superficie());
        f.setGlobalArea(r.superficieGlobale());
        f.setVATRatio(r.tva());
        f.setUnitCostHT(r.prixM2HT());
        f.setUnitCost(r.prixM2TTC());
        f.setItemPriceHT(r.prixArticleHT());
        f.setItemPrice(r.prixArticleTTC());
        f.setUnderItemsPriceHT(r.prixSousArticlesHT());
        f.setUnderItemsPrice(r.prixSousArticlesTTC());
        f.setPriceBeforeDiscountHT(r.totalHT());
        f.setPriceBeforeDiscount(r.totalTTC());
        f.setDiscount(r.remise());
        f.setItemPriceAfterHT(r.prixArticleApresRemiseHT());
        f.setItemPriceAfter(r.prixArticleApresRemiseTTC());
        f.setUnderItemsPriceAfterHT(r.prixSousArticlesApresRemiseHT());
        f.setUnderItemsPriceAfter(r.prixSousArticlesApresRemiseTTC());
        f.setPriceAfterDiscountHT(r.prixVenteHT());
        f.setPriceAfterDiscount(r.prixVenteTTC());
        f.setPriceHT(r.prixHT());
        f.setPrice(r.prix());
        for (SousArticleCalcule c : r.sousArticles()) {
            ComFolderItem i = Objects.requireNonNull(sousArticles.get(c.id()), "sous-article absent du dossier");
            i.setArea(c.superficie());
            i.setVATRatio(c.tva());
            i.setPriceBeforeDiscountHT(c.prixAvantRemiseHT());
            i.setPriceBeforeDiscount(c.prixAvantRemiseTTC());
            i.setDiscount(c.remise());
            i.setPriceHT(c.prixHT());
            i.setPrice(c.prixTTC());
        }
    }

    private static BigDecimal zero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal r(BigDecimal v) {
        return v.setScale(ECHELLE, RoundingMode.HALF_UP);
    }
}
