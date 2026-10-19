package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;

@Schema(description = "Ligne de l'onglet Frais de vente (CfgTrancheSettlementCategory).")
public record LigneFraisDto(
        @Schema(description = "PKey de la ligne ; absent au PUT = nouvelle ligne.") UUID id,
        @Schema(description = "Catégorie de règlement, obligatoire ; choix : /api/tranches/categories-frais.")
        UUID categorieReglementId,
        @Schema(description = "O.D.T. (Sorting) ; absent = rang dans la liste.") Integer ordre,
        @Schema(description = "Montant HT ; recalculé depuis le TTC si saisiEnTTC.") BigDecimal montantHT,
        @Schema(description = "Taux de TVA (fraction entre 0 et 1) ; absent = 0.") BigDecimal tauxTva,
        @Schema(description = "Montant TTC = HT × (1 + TVA) ; recalculé depuis le HT sauf si saisiEnTTC.")
        BigDecimal montantTTC,
        @Schema(description = "Pourcentage du prix de vente (PercentOf, fraction entre 0 et 1).") BigDecimal pourcentageDe,
        @Schema(description = "Vrai : le TTC est saisi et le HT calculé ; faux ou absent : l'inverse.", accessMode = WRITE_ONLY)
        Boolean saisiEnTTC,
        @Schema(description = "Code de la catégorie.", accessMode = READ_ONLY) String categorieCode,
        @Schema(description = "Description de la catégorie.", accessMode = READ_ONLY) String categorieDescription
) implements LignesTrancheDto.Ligne {
}
