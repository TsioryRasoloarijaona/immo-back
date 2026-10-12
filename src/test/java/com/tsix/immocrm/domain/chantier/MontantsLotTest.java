package com.tsix.immocrm.domain.chantier;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MontantsLotTest {

    private static final Totaux FORFAIT = Totaux.forfait(new BigDecimal("5000"), new BigDecimal("1000"),
            new BigDecimal("6000"));
    private static final Totaux AUCUNE_LIGNE = Totaux.deLignes(List.of());

    private static LigneMontant ligne(String quantite, String puHT, String puTTC) {
        return new LigneMontant(null, new BigDecimal(quantite), new BigDecimal(puHT), new BigDecimal("0.2"),
                new BigDecimal(puTTC));
    }

    @Test
    void lotAvecBordereau_montantsDepuisLesLignes_arrondisALaFin() {
        Totaux bordereau = Totaux.deLignes(List.of(ligne("10", "100", "120"), ligne("5", "200", "240"),
                ligne("2", "333.3333333333333333", "400")));

        MontantsLot m = MontantsLot.calculer(bordereau, AUCUNE_LIGNE, FORFAIT, new BigDecimal("0.1"),
                BigDecimal.ZERO, new BigDecimal("840"));

        assertAll(
                () -> assertThat(m.montantHT()).isEqualByComparingTo("2666.666667"),
                () -> assertThat(m.montantTVA()).isEqualByComparingTo("533.333333"),
                () -> assertThat(m.montantTTC()).isEqualByComparingTo("3200"),
                () -> assertThat(m.montantsDepuisBordereau()).isTrue(),
                () -> assertThat(m.retenueGarantieTheorique()).isEqualByComparingTo("320"),
                () -> assertThat(m.avancementPct()).isEqualByComparingTo("26.25"));
    }

    @Test
    void lotSansLigne_repliSurLeForfaitSaisi() {
        MontantsLot m = MontantsLot.calculer(AUCUNE_LIGNE, AUCUNE_LIGNE, FORFAIT, BigDecimal.ZERO,
                new BigDecimal("0.15"), BigDecimal.ZERO);

        assertAll(
                () -> assertThat(m.montantHT()).isEqualByComparingTo("5000"),
                () -> assertThat(m.montantTTC()).isEqualByComparingTo("6000"),
                () -> assertThat(m.montantsDepuisBordereau()).isFalse(),
                () -> assertThat(m.estimationHT()).isEqualByComparingTo("5000"),
                () -> assertThat(m.estimationDepuisBordereau()).isFalse(),
                () -> assertThat(m.avanceTheorique()).isEqualByComparingTo("900"),
                () -> assertThat(m.avancementPct()).isEqualByComparingTo("0"));
    }

    @Test
    void marcheTTCNul_avancementNull_pasDeDivisionParZero() {
        Totaux zero = Totaux.forfait(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        MontantsLot m = MontantsLot.calculer(AUCUNE_LIGNE, AUCUNE_LIGNE, zero, null, null, new BigDecimal("100"));

        assertThat(m.avancementPct()).isNull();
    }

    @Test
    void avancementNonPlafonne_auDelaDeCentPourCent() {
        MontantsLot m = MontantsLot.calculer(AUCUNE_LIGNE, AUCUNE_LIGNE, FORFAIT, null, null, new BigDecimal("9000"));

        assertThat(m.avancementPct()).isEqualByComparingTo("150");
    }

    @Test
    void estimationDepuisLeBordereauEstimatif_independanteDuMarche() {
        Totaux estimation = Totaux.deLignes(List.of(ligne("1", "1000", "1200")));

        MontantsLot m = MontantsLot.calculer(AUCUNE_LIGNE, estimation, FORFAIT, null, null, null);

        assertAll(
                () -> assertThat(m.estimationHT()).isEqualByComparingTo("1000"),
                () -> assertThat(m.estimationTVA()).isEqualByComparingTo("200"),
                () -> assertThat(m.estimationTTC()).isEqualByComparingTo("1200"),
                () -> assertThat(m.estimationDepuisBordereau()).isTrue(),
                () -> assertThat(m.montantHT()).isEqualByComparingTo("5000"));
    }

    @Test
    void ratioEnPourcentage_nullResteNull() {
        assertAll(
                () -> assertThat(Montants.enPourcentage(new BigDecimal("0.100000"))).isEqualByComparingTo("10"),
                () -> assertThat(Montants.enPourcentage(new BigDecimal("0.19"))).isEqualByComparingTo("19"),
                () -> assertThat(Montants.enPourcentage(null)).isNull());
    }
}
