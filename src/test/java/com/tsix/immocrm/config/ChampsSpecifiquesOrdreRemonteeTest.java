package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot P9d — FEATURE-patrimoine-import-export.md §4.2 : {@code EcrituresChampsSpecifiques} (rang 1700) fait connaître au
 * rapatriement les 4 tables, dans l'ordre Entity, DataType, Field, Value ; les écritures des relances ({@code ComFolder},
 * {@code ComActionMessage}, {@code AdmAlertData}) sont déjà connues.
 */
class ChampsSpecifiquesOrdreRemonteeTest {

    private static int rang(String table) {
        return CollecteurLignesEcrites.rang(table);
    }

    private static int inconnue() {
        return CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES.size();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"AdmSpecificFieldEntity", "AdmSpecificFieldDataType", "AdmSpecificField",
            "AdmSpecificFieldValue", "ComFolder", "ComActionMessage", "AdmAlertData"})
    void tableEcrite_connue(String table) {
        assertThat(rang(table)).isLessThan(inconnue());
    }

    @Test
    void ordre_entitePuisTypePuisChampPuisValeur() {
        assertThat(rang("AdmSpecificFieldEntity")).isLessThan(rang("AdmSpecificFieldDataType"));
        assertThat(rang("AdmSpecificFieldDataType")).isLessThan(rang("AdmSpecificField"));
        assertThat(rang("AdmSpecificField")).isLessThan(rang("AdmSpecificFieldValue"));
    }
}
