package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-patrimoine-terrains.md §6.1 « Registre / copie » : les lignes écrites par le lot P7a sont connues du
 * rapatriement, parent avant enfant — StkExpense après StkItem ; BuyDocument après StkExpense, CfgTier et CfgCompany ;
 * ses trois tables compagnes après BuyDocument ; toutes avant l'audit qui les cite.
 */
class TerrainsOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    @Test
    void r6_1_frais_apresLeTerrain_avantLAudit() {
        assertThat(rang("StkExpense"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("StkItem"))
                .isGreaterThan(rang("StkExpenseType"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @Test
    void r6_1_facture_apresLeFraisLeTiersEtLaSociete_avantLAudit() {
        assertThat(rang("BuyDocument"))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("StkExpense"))
                .isGreaterThan(rang("CfgTier"))
                .isGreaterThan(rang("CfgCompany"))
                .isLessThan(rang("AdmWebHistorics"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"BuyDocumentLine", "BuyVentilationCategory", "BuyDocumentStatusHistory"})
    void r6_1_ligneCompagne_apresLaFacture_avantLAudit(String table) {
        assertThat(rang(table))
                .isLessThan(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size())
                .isGreaterThan(rang("BuyDocument"))
                .isLessThan(rang("AdmWebHistorics"));
    }
}
