package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;

/**
 * Colonnes calculées d'une ligne de pièce d'achat, règle de la vue OMRAN {@code BuyDocumentLineView} : pleine échelle,
 * aucun arrondi par ligne ; valeur absente = 0. Remise, FODEC et TVA en fraction.
 */
public record LigneAchatCalculee(BigDecimal prixUnitaireHTNet, BigDecimal totalHT, BigDecimal totalTVA,
                                 BigDecimal totalTTC) {

    public static LigneAchatCalculee de(BigDecimal prixUnitaireHT, BigDecimal quantite, BigDecimal remise,
                                        BigDecimal fodec, BigDecimal tauxTva) {
        BigDecimal net = zero(prixUnitaireHT).multiply(BigDecimal.ONE.subtract(zero(remise)))
                .multiply(BigDecimal.ONE.add(zero(fodec)));
        BigDecimal ht = net.multiply(zero(quantite));
        BigDecimal tva = ht.multiply(zero(tauxTva));
        return new LigneAchatCalculee(net, ht, tva, ht.add(tva));
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
