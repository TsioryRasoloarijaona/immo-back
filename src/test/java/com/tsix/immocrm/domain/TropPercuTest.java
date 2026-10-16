package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Formule de {@code ComSaleFolderView} (FEATURE-vente-socle.md B8). */
class TropPercuTest {

    private static TropPercu.Ligne avance(String montant, String regle) {
        return new TropPercu.Ligne(new BigDecimal(montant), new BigDecimal(regle), true,
                false, true, false, false, false, false, false);
    }

    private static TropPercu.Ligne remboursement(String montant, String regle) {
        return new TropPercu.Ligne(new BigDecimal(montant), new BigDecimal(regle), false,
                false, false, false, false, false, false, true);
    }

    @Test
    void dossierDebiteur_aucunTropPercu() {
        TropPercu.Resultat r = TropPercu.calculer(List.of(avance("1000000", "600000")),
                new BigDecimal("1000000"), BigDecimal.ZERO, false);

        assertThat(r).isEqualTo(TropPercu.Resultat.AUCUN);
    }

    @Test
    void dossierActifSurpaye_tropPercuPositif_resteEntier() {
        TropPercu.Resultat r = TropPercu.calculer(List.of(avance("1000000", "1050000")),
                new BigDecimal("1000000"), null, false);

        assertAll(
                () -> assertThat(r.tropPercu()).isEqualByComparingTo("50000"),
                () -> assertThat(r.tropPercuReste()).isEqualByComparingTo("50000"));
    }

    /** Net à payer de la liste OMRAN : Total et TotalSettled de la vue, dont le trop-perçu est le reste négatif. */
    @Test
    void bilan_totalEtTotalRegleDeLaVue() {
        TropPercu.Ligne frais = new TropPercu.Ligne(new BigDecimal("15000"), new BigDecimal("15000"), true,
                true, false, false, false, false, false, false);
        TropPercu.Bilan b = TropPercu.bilan(List.of(avance("100000", "60000"), frais),
                new BigDecimal("1000000"), new BigDecimal("5000"), false);

        assertAll(
                () -> assertThat(b.total()).isEqualByComparingTo("1020000"),
                () -> assertThat(b.totalRegle()).isEqualByComparingTo("75000"),
                () -> assertThat(b.reste()).isEqualByComparingTo("945000"));
    }

    /** Cas B de FEATURE-dossier-durcissement.md §2 : résiliation au débit non décaissée. */
    @Test
    void dossierAnnule_tropPercuEstLeRemboursementPrevu() {
        // Annulé : total = réglé − remboursement prévu (échéance au débit), donc trop-perçu = ce remboursement.
        TropPercu.Resultat r = TropPercu.calculer(
                List.of(avance("1000000", "300000"), remboursement("270000", "0")),
                new BigDecimal("1000000"), BigDecimal.ZERO, true);

        assertAll(
                () -> assertThat(r.tropPercu()).isEqualByComparingTo("270000"),
                () -> assertThat(r.tropPercuReste()).isEqualByComparingTo("270000"));
    }

    /** Ligne de remboursement au débit sans nature (ni avance, ni résiliation) : réglé signé, négatif si décaissé. */
    private static TropPercu.Ligne remboursementSimple(String montant, String regle) {
        return new TropPercu.Ligne(new BigDecimal(montant), new BigDecimal(regle), false,
                false, false, false, false, false, false, false);
    }

    @Test
    void casA_dossierActif_remboursementNonDecaisse_resteEntier() {
        TropPercu.Resultat r = TropPercu.calculer(
                List.of(avance("110000", "110000"), remboursementSimple("10000", "0")),
                new BigDecimal("100000"), BigDecimal.ZERO, false);

        assertAll(
                () -> assertThat(r.tropPercu()).isEqualByComparingTo("10000"),
                () -> assertThat(r.tropPercuReste()).isEqualByComparingTo("10000"));
    }

    @Test
    void casAPrime_dossierActif_remboursementDecaisse_resteNul() {
        TropPercu.Resultat r = TropPercu.calculer(
                List.of(avance("110000", "110000"), remboursementSimple("10000", "-10000")),
                new BigDecimal("100000"), BigDecimal.ZERO, false);

        assertAll(
                () -> assertThat(r.tropPercu()).isEqualByComparingTo("10000"),
                () -> assertThat(r.tropPercuReste()).isEqualByComparingTo("0"));
    }

    /** Vue : TotalSettled = … − CancelSettled (remboursé, positif) → TotalRest = 0, donc ni trop-perçu ni reste. */
    @Test
    void casBPrime_dossierAnnule_resiliationDecaissee_plusRienARembourser() {
        TropPercu.Ligne resiliationDecaissee = remboursement("270000", "-270000");
        List<TropPercu.Ligne> lignes = List.of(avance("1000000", "300000"), resiliationDecaissee);

        TropPercu.Resultat r = TropPercu.calculer(lignes, new BigDecimal("1000000"), BigDecimal.ZERO, true);
        TropPercu.Bilan b = TropPercu.bilan(lignes, new BigDecimal("1000000"), BigDecimal.ZERO, true);

        assertAll(
                () -> assertThat(b.totalRegle()).isEqualByComparingTo("30000"),
                () -> assertThat(r.tropPercu()).isEqualByComparingTo("0"),
                () -> assertThat(r.tropPercuReste()).isEqualByComparingTo("0"));
    }

    @Test
    void remboursementSuperieurAuTropPercu_resteJamaisNegatif() {
        TropPercu.Resultat r = TropPercu.calculer(
                List.of(avance("110000", "110000"), remboursementSimple("15000", "-15000")),
                new BigDecimal("100000"), BigDecimal.ZERO, false);

        assertThat(r.tropPercuReste()).isEqualByComparingTo("0");
    }
}
