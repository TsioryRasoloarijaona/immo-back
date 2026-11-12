package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les pièces d'achat (1100) que la table cite des deux côtés (FEATURE-achat-fiche-ecritures.md E-B7). */
public class EcrituresAchat implements TablesEcrites {

    @Override
    public int rang() {
        return 1110;
    }

    @Override
    public List<String> tables() {
        return List.of("BuyOrderInvoice");
    }
}
