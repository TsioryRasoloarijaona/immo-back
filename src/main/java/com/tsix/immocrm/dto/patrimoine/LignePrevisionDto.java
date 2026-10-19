package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Ligne mensuelle des prévisions de base (CfgPrevision).")
public record LignePrevisionDto(
        @Schema(description = "PKey de la ligne ; nul pour une ligne générée.", accessMode = READ_ONLY) UUID id,
        @Schema(description = "Date du mois (le 15, comme OMRAN) ; au PUT, sert si annee / mois sont absents.")
        LocalDateTime date,
        @Schema(description = "Année (Year).") Integer annee,
        @Schema(description = "Mois de 1 à 12 (Month).") Integer mois,
        @Schema(description = "Articles concrétisés ce mois (Quantity) ; absent = 0.") Integer quantiteConcretisation,
        @Schema(description = "Articles finalisés ce mois (TurnoverQuantity) ; absent = 0.") Integer quantiteFinalisation,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Versement concrétisation = quantité × valorisation × % concrétisation / nombre d'articles.",
                accessMode = READ_ONLY) BigDecimal versementConcretisation,
        @Schema(description = "Versement finalisation = quantité × valorisation × (1 − %) / nombre d'articles.",
                accessMode = READ_ONLY) BigDecimal versementFinalisation,
        @Schema(description = "Versement total = concrétisation + finalisation.", accessMode = READ_ONLY)
        BigDecimal versementTotal,
        @Schema(description = "Chiffre d'affaires TTC = quantité finalisée × valorisation / nombre d'articles.",
                accessMode = READ_ONLY) BigDecimal chiffreAffaires
) {
}
