package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les lots et les tiers, avant les pièces d'achat (1100) qui citent {@code BuySelection} (FEATURE-n6-demandes-achat.md). */
public class EcrituresDemandesAchat implements TablesEcrites {

    @Override
    public int rang() {
        return 1073;
    }

    @Override
    public List<String> tables() {
        return List.of("BuySelectionKind", "BuySelection", "BuySelectionGroup", "BuySelectionLine", "BuySelectionStockLine",
                "BuySelectionPaymentTerm", "BuySelectionSupplier");
    }
}
