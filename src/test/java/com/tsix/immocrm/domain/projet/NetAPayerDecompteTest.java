package com.tsix.immocrm.domain.projet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class NetAPayerDecompteTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void formule_soustraitAvancesEtRetenues_ajouteExtrasProrataStockFluctuations() {
        NetAPayerDecompte r = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(d("1000"), d("100"), d("50"), d("70"),
                d("30"), d("20"), d("10"), d("5"), d("2.5"), d("787.5")));

        assertAll(
                () -> assertThat(r.recalcule()).isEqualByComparingTo("787.50"),
                () -> assertThat(r.ecart()).isEqualByComparingTo("0"),
                () -> assertThat(r.significatif()).isFalse());
    }

    @Test
    void termesNulsValentZero_netStockeNulDonneEcartNegatif() {
        NetAPayerDecompte r = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(d("100"), null, null, d("10"),
                null, null, null, null, null, null));

        assertAll(
                () -> assertThat(r.recalcule()).isEqualByComparingTo("90"),
                () -> assertThat(r.ecart()).isEqualByComparingTo("-90"),
                () -> assertThat(r.significatif()).isTrue());
    }

    @Test
    void seuil_ecartDeDeuxCentimesToleré_auDelaSignificatif() {
        NetAPayerDecompte tolere = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(d("100.004"), null, null, null,
                null, null, null, null, null, d("100.02")));
        NetAPayerDecompte signale = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(d("100"), null, null, null,
                null, null, null, null, null, d("100.03")));

        assertAll(
                () -> assertThat(tolere.recalcule()).isEqualByComparingTo("100.00"),
                () -> assertThat(tolere.significatif()).isFalse(),
                () -> assertThat(signale.significatif()).isTrue());
    }
}
