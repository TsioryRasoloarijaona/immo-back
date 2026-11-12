package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-statuts-achat-parite.md règle 11 : {@code CfgImage}, {@code BuyDocumentStatus},
 * {@code BuyDocumentStatusDocumentToAttach} déclarées, parents avant enfants, toutes avant {@code BuyDocument} qui les cite.
 */
class StatutsAchatOrdreRemonteeTest {

    private static final List<String> ORDRE_DU_LOT = List.of("CfgImage", "BuyDocumentStatus",
            "BuyDocumentStatusDocumentToAttach");

    @Test
    void r11_tablesDeclarees_parentsAvantEnfants_avantBuyDocument() {
        List<Integer> rangs = ORDRE_DU_LOT.stream().map(CollecteurLignesEcrites::rang).toList();

        assertThat(rangs).hasSize(3).isSorted().doesNotHaveDuplicates().isNotEmpty()
                .allMatch(r -> r < CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .allMatch(r -> r < CollecteurLignesEcrites.rang("BuyDocument"));
    }
}
