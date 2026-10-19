package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-entites-juridiques.md §7.8 : {@code ORDRE_CLES_ETRANGERES} remonte, avant {@code CfgProject}, dans l'ordre
 * CfgShareholder, CfgGroupShareholder, CfgGroupShareholderLine, CfgCompany, CfgCompanyShareholder (parents d'abord).
 */
class EntitesOrdreRemonteeTest {

    private static final List<String> ORDRE_DU_LOT = List.of("CfgShareholder", "CfgGroupShareholder",
            "CfgGroupShareholderLine", "CfgCompany", "CfgCompanyShareholder");

    @Test
    void r7_8_lesCinqTablesSontListees() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES).containsAll(ORDRE_DU_LOT);
    }

    @Test
    void r7_8_ordreParentsAvantEnfants_toutAvantCfgProject() {
        List<Integer> rangs = ORDRE_DU_LOT.stream().map(CollecteurLignesEcrites::rang).toList();

        assertThat(rangs).hasSize(5).isSorted().doesNotHaveDuplicates()
                .allMatch(r -> r < CollecteurLignesEcrites.rang("CfgProject"));
    }

    /** L'habilitation créée avec l'entité (§7.4) remonte après elle. */
    @Test
    void r7_8_habilitationApresLEntite() {
        assertThat(CollecteurLignesEcrites.rang("AdmUserHabilitation"))
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgCompany"));
    }
}
