package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Mois de la courbe de trésorerie : montants du mois par type et solde cumulé.")
public record MoisTresorerieDto(
        @Schema(description = "Premier jour du mois.") LocalDate mois,
        @Schema(description = "Concrétisation (crédit).") BigDecimal concretisation,
        @Schema(description = "Finalisation (crédit).") BigDecimal finalisation,
        @Schema(description = "Dépense d'exécution (débit).") BigDecimal depense,
        @Schema(description = "Retenue de garantie (débit).") BigDecimal garantie,
        @Schema(description = "Déblocages de crédit (crédit) ; 0 sans financement.") BigDecimal financement,
        @Schema(description = "Remboursements (débit) ; 0 sans financement.") BigDecimal remboursement,
        @Schema(description = "Dépenses indivises datées (débit).") BigDecimal depenseProjet,
        @Schema(description = "Solde cumulé : Σ crédits − Σ débits jusqu'à ce mois inclus.") BigDecimal solde,
        @Schema(description = "Crédits et débits du mois par tranche de l'étude (vide sans tranche).")
        List<MontantTrancheDto> tranches) {
}
