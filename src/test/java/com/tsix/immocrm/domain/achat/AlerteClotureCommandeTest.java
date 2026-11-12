package com.tsix.immocrm.domain.achat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FEATURE-achat-liste-parite.md E3 : table de vérité de « À clôturer ». */
class AlerteClotureCommandeTest {

    private static final String SUCCES = "D572CE4B-A4DD-4F38-8998-359EAB5A9FCA";
    private static final String ECHEC = "22B7D73E-2A8F-4A74-839D-29F1DE329B97";
    private static final String ANNULATION = "A9E040DB-533F-4744-AE9A-1425793ADEA0";
    private static final String INTERMEDIAIRE = "B53E0A4F-3D67-4BF9-8C0E-B24155865FD6";

    @ParameterizedTest(name = "{0} HT {1} / factures {2}, état {3} → {4}")
    @CsvSource(nullValues = "NULL", value = {
            "ORDER, 1000, 1000, " + INTERMEDIAIRE + ", true",
            "ORDER, 1000, 1200, " + INTERMEDIAIRE + ", true",
            "ORDER, 1000, 999.99, " + INTERMEDIAIRE + ", false",
            "ORDER, 0, 0, " + INTERMEDIAIRE + ", true",
            "ORDER, 1000, 1000, " + ANNULATION + ", true",
            "ORDER, 1000, 1000, " + SUCCES + ", false",
            "ORDER, 1000, 1000, " + ECHEC + ", false",
            "ORDER, 1000, 1000, NULL, false",
            "ORDER, NULL, 1000, " + INTERMEDIAIRE + ", false",
            "INVOICE, 1000, 1000, " + INTERMEDIAIRE + ", false",
            "QUOTE, 0, 0, " + INTERMEDIAIRE + ", false"})
    void aCloturer_tableDeVerite(String nature, BigDecimal ht, BigDecimal htFactures, String etat, boolean attendu) {
        UUID typeEtat = etat == null ? null : UUID.fromString(etat);

        boolean resultat = AlerteClotureCommande.aCloturer(nature, ht, htFactures, typeEtat);

        assertThat(resultat).isEqualTo(attendu);
    }
}
