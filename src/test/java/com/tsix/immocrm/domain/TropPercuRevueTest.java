package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue de FEATURE-dossier-durcissement.md §2 : une résiliation partiellement décaissée est retranchée deux fois,
 * une fois du « Net à payer réglé » (donc du trop-perçu), une seconde fois du reste à rembourser.
 */
class TropPercuRevueTest {

    private static TropPercu.Ligne avance(String montant, String regle) {
        return new TropPercu.Ligne(new BigDecimal(montant), new BigDecimal(regle), true,
                false, true, false, false, false, false, false);
    }

    private static TropPercu.Ligne resiliation(String montant, String regle) {
        return new TropPercu.Ligne(new BigDecimal(montant), new BigDecimal(regle), false,
                false, false, false, false, false, false, true);
    }

    /** Annulé : 300 000 versés, 270 000 à rendre, 100 000 déjà décaissés → il reste 170 000 à rembourser. */
    @Test
    void dossierAnnule_resiliationPartiellementDecaissee_resteARembourserEstLeSolde() {
        TropPercu.Resultat r = TropPercu.calculer(
                List.of(avance("1000000", "300000"), resiliation("270000", "-100000")),
                new BigDecimal("1000000"), BigDecimal.ZERO, true);

        assertThat(r.tropPercuReste()).isEqualByComparingTo("170000");
    }

    /**
     * « REMBOURSEMENT POUR ANNULATION » est au crédit (IsCredit = 1, IsSaleResiliation = 1, yesRemsProd) :
     * le décaissement de 100 000 ne doit pas faire naître un trop-perçu (§2 : un remboursement ne l'augmente jamais).
     */
    @Test
    void dossierAnnule_resiliationAuCreditDecaissee_neCreePasDeTropPercu() {
        TropPercu.Ligne avance = avance("1000000", "300000");
        TropPercu.Ligne resiliationAuCredit = new TropPercu.Ligne(new BigDecimal("270000"),
                new BigDecimal("-100000"), true, false, false, false, false, false, false, true);
        TropPercu.Resultat avant = TropPercu.calculer(
                List.of(avance, new TropPercu.Ligne(new BigDecimal("270000"), BigDecimal.ZERO, true,
                        false, false, false, false, false, false, true)),
                new BigDecimal("1000000"), BigDecimal.ZERO, true);
        TropPercu.Resultat apres = TropPercu.calculer(List.of(avance, resiliationAuCredit),
                new BigDecimal("1000000"), BigDecimal.ZERO, true);

        assertThat(avant.tropPercu()).isEqualByComparingTo("0");
        assertThat(apres.tropPercu()).isLessThanOrEqualTo(avant.tropPercu());
    }
}
