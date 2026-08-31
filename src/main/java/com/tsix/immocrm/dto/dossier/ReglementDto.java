package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for {@code POST .../reglements} (creation) and {@code GET .../reglements} (historique
 * complet d'une échéance) — backed by {@code StlSettlement}/{@code StlSettlementLine}.
 */
public record ReglementDto(
        /** {@code StlSettlement.PKey} — c'est cet id qu'il faut envoyer tel quel comme {@code reglementId}
         * à {@code GET .../documents/recu}, jamais un id de ligne d'échéancier. */
        UUID id,
        UUID echeanceId,
        /** {@code StlSettlement.Code} — référence/numéro du règlement (ex. n° de chèque), quand renseignée. */
        String reference,
        LocalDateTime dateDocument,
        /** {@code StlSettlement.EcheanceDate} — "date d'échéance" du règlement (voir {@code CreerReglementRequest}). */
        LocalDateTime dateEcheance,
        /** {@code StlSettlement.SettlementDate} — "date de règlement effectif" (encaissement réel), souvent absente. */
        LocalDateTime dateReglement,
        /** Part de ce règlement affectée à cette échéance ({@code StlSettlementLine.Amount}) — peut être
         * inférieur au montant total du règlement si celui-ci est réparti sur plusieurs échéances. */
        BigDecimal montant,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        /** {@code StlSettlement.CfgBankId} — n'a de sens que si le type de règlement le requiert (voir `GET /api/referentiels/types-reglement`.banqueObligatoire). */
        UUID banqueId,
        String banqueNom,
        /** {@code StlSettlement.TierId} — le tiers qui a effectivement réglé ("réglé par", même terme que la Fiche de réservation), peut différer de l'acquéreur principal (co-acquéreur, tiers payeur...). */
        UUID payeurId,
        String payeurNom,
        String notes
) {
}
