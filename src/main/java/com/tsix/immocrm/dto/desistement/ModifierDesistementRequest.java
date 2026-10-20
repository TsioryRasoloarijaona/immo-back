package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Champ absent ou {@code null} = inchangé. */
@Schema(description = "Modification d'un désistement ; un champ absent reste inchangé.")
public record ModifierDesistementRequest(
        @Schema(description = "Observations (`Notes`).")
        String observations,
        @Schema(description = "Motif (`ComSaleWithdrawalReason.PKey`).")
        UUID motifId,
        @Schema(description = "Date de la demande (`ClaimDate`).")
        LocalDateTime dateDemande,
        @Schema(description = "Date de décision (`DecisionDate`).")
        LocalDateTime dateDecision,
        @Schema(description = "Date de légalisation : la poser rend le désistement légalisé, plus rien n'est modifiable ensuite.")
        LocalDateTime dateLegalisation,
        @Schema(description = "Remise sur la pénalité, en fraction (0,5 = 50 %). Brouillon seulement.", example = "0.5")
        BigDecimal remise,
        @Schema(description = "Pénalité après remise saisie : la remise est recalculée. Brouillon seulement.", example = "50000")
        BigDecimal penaliteApresRemise,
        @Schema(description = "Type de pièce du remboursement (`StlSettlementType.PKey`).")
        UUID typeReglementId,
        @Schema(description = "Référence du paiement (`Reference`).", example = "VIR-2026-001")
        String reference,
        @Schema(description = "Banque (`CfgTier.PKey`).")
        UUID banqueId,
        @Schema(description = "Contact à rembourser (`CfgTier.PKey`) ; par défaut le client du dossier.")
        UUID contactId
) {
}
