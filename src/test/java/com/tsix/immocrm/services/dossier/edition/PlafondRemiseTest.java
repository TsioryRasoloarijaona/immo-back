package com.tsix.immocrm.services.dossier.edition;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** {@code Sale.cs:VerifDiscount} — plafonds réels de l'article du dossier APF 2025/00320 (3,70 % / 7,41 %). */
class PlafondRemiseTest {

    private static final BigDecimal MAX1 = new BigDecimal("0.037037");
    private static final BigDecimal MAX2 = new BigDecimal("0.074074");

    @Test
    void premierNiveau_auDelaDuPlafond1_messageOmran() {
        assertThat(PlafondRemise.depassement(new BigDecimal("0.05"), MAX1, MAX2, PlafondRemise.Niveau.PREMIER))
                .contains("Vous ne pouvez pas dépasser le premier niveau de remise pour cet article (3,70 %) !");
    }

    @Test
    void premierNiveau_egalAuPlafondArrondiA4Decimales_accepte() {
        assertThat(PlafondRemise.depassement(new BigDecimal("0.03704"), MAX1, MAX2, PlafondRemise.Niveau.PREMIER))
                .isEmpty();
    }

    @Test
    void deuxiemeNiveau_entreLesDeuxPlafonds_accepte_auDela_messageOmran() {
        assertAll(
                () -> assertThat(PlafondRemise.depassement(new BigDecimal("0.05"), MAX1, MAX2,
                        PlafondRemise.Niveau.DEUXIEME)).isEmpty(),
                () -> assertThat(PlafondRemise.depassement(new BigDecimal("0.298246"), MAX1, MAX2,
                        PlafondRemise.Niveau.DEUXIEME))
                        .contains("Vous ne pouvez pas dépasser le deuxième niveau de remise pour cet article (7,41 %) !"));
    }

    @Test
    void sansDroit_touteRemiseRefusee() {
        assertAll(
                () -> assertThat(PlafondRemise.depassement(new BigDecimal("0.01"), MAX1, MAX2,
                        PlafondRemise.Niveau.AUCUN))
                        .contains("Vous ne pouvez pas appliquer une remise pour cet article (7,41 %) !"),
                () -> assertThat(PlafondRemise.depassement(BigDecimal.ZERO, MAX1, MAX2, PlafondRemise.Niveau.AUCUN))
                        .isEmpty());
    }

    @Test
    void verrouillee_articleSansPremierPlafond_saufAuDeuxiemeNiveau() {
        assertAll(
                () -> assertThat(PlafondRemise.verrouillee(BigDecimal.ZERO, PlafondRemise.Niveau.PREMIER)).isTrue(),
                () -> assertThat(PlafondRemise.verrouillee(null, PlafondRemise.Niveau.AUCUN)).isTrue(),
                () -> assertThat(PlafondRemise.verrouillee(BigDecimal.ZERO, PlafondRemise.Niveau.DEUXIEME)).isFalse(),
                () -> assertThat(PlafondRemise.verrouillee(MAX1, PlafondRemise.Niveau.PREMIER)).isFalse());
    }

    @Test
    void niveauDuRole_selonLesDroitsOmranMesures() {
        assertAll(
                () -> assertThat(PlafondRemise.niveauDe(List.of("Administrateur"))).isEqualTo(PlafondRemise.Niveau.DEUXIEME),
                () -> assertThat(PlafondRemise.niveauDe(List.of("Commercial"))).isEqualTo(PlafondRemise.Niveau.PREMIER),
                () -> assertThat(PlafondRemise.niveauDe(List.of("Standardiste"))).isEqualTo(PlafondRemise.Niveau.AUCUN));
    }
}
