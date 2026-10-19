package com.tsix.immocrm.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-article-fiche.md §9.8 : les lignes enfants d'un article écrites ou supprimées par le lot sont connues
 * du rapatriement, après {@code StkItem} (parent avant enfant), avant {@code CfgTier} et avant l'audit qui les cite.
 */
class ArticleFicheOrdreRemonteeTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"StkItemVue", "StkItemContenance", "StkHierarchyPath"})
    void r9_8_ligneDArticleConnue_apresLArticle_avantCfgTierEtLAudit(String table) {
        int rang = CollecteurLignesEcrites.rang(table);

        assertThat(rang)
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(CollecteurLignesEcrites.rang("StkItem"))
                .isLessThan(CollecteurLignesEcrites.rang("CfgTier"))
                .isLessThan(CollecteurLignesEcrites.rang("AdmWebHistorics"));
    }
}
