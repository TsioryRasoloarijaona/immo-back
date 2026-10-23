package com.tsix.immocrm.services.rapport.vente.flux;

import java.math.BigDecimal;
import java.util.List;

/**
 * Données hors grille d'un flux (FEATURE-tdb-vente-rapports-flux.md §2, avertissements a–d) : dossiers sans statut,
 * règlements sans aucune date, paramètres {@code AdmParamsGuid} de seuil absents, prévu non saisi.
 */
public record AnomaliesFlux(long dossiersSansStatut, ReglementsSansDate reglementsSansDate,
                            List<String> parametresAbsents, boolean prevuNonSaisi) {

    public static final AnomaliesFlux AUCUNE = new AnomaliesFlux(0, ReglementsSansDate.AUCUN, List.of(), false);

    public AnomaliesFlux {
        reglementsSansDate = reglementsSansDate == null ? ReglementsSansDate.AUCUN : reglementsSansDate;
        parametresAbsents = parametresAbsents == null ? List.of() : List.copyOf(parametresAbsents);
    }

    /** Lignes de règlement comptées mais sans date de règlement ni d'échéance : nombre et montant signé. */
    public record ReglementsSansDate(long nombre, BigDecimal montant) {

        public static final ReglementsSansDate AUCUN = new ReglementsSansDate(0, BigDecimal.ZERO);
    }
}
