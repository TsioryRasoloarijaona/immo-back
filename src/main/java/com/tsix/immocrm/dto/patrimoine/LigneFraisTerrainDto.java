package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Frais d'un terrain (StkExpense). Une ligne validée est intouchable : renvoyée telle quelle au PUT.")
public record LigneFraisTerrainDto(
        @Schema(description = "Pkey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Date du frais. Obligatoire.") LocalDate date,
        @Schema(description = "Amount, > 0. Obligatoire.") BigDecimal montant,
        @Schema(description = "Notes (1000 caractères au plus), \"\" si absentes.") String notes,
        @Schema(description = "Type de frais (StkExpenseType), choix : /api/referentiels-admin/types-frais. Obligatoire.")
        UUID typeFraisId,
        @Schema(description = "Fournisseur (CfgTier) ; absent = propriétaire du terrain, absent des deux → 400.")
        UUID fournisseurId,
        @Schema(description = "Code du type de frais.", accessMode = READ_ONLY) String typeFraisCode,
        @Schema(description = "Description du type de frais.", accessMode = READ_ONLY) String typeFraisDescription,
        @Schema(description = "Description du fournisseur.", accessMode = READ_ONLY) String fournisseurDescription,
        @Schema(description = "IsValidated : validé pour règlement.", accessMode = READ_ONLY) Boolean valide,
        @Schema(description = "Facture (BuyDocument) née de la validation, sinon null.", accessMode = READ_ONLY)
        UUID factureId,
        @Schema(description = "Code de cette facture.", accessMode = READ_ONLY) String factureCode
) implements LignesTrancheDto.Ligne {
}
