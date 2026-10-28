package com.tsix.immocrm.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot P9f, règle P1 (§1.1) : l'aperçu et les lettres de la Relance 2 exposent clients et montants, hors de toute portée de clé d'API. */
class ImpressionsTranchePorteeCleApiTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/tranches/7c9e6679-7425-40de-944b-e07fc1f90ae7/relances-2/apercu",
            "/api/tranches/7c9e6679-7425-40de-944b-e07fc1f90ae7/relances-2.pdf"})
    void p1_relance2_refuseeAToutesLesPortees(String chemin) {
        assertThat(PorteeCleApi.autorise(EnumSet.allOf(PorteeCleApi.class), "GET", chemin)).isFalse();
    }
}
