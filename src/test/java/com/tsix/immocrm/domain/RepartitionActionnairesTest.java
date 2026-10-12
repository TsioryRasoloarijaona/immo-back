package com.tsix.immocrm.domain;

import com.tsix.immocrm.domain.RepartitionActionnaires.Parts;
import com.tsix.immocrm.domain.RepartitionActionnaires.Pourcentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code CfgCompany.SetRepartitions} et {@code Verif} (FEATURE-entites-juridiques.md §2). */
class RepartitionActionnairesTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final UUID C = UUID.randomUUID();

    private static Pourcentage ligne(UUID id, String fraction) {
        return new Pourcentage(id, new BigDecimal(fraction));
    }

    @Test
    void depuisGroupe_troncatureEntiere_etResteSurLaDerniere() {
        List<Parts> parts = RepartitionActionnaires.depuisGroupe(
                List.of(ligne(A, "0.333333"), ligne(B, "0.333333"), ligne(C, "0.333334")), 1000);

        assertThat(parts).containsExactly(new Parts(A, 330), new Parts(B, 330), new Parts(C, 340));
    }

    @Test
    void depuisGroupe_pourcentageTronqueAuCentieme_avantLeProduit() {
        List<Parts> parts = RepartitionActionnaires.depuisGroupe(List.of(ligne(A, "0.4999"), ligne(B, "0.5001")), 7);

        assertThat(parts).containsExactly(new Parts(A, 3), new Parts(B, 4));
    }

    @Test
    void depuisGroupe_uneSeuleLigne_recoitTout() {
        assertThat(RepartitionActionnaires.depuisGroupe(List.of(ligne(A, "1")), 250))
                .containsExactly(new Parts(A, 250));
    }

    @Test
    void depuisGroupe_zeroPart_toutesLesLignesAZero() {
        assertThat(RepartitionActionnaires.depuisGroupe(List.of(ligne(A, "0.6"), ligne(B, "0.4")), 0))
                .containsExactly(new Parts(A, 0), new Parts(B, 0));
    }

    @Test
    void depuisGroupe_groupeVide_aucuneLigne() {
        assertThat(RepartitionActionnaires.depuisGroupe(List.of(), 100)).isEmpty();
    }

    @Test
    void depuisGroupe_grandNombreDeParts_sansDebordement() {
        List<Parts> parts = RepartitionActionnaires.depuisGroupe(List.of(ligne(A, "0.5"), ligne(B, "0.5")),
                2_000_000_000);

        assertThat(parts).containsExactly(new Parts(A, 1_000_000_000), new Parts(B, 1_000_000_000));
    }

    @ParameterizedTest
    @CsvSource({
            "2, 100, 60, 40, ",
            "3, 100, 60, 40, " + RepartitionActionnaires.NOMBRE_ACTIONNAIRES,
            "2, 100, 60, 30, " + RepartitionActionnaires.NOMBRE_PARTS
    })
    void erreurRepartition_nombreDeLignesPuisSomme(int actionnaires, int partsTotales, int p1, int p2,
                                                   String attendu) {
        assertThat(RepartitionActionnaires.erreurRepartition(actionnaires, partsTotales, List.of(p1, p2)))
                .isEqualTo(Optional.ofNullable(attendu));
    }

    @Test
    void erreurRepartition_vide_accepteeSiZeroActionnaireEtZeroPart() {
        assertThat(RepartitionActionnaires.erreurRepartition(0, 0, List.of())).isEmpty();
    }

    @Test
    void erreurRepartition_vide_refuseeSiDesPartsSontDeclarees() {
        assertThat(RepartitionActionnaires.erreurRepartition(0, 100, List.of()))
                .contains(RepartitionActionnaires.NOMBRE_PARTS);
    }

    @Test
    void fontCentPourCent_exactement1() {
        assertThat(RepartitionActionnaires.fontCentPourCent(List.of(new BigDecimal("0.6"), new BigDecimal("0.400000"))))
                .isTrue();
    }

    @Test
    void fontCentPourCent_groupeVideRefuse() {
        assertThat(RepartitionActionnaires.fontCentPourCent(List.of())).isFalse();
    }

    @Test
    void contientUnDoublon_memeActionnaireDeuxFois() {
        assertThat(RepartitionActionnaires.contientUnDoublon(List.of(A, B, A))).isTrue();
    }
}
