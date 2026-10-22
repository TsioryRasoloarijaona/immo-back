package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Remboursement d'un crédit (FSReimbursement). Si taux et intérêts sont envoyés, le taux gagne ; "
        + "montantARegler n'est jamais saisi.")
public record RemboursementFaisabiliteDto(
        @Schema(description = "PKey ; absent = création.") UUID id,
        @Schema(description = "Date du remboursement (obligatoire).") LocalDateTime date,
        @Schema(description = "Montant de base (≥ 0).") BigDecimal montantBase,
        @Schema(description = "Taux d'intérêt de l'échéance (fraction ≥ 0), appliqué à la base.") BigDecimal tauxInteret,
        @Schema(description = "Intérêts = base × taux ; saisis seuls, ils recalculent le taux.") BigDecimal montantInterets,
        @Schema(description = "Calculé : base + intérêts (ignoré en écriture).") BigDecimal montantARegler) {
}
