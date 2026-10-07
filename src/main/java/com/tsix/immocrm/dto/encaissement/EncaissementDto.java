package com.tsix.immocrm.dto.encaissement;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneEncaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne de l'écran légataire « Encaissements Vente » : un {@code StlSettlement} lié à au moins une
 * échéance de dossier. Seules les clés demandées par {@code ?colonnes=} sont sérialisées
 * ({@link ColonneEncaissement}, liste blanche) ; sans le paramètre, les colonnes par défaut.
 */
@JsonFilter(ColonneEncaissement.FILTRE)
public record EncaissementDto(
        UUID id,
        /** {@code StlSettlement.Code} — "Référence" dans l'écran legacy (ex. n° de chèque). */
        String reference,
        /** {@code StlSettlement.Status} brut. */
        String etat,
        UUID dossierId,
        String dossierCode,
        /** "Date pièce" légataire : date de saisie du règlement. */
        LocalDateTime dateDocument,
        /** Date de l'échéance réglée. */
        LocalDateTime dateEcheance,
        /** Date de règlement effectif (encaissement du chèque…). */
        LocalDateTime dateReglement,
        UUID tierId,
        /** Tiers qui a réglé, pas forcément l'acquéreur principal. */
        String tierNom,
        /** {@code StlSettlement.Amount}. */
        BigDecimal montantBrut,
        /** Calculé : {@code montantBrut} moins les lignes déjà affectées à une échéance. */
        BigDecimal montantBrutRestant,
        /**
         * Calculé : {@code montantBrut - AmountRS (retenue source) - AmountStamp (timbre)}. Formule non
         * vérifiable sur les données réelles (retenue et timbre toujours nuls).
         */
        BigDecimal montantNet,
        /** {@code StlSettlement.IsAccounted}. */
        Boolean comptabilise,
        UUID typeId,
        String typeCode,
        String typeLibelle,
        /** {@code StlSettlement.CfgCompanyId}. */
        UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        /** {@code StlSettlement.StlAccountId}. */
        UUID compteId,
        String compteCode,
        /** {@code StlSettlement.StlDepositId} (FOURNISSEUR/CLIENT/BANQUE). */
        UUID depotId,
        String depotCode,
        UUID banqueId,
        String banqueNom,
        /** Non groupable : {@code nvarchar(max)}, que le {@code COUNT(DISTINCT)} du comptage de groupes refuse. */
        String notes,

        // Hors sélection par défaut : absents sans ?colonnes=. Voir FEATURE-colonnes-groupement-encaissements.md.

        /** {@code StlSettlement.Description}. */
        String description,
        /** {@code StlSettlement.AmountBase}. */
        BigDecimal montantBase,
        /** Tiers <em>payeur</em> ({@code TierPayerId}), rarement différent de {@code tierId}. */
        UUID tierPayeurId,
        String tierPayeurNom,
        /** {@code IsDcoumentDeliveredToCenter} : la faute de frappe est celle du schéma légataire. */
        Boolean remisAuCentre,
        /** {@code ReceiptCode}. */
        String recuCode,
        LocalDateTime dateRemiseAuCentre,
        /** {@code SlipCode}. */
        String bordereauCode,
        LocalDateTime dateBordereau,
        /** {@code UserCreate}. */
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
