package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.chantier.SoldeFacture;

import java.math.BigDecimal;
import java.util.Collection;

/** Totaux d'un projet = somme des mêmes grandeurs que chaque lot ({@link ChiffrageLots}) ; pas de seconde formule. */
record TotauxProjet(long nbLots, BigDecimal marchesHT, BigDecimal marchesTTC, long nbDecomptes,
                    BigDecimal decomptesTTC, SoldeFacture solde) {

    static TotauxProjet de(Collection<LotChiffre> lots) {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal ttc = BigDecimal.ZERO;
        BigDecimal decomptesTTC = BigDecimal.ZERO;
        long nbDecomptes = 0;
        for (LotChiffre lot : lots) {
            ht = ht.add(lot.montants().montantHT());
            ttc = ttc.add(lot.montants().montantTTC());
            decomptesTTC = decomptesTTC.add(lot.cumuls().ttc());
            nbDecomptes += lot.cumuls().nombre();
        }
        return new TotauxProjet(lots.size(), ht, ttc, nbDecomptes, decomptesTTC,
                SoldeFacture.somme(lots.stream().map(LotChiffre::solde).toList()));
    }
}
