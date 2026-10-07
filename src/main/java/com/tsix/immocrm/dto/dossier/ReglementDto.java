package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Un {@code StlSettlement} vu depuis sa ligne ({@code StlSettlementLine}) sur une échéance. */
public record ReglementDto(
        /** {@code StlSettlement.PKey} : le {@code reglementId} de {@code GET .../documents/recu}, jamais un id de ligne. */
        UUID id,
        UUID echeanceId,
        /** {@code StlSettlement.Code} (n° de chèque…). */
        String reference,
        LocalDateTime dateDocument,
        /** {@code StlSettlement.EcheanceDate}. */
        LocalDateTime dateEcheance,
        /** {@code StlSettlement.SettlementDate} : renseignée si et seulement si {@code statut} vaut POINTED
         * (voir FEATURE-reglement-statut-preuve.md §2). */
        LocalDateTime dateReglement,
        /** Part affectée à cette échéance ({@code StlSettlementLine.Amount}), pas le montant total du règlement. */
        BigDecimal montant,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        /** N'a de sens que si le type l'exige ({@code banqueObligatoire} des types de règlement). */
        UUID banqueId,
        String banqueNom,
        /** {@code StlSettlement.TierId} : qui a réglé, pas forcément l'acquéreur principal. */
        UUID payeurId,
        String payeurNom,
        String notes,
        /** IN_WORK (saisi), POINTED (encaissé) ou CANCELED ; {@code null} sur d'anciennes lignes légataires.
         * Un règlement créé par l'API naît IN_WORK. */
        String statut
) {
}
