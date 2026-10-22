package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-prospections-terrain.md §6.1 « Routage » : LndLand et LndLandZone connues du rapatriement, parent
 * avant enfant — LndLand avant StkItem (StkItem.LndLandId) et StkFeasibilityStudy (LndLandId), LndLandZone après LndLand ;
 * StrHistory (écrite par les transitions) connue ; toutes avant l'audit qui les cite.
 */
class ProspectionsTerrainOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    private static int inconnue() {
        return CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();
    }

    @Test
    void r6_1_prospection_avantLeTerrainEtLEtude_avantLAudit() {
        assertThat(rang("LndLand"))
                .isLessThan(inconnue())
                .isLessThan(rang("StkItem"))
                .isLessThan(rang("StkFeasibilityStudy"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void r6_1_zone_apresLaProspection_avantLAudit() {
        assertThat(rang("LndLandZone"))
                .isLessThan(inconnue())
                .isGreaterThan(rang("LndLand"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void r6_1_historiqueDeStatut_connu() {
        assertThat(rang("StrHistory")).isLessThan(inconnue());
    }
}
