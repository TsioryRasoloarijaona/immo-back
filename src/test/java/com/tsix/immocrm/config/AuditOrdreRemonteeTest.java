package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-audit-actions.md §2.2.5, §4 et §11.4 : {@code AdmWebHistorics} et {@code AdmMessage} sont connues
 * du rapatriement, en fin de liste, après tous les objets que la ligne d'audit cite.
 */
class AuditOrdreRemonteeTest {

    private static final String AUDIT = "AdmWebHistorics";
    private static final String NOTES = "AdmMessage";

    @Test
    void r11_4_auditEtNotesConnues() {
        int inconnue = CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();

        assertThat(List.of(CollecteurLignesEcrites.rang(AUDIT), CollecteurLignesEcrites.rang(NOTES)))
                .isNotEmpty()
                .allSatisfy(rang -> assertThat(rang).isLessThan(inconnue));
    }

    @Test
    void r11_4_auditEtNotesEnFinDeListe() {
        int taille = CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();

        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.subList(Math.max(0, taille - 2), taille))
                .containsExactlyInAnyOrder(AUDIT, NOTES);
    }

    @Test
    void r2_2_5_auditApresLesObjetsQuIlCite() {
        int rangAudit = CollecteurLignesEcrites.rang(AUDIT);

        assertThat(List.of("CfgProject", "CfgTranche", "StkItem"))
                .isNotEmpty()
                .allSatisfy(table -> assertThat(CollecteurLignesEcrites.rang(table)).as(table).isLessThan(rangAudit));
    }
}
