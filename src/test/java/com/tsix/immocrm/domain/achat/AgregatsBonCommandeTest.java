package com.tsix.immocrm.domain.achat;

import com.tsix.immocrm.domain.achat.AgregatsBonCommande.Commande;
import com.tsix.immocrm.domain.achat.AgregatsBonCommande.PieceLiee;
import com.tsix.immocrm.dto.achat.AgregatsBonCommandeDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** FEATURE-achat-bc-agregats.md cas 11 : arrondi HALF_UP sur l'agrégat final, total = Σ des lignes, ratios nuls. */
class AgregatsBonCommandeTest {

    private static BigDecimal n(String valeur) {
        return new BigDecimal(valeur);
    }

    private static PieceLiee facture(String code, String ht, String regle) {
        return new PieceLiee("INVOICE", null, code, n(ht), n(ht), n(ht), null, null, regle == null ? null : n(regle), null);
    }

    @Test
    void de_arrondiSurLaSommeEtNonParPiece() {
        Commande bc = new Commande(n("1"), n("1"), n("1"), null, null, "DV-1");

        AgregatsBonCommandeDto a = AgregatsBonCommande.de(bc, List.of(facture("F1", "0.004", null),
                facture("F2", "0.004", "0.001")));

        assertAll(
                () -> assertThat(a.factureHT()).isEqualByComparingTo("0.01"),
                () -> assertThat(a.resteALivrerHT()).isEqualByComparingTo("1.00"),
                () -> assertThat(a.avanceTauxPrevu()).isEqualByComparingTo("0"),
                () -> assertThat(a.devisCode()).isEqualTo("DV-1"),
                () -> assertThat(a.facturesCodes()).isEqualTo("F1, F2"));
    }

    @Test
    void total_sommeChampAChamp_ratiosEtCodesNuls_videNul() {
        Commande bc = new Commande(n("100"), n("120"), n("120"), n("0.2"), "REF", "DV");
        AgregatsBonCommandeDto a = AgregatsBonCommande.de(bc, List.of(facture("F1", "40", "10")));
        AgregatsBonCommandeDto b = AgregatsBonCommande.de(bc, List.of());

        AgregatsBonCommandeDto total = AgregatsBonCommande.total(List.of(a, b));

        assertAll(
                () -> assertThat(total.factureHT()).isEqualByComparingTo("40"),
                () -> assertThat(total.resteAPayerTotal()).isEqualByComparingTo("230"),
                () -> assertThat(total.avanceTauxPrevu()).isNull(),
                () -> assertThat(total.devisCode()).isNull(),
                () -> assertThat(b.devisCode()).isEqualTo("REF"),
                () -> assertThat(AgregatsBonCommande.total(List.of())).isNull());
    }
}
