package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Montants d'un désistement calculés à l'instant, sans rien écrire (FEATURE-desistements.md R3-R5).")
public record SimulationDesistementDto(
        @Schema(description = "Prix de vente du dossier (`ComFolder.Price`).")
        BigDecimal prixVente,
        @Schema(description = "Total réglé sur l'avance, le crédit et l'autofinancement (décaissements déduits).")
        BigDecimal totalRegle,
        @Schema(description = "Taux de pénalité de la tranche, en fraction (0,10 = 10 %).")
        BigDecimal taux,
        @Schema(description = "Vrai : pénalité calculée sur le prix de vente ; faux : sur le total réglé.")
        boolean basePrix,
        @Schema(description = "Remise sur la pénalité en fraction : celle de la tranche pour un ancien client, sinon 0.")
        BigDecimal remise,
        @Schema(description = "Pénalité après remise = taux × base × (1 − remise), au centime.")
        BigDecimal penaliteApresRemise,
        @Schema(description = "Reste dû des frais « toujours payés » (frais, pénalités, modifications au crédit).")
        BigDecimal resteFraisARegler,
        @Schema(description = "À rembourser = max(0, total réglé − pénalité après remise − reste frais à régler).")
        BigDecimal aRembourser
) {
}
