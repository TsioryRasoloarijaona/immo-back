package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * « À clôturer » d'un bon de commande, comme {@code BuyDocumentOrderView.ActiveAlert} d'OMRAN : HT du BC ≤ Σ HT de ses
 * factures et type d'état du statut ni « Terminé avec succès » ni « Terminé avec échec » (voir FEATURE-achat-liste-parite.md E3).
 */
public final class AlerteClotureCommande {

    /** Σ HT des factures rattachées au BC {@code d} ({@code BuyOrderId}), 0 sans facture. */
    public static final String SQL_HT_FACTURE = "(SELECT COALESCE(SUM(f.AmountHT), 0) FROM BuyDocument f "
            + "WHERE f.DocType = 'INVOICE' AND f.BuyOrderId = d.PKey)";

    private static final Set<UUID> ETATS_TERMINES = Set.of(
            UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA"),
            UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97"));

    private AlerteClotureCommande() {
    }

    /** Faux pour une autre nature, un HT absent ou un BC sans statut ({@code NOT IN} sur NULL n'est pas vrai en SQL). */
    public static boolean aCloturer(String nature, BigDecimal ht, BigDecimal htFactures, UUID typeEtat) {
        return NatureDocumentAchat.ORDER.name().equals(nature) && ht != null && htFactures != null && typeEtat != null
                && ht.compareTo(htFactures) <= 0 && !ETATS_TERMINES.contains(typeEtat);
    }
}
