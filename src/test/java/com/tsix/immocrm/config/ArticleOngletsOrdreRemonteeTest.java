package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-article-onglets.md §9.3 : le lot n'écrit que {@code AdmAttachedFile}, {@code AppFileObject} et
 * l'audit ; les deux premières sont déjà connues du rapatriement, la pièce avant son pointeur et avant l'audit.
 */
class ArticleOngletsOrdreRemonteeTest {

    @Test
    void r9_3_pieceEtPointeurConnus_pieceAvantPointeurEtAvantLAudit() {
        int piece = CollecteurLignesEcrites.rang("AdmAttachedFile");

        assertThat(piece)
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isLessThan(CollecteurLignesEcrites.rang("AppFileObject"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
        assertThat(CollecteurLignesEcrites.rang("AppFileObject"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size());
    }
}
