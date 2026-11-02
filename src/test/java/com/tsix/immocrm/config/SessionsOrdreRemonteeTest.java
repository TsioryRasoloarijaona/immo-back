package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** FEATURE-admin-sessions.md, section Back : {@code AdmWebSessions} déclarée, après son parent, avant le journal. */
class SessionsOrdreRemonteeTest {

    @Test
    void admWebSessions_declaree_entreAdmUserEtAdmWebHistorics() {
        assertThat(CollecteurLignesEcrites.rang("AdmWebSessions"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("AdmUser"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }
}
