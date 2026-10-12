package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;
import java.util.Collection;

/** Trio HT / TVA / TTC d'un bordereau (somme de lignes, arrondie à la fin) ou d'un forfait saisi. */
public record Totaux(int nbLignes, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {

    public static Totaux deLignes(Collection<LigneMontant> lignes) {
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal tva = BigDecimal.ZERO;
        BigDecimal ttc = BigDecimal.ZERO;
        for (LigneMontant ligne : lignes) {
            ht = ht.add(ligne.totalHT());
            tva = tva.add(ligne.totalTVA());
            ttc = ttc.add(ligne.totalTTC());
        }
        return new Totaux(lignes.size(), Montants.arrondi(ht), Montants.arrondi(tva), Montants.arrondi(ttc));
    }

    /** Montants saisis à la main ({@code EstimatedPrice*Manual}) : aucune ligne. */
    public static Totaux forfait(BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
        return new Totaux(0, Montants.arrondi(ht), Montants.arrondi(tva), Montants.arrondi(ttc));
    }

    public boolean aDesLignes() {
        return nbLignes > 0;
    }
}
