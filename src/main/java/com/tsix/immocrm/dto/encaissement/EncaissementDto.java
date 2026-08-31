package com.tsix.immocrm.dto.encaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for {@code GET /api/encaissements} — reproduit l'écran legacy "Encaissements Vente"
 * (module Vente → onglet "Encaissements Vente"). Backed by {@code StlSettlement}, restreint aux
 * règlements liés à au moins une échéance de Dossier de vente (via {@code StlSettlementLine.ComDocumentId}
 * — un {@code StlSettlement} peut en théorie aussi servir des achats/locations, hors périmètre ici).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc — dérivé directement de
 * {@code StlSettlement} et de l'écran "Encaissements Vente" observé dans OMRAN Estate.
 */
public record EncaissementDto(
        UUID id,
        /** {@code StlSettlement.Code} — "Référence" dans l'écran legacy (ex. n° de chèque). */
        String reference,
        /** {@code StlSettlement.Status} brut ("IN_WORK"/"POINTED"/"CANCELED" confirmés sur données réelles ; "UNPAID"/"LOST" existent comme filtres dans l'écran legacy mais aucune ligne réelle ne les porte à ce jour — voir le guide front). */
        String etat,
        UUID dossierId,
        String dossierCode,
        /** "Date p[ièce]" — date de saisie du règlement. */
        LocalDateTime dateDocument,
        /** "Date éch[éance]" — date de l'échéance réglée. */
        LocalDateTime dateEcheance,
        /** Date de règlement effectif (encaissement du chèque, etc.). */
        LocalDateTime dateReglement,
        UUID tierId,
        /** "Tier" dans l'écran legacy — le tiers qui a réglé (peut différer de l'acquéreur principal). */
        String tierNom,
        /** "Montant brut" — {@code StlSettlement.Amount}. */
        BigDecimal montantBrut,
        /** "Montant brut rest[ant]" — calculé : {@code montantBrut - somme des lignes déjà affectées à une échéance}. */
        BigDecimal montantBrutRestant,
        /**
         * "Montant net" — calculé : {@code montantBrut - AmountRS (retenue source) - AmountStamp
         * (droit de timbre)}. Formule non confirmable sur données réelles (AmountRS/AmountStamp
         * valent toujours 0 dans yesRems à ce jour) — voir le guide front.
         */
        BigDecimal montantNet,
        /** "Comptabilisé" — {@code StlSettlement.IsAccounted}. */
        Boolean comptabilise,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        /** "Entité" — {@code StlSettlement.CfgCompanyId}. */
        UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        /** "Compte" — {@code StlSettlement.StlAccountId}. */
        UUID compteId,
        String compteCode,
        /** "Dépôt" — {@code StlSettlement.StlDepositId} (FOURNISSEUR/CLIENT/BANQUE). */
        UUID depotId,
        String depotCode,
        UUID banqueId,
        String banqueNom,
        String notes
) {
}
