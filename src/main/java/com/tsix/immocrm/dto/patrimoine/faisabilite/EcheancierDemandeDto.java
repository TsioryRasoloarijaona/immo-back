package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Paramètres du bouton « Générer » de l'échéancier d'un crédit (rien n'est écrit).")
public record EcheancierDemandeDto(
        @Schema(description = "Date du déblocage (obligatoire).") LocalDateTime dateDeblocage,
        @Schema(description = "Montant débloqué (≥ 0).") BigDecimal montant,
        @Schema(description = "Nombre de remboursements (> 0).") Integer nombreRemboursements,
        @Schema(description = "Périodicité en mois ; < 1 devient 1.") Integer periodiciteMois,
        @Schema(description = "Période de grâce en mois (0 si absente).") Integer periodeGraceMois,
        @Schema(description = "Taux de charge de chaque échéance (fraction ≥ 0).") BigDecimal tauxCharge) {
}
