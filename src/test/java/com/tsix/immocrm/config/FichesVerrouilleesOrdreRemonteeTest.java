package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** FEATURE-admin-fiches-verrouillees.md, section Back : table déclarée, après sa session parente, avant le journal. */
class FichesVerrouilleesOrdreRemonteeTest {

    @Test
    void admWebLockEntities_declaree_entreAdmWebSessionsEtAdmWebHistorics() {
        assertThat(CollecteurLignesEcrites.rang("AdmWebLockEntities"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("AdmWebSessions"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }
}
