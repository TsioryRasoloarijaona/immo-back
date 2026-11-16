package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * FEATURE-restes-projet-achat.md §6 : tables écrites par l'éditeur des statuts / transitions de lot déclarées dans l'ordre de
 * remontée, parents avant enfants, {@code StrStatus} avant {@code PrjMarket} qui le cite.
 */
class StatutsLotOrdreRemonteeTest {

    private static final int HORS_LISTE = CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();

    @Test
    void statuts_declares_imageAvantStatutAvantChampsObligatoires_avantPrjMarket() {
        List<Integer> rangs = List.of("CfgImage", "StrStatus", "StrStatusMandatoryField").stream()
                .map(CollecteurLignesEcrites::rang).toList();

        assertAll(
                () -> assertThat(rangs).hasSize(3).isSorted().doesNotHaveDuplicates().isNotEmpty()
                        .allMatch(r -> r < HORS_LISTE),
                () -> assertThat(CollecteurLignesEcrites.rang("StrStatus")).isLessThan(CollecteurLignesEcrites.rang("PrjMarket")));
    }

    @Test
    void transitions_declarees_statutAvantTransitionAvantIntervenants() {
        List<Integer> rangs = List.of("StrStatus", "StrWorkFlow", "StrWorkFlowIntervener").stream()
                .map(CollecteurLignesEcrites::rang).toList();

        assertThat(rangs).hasSize(3).isSorted().doesNotHaveDuplicates().isNotEmpty().allMatch(r -> r < HORS_LISTE);
    }
}
