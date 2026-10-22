package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ligne de trésorerie (StkFSPrevisionLine) : un débit ou un crédit daté.")
public record LigneTresorerieDto(
        @Schema(description = "Date de la ligne.") LocalDateTime date,
        @Schema(description = "PrevisionType : Concretisation, Finalization, Expense, ExpenseBackGuarantee, Funding, "
                + "Refunding, ProjectExpense.") String type,
        @Schema(description = "Libellé français du type.") String libelle,
        @Schema(description = "Montant au débit (0 pour un crédit).") BigDecimal debit,
        @Schema(description = "Montant au crédit (0 pour un débit).") BigDecimal credit,
        @Schema(description = "Code de la tranche de l'étude, null sans tranche.") String trancheCode) {
}
