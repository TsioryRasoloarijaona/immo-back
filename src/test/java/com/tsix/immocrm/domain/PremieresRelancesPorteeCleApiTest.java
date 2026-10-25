package com.tsix.immocrm.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot P9d : l'aperçu et les lettres des relances exposent clients et restes dus, hors de toute portée de clé d'API. */
class PremieresRelancesPorteeCleApiTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/tranches/7c9e6679-7425-40de-944b-e07fc1f90ae7/premieres-relances/apercu",
            "/api/tranches/7c9e6679-7425-40de-944b-e07fc1f90ae7/premieres-relances.pdf"})
    void lecturesDesRelances_refuseesAToutesLesPortees(String chemin) {
        assertThat(PorteeCleApi.autorise(EnumSet.allOf(PorteeCleApi.class), "GET", chemin)).isFalse();
    }
}
