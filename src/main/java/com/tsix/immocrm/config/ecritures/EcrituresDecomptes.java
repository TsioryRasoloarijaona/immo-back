package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les pièces d'achat (1100) citées par {@code PrjWorkingOut.BuyDocumentId} ; décompte avant ses lignes (FEATURE-decomptes-parite.md). */
public class EcrituresDecomptes implements TablesEcrites {

    @Override
    public int rang() {
        return 1180;
    }

    @Override
    public List<String> tables() {
        return List.of("PrjWorkingOut", "PrjWorkingOutLine", "PrjWorkingOutLineExtra", "PrjWorkingOutStockLine",
                "PrjWorkingOutRetainedLine");
    }
}
