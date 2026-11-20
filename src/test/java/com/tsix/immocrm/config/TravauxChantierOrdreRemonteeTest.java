package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** FEATURE-n2-travaux-chantier.md : GrhWorkedDay est connue du rapatriement, après chacun de ses parents, avant l'audit. */
class TravauxChantierOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @Test
    void jourTravaille_connu_avantLAudit() {
        assertThat(rang("GrhWorkedDay"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(rang("AdmWebHistorics"));
    }

    /** R-N2b-11 : les lignes {@code StrHistory} des transitions remontent après le jour travaillé, avant l'audit. */
    @Test
    void r11_historiqueDesStatuts_apresLeJourTravaille_avantLAudit() {
        assertThat(rang("StrHistory"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("GrhWorkedDay"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GrhWorkedDayType", "CfgProjectZone", "CfgTrancheLevelingDefinition", "PrjProject", "StrStatus"})
    void jourTravaille_apresSonParent_connu(String parent) {
        assertThat(rang(parent))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(rang("GrhWorkedDay"));
    }
}
