package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.chantier.CumulsDecomptes;
import com.tsix.immocrm.domain.chantier.MontantsLot;
import com.tsix.immocrm.domain.chantier.SoldeFacture;
import com.tsix.immocrm.domain.chantier.Totaux;

/** Tout l'argent d'un lot : montants recalculés, cumuls stockés des décomptes, solde des factures. */
public record LotChiffre(MontantsLot montants, Totaux forfait, CumulsDecomptes cumuls, SoldeFacture solde) {
}
