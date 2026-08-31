package com.tsix.immocrm.services.dossier.document;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the French number-to-words conversion used by the "Fiche Trop Perçu" PDF (see
 * {@code FicheTropPercuPdfRenderer}) — covers the classic French irregularities (soixante-dix,
 * quatre-vingts, the "et" exceptions, cent/vingt pluralization).
 */
class NombreEnLettresFrTest {

    @Test
    void enToutesLettres_casSimples() {
        assertThat(NombreEnLettresFr.enToutesLettres(0)).isEqualTo("zéro");
        assertThat(NombreEnLettresFr.enToutesLettres(1)).isEqualTo("un");
        assertThat(NombreEnLettresFr.enToutesLettres(15)).isEqualTo("quinze");
        assertThat(NombreEnLettresFr.enToutesLettres(21)).isEqualTo("vingt et un");
        assertThat(NombreEnLettresFr.enToutesLettres(31)).isEqualTo("trente et un");
        assertThat(NombreEnLettresFr.enToutesLettres(42)).isEqualTo("quarante-deux");
    }

    @Test
    void enToutesLettres_soixanteDixEtQuatreVingt() {
        assertThat(NombreEnLettresFr.enToutesLettres(70)).isEqualTo("soixante-dix");
        assertThat(NombreEnLettresFr.enToutesLettres(71)).isEqualTo("soixante et onze"); // exception à "-dix"
        assertThat(NombreEnLettresFr.enToutesLettres(72)).isEqualTo("soixante-douze");
        assertThat(NombreEnLettresFr.enToutesLettres(79)).isEqualTo("soixante-dix-neuf");
        assertThat(NombreEnLettresFr.enToutesLettres(80)).isEqualTo("quatre-vingts"); // pluriel, rond et final
        assertThat(NombreEnLettresFr.enToutesLettres(81)).isEqualTo("quatre-vingt-un"); // pas de "et"
        assertThat(NombreEnLettresFr.enToutesLettres(90)).isEqualTo("quatre-vingt-dix");
        assertThat(NombreEnLettresFr.enToutesLettres(91)).isEqualTo("quatre-vingt-onze"); // pas de "et"
        assertThat(NombreEnLettresFr.enToutesLettres(99)).isEqualTo("quatre-vingt-dix-neuf");
    }

    @Test
    void enToutesLettres_centaines() {
        assertThat(NombreEnLettresFr.enToutesLettres(100)).isEqualTo("cent");
        assertThat(NombreEnLettresFr.enToutesLettres(101)).isEqualTo("cent un");
        assertThat(NombreEnLettresFr.enToutesLettres(180)).isEqualTo("cent quatre-vingts");
        assertThat(NombreEnLettresFr.enToutesLettres(200)).isEqualTo("deux cents"); // pluriel, rond
        assertThat(NombreEnLettresFr.enToutesLettres(250)).isEqualTo("deux cent cinquante"); // pas de 's', suivi
    }

    @Test
    void enToutesLettres_milliersEtMillions() {
        assertThat(NombreEnLettresFr.enToutesLettres(1000)).isEqualTo("mille"); // pas "un mille"
        assertThat(NombreEnLettresFr.enToutesLettres(2000)).isEqualTo("deux mille");
        assertThat(NombreEnLettresFr.enToutesLettres(80000)).isEqualTo("quatre-vingt mille"); // pas de 's', suivi de "mille"
        assertThat(NombreEnLettresFr.enToutesLettres(100000)).isEqualTo("cent mille");
        assertThat(NombreEnLettresFr.enToutesLettres(1000000)).isEqualTo("un million");
        assertThat(NombreEnLettresFr.enToutesLettres(1250000)).isEqualTo("un million deux cent cinquante mille");
    }

    @Test
    void montant_ajouteLaDeviseEtPluralise() {
        assertThat(NombreEnLettresFr.montant(BigDecimal.valueOf(50000), "dirham", "centime"))
                .isEqualTo("cinquante mille dirhams");
        assertThat(NombreEnLettresFr.montant(BigDecimal.ONE, "dirham", "centime"))
                .isEqualTo("un dirham");
        assertThat(NombreEnLettresFr.montant(new BigDecimal("1.50"), "dirham", "centime"))
                .isEqualTo("un dirham et cinquante centimes");
        assertThat(NombreEnLettresFr.montant(null, "dirham", "centime"))
                .isEqualTo("zéro dirham");
    }
}
