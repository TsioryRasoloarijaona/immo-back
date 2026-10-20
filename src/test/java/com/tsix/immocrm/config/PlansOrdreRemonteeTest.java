package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-plans.md, « Recalage sur main » (Recopie) et Carte des fichiers : {@code StkHierarchyPlan} est
 * connue du rapatriement, après {@code CfgTranche} (FK {@code StkHierarchyId}) et avant l'audit qui la cite.
 */
class PlansOrdreRemonteeTest {

    @Test
    void recopie_plan_apresLaTranche_avantLAudit() {
        assertThat(CollecteurLignesEcrites.rang("StkHierarchyPlan"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("CfgTranche"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }
}
