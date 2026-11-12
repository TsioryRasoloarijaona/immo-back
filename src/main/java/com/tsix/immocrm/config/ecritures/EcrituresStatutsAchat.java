package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Après les tiers (1000), avant les pièces d'achat (1100) qui citent {@code BuyDocumentStatus} (FEATURE-statuts-achat-parite.md). */
public class EcrituresStatutsAchat implements TablesEcrites {

    @Override
    public int rang() {
        return 1050;
    }

    @Override
    public List<String> tables() {
        return List.of("CfgImage", "BuyDocumentStatus", "BuyDocumentStatusDocumentToAttach", "BuyDocumentWorkFlow");
    }
}
