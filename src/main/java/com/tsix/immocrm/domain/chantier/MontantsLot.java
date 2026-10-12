package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;

/**
 * Seul calcul des montants d'un lot (liste, fiche, totaux projet) — voir FEATURE-projet-chantier-lecture.md §5.2.
 * Marché = Σ bordereau définitif, repli sur le forfait saisi si le lot n'a aucune ligne ; idem pour l'estimation.
 * L'avancement ignore les travaux supplémentaires (aide OMRAN, page LOTS).
 */
public record MontantsLot(
        BigDecimal montantHT,
        BigDecimal montantTVA,
        BigDecimal montantTTC,
        boolean montantsDepuisBordereau,
        BigDecimal estimationHT,
        BigDecimal estimationTVA,
        BigDecimal estimationTTC,
        boolean estimationDepuisBordereau,
        BigDecimal retenueGarantieTheorique,
        BigDecimal avanceTheorique,
        BigDecimal avancementPct) {

    public static MontantsLot calculer(Totaux bordereau, Totaux estimation, Totaux forfait,
                                       BigDecimal tauxRetenueGarantie, BigDecimal tauxAvance,
                                       BigDecimal cumulDecomptesTTC) {
        Totaux marche = bordereau.aDesLignes() ? bordereau : forfait;
        Totaux estime = estimation.aDesLignes() ? estimation : forfait;
        BigDecimal ttc = marche.ttc();
        return new MontantsLot(
                marche.ht(), marche.tva(), ttc, bordereau.aDesLignes(),
                estime.ht(), estime.tva(), estime.ttc(), estimation.aDesLignes(),
                Montants.arrondi(Montants.produit(ttc, tauxRetenueGarantie)),
                Montants.arrondi(Montants.produit(ttc, tauxAvance)),
                Montants.pourcentage(cumulDecomptesTTC, ttc));
    }
}
