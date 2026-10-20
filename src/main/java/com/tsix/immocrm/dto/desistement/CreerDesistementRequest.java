package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Demande de désistement d'un dossier de vente (brouillon, non validé).")
public record CreerDesistementRequest(
        @Schema(description = "Date de la demande du client (`ClaimDate`).", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime dateDemande,
        @Schema(description = "Observations (`Notes`) ; après « motif : », elles deviennent la cause d'annulation du dossier.")
        String observations,
        @Schema(description = "Motif (`ComSaleWithdrawalReason.PKey`), voir `GET /api/referentiels/motifs-desistement`.")
        UUID motifId
) {
}
