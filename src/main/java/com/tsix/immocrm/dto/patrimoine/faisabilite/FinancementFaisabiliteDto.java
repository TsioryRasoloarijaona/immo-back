package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Crédit d'une étude (FSFunding, « Déblocage de crédit ») et ses remboursements.")
public record FinancementFaisabiliteDto(
        @Schema(description = "PKey ; absent = création.") UUID id,
        @Schema(description = "Description (\"\" si absente).") String description,
        @Schema(description = "Date du déblocage (obligatoire).") LocalDateTime dateDeblocage,
        @Schema(description = "Montant débloqué (≥ 0).") BigDecimal montant,
        @Schema(description = "Nombre de remboursements (≥ 0).") Integer nombreRemboursements,
        @Schema(description = "Périodicité en mois ; toute valeur < 1 devient 1.") Integer periodiciteMois,
        @Schema(description = "Période de grâce en mois (0 si absente).") Integer periodeGraceMois,
        @Schema(description = "Taux de charge (fraction ≥ 0), taux d'intérêt de chaque échéance générée.") BigDecimal tauxCharge,
        @Schema(description = "Calculé : Σ montantARegler des remboursements.") BigDecimal montantARegler,
        @Schema(description = "Calculé : Σ intérêts des remboursements.") BigDecimal totalInterets,
        @Schema(description = "Calculé : Σ bases − montant débloqué (information, non bloquant).") BigDecimal ecartBase,
        @Schema(description = "Remboursements, par date ; liste complète en écriture.")
        List<RemboursementFaisabiliteDto> remboursements) {
}
