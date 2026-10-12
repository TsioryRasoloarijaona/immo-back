package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ligne chiffrée d'un bordereau (définitif ou estimatif) : quantité × prix. Les totaux de ligne ne
 * sont pas stockés en base ; ils se calculent ici et nulle part ailleurs.
 */
public record LigneMontant(UUID lotId, BigDecimal quantite, BigDecimal prixUnitaireHT, BigDecimal tauxTva,
                           BigDecimal prixUnitaireTTC) {

    /** {@code Quantity × UnitPriceHT}, non arrondi. */
    public BigDecimal totalHT() {
        return Montants.produit(quantite, prixUnitaireHT);
    }

    /** {@code Quantity × UnitPriceHT × VatRatio} ({@code VatRatio} est une fraction : 0,2). */
    public BigDecimal totalTVA() {
        return Montants.produit(totalHT(), tauxTva);
    }

    /** {@code Quantity × UnitPriceTTC} — pas HT + TVA : le prix TTC est stocké à part. */
    public BigDecimal totalTTC() {
        return Montants.produit(quantite, prixUnitaireTTC);
    }
}
