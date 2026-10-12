package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne voulue de l'onglet Frais : le serveur calcule le montant non saisi (HT ou TTC).")
public record FraisTrancheEcritureDto(
        @Schema(description = "Id d'une ligne existante de cette tranche ; absent = nouvelle ligne.")
        UUID id,
        @Schema(description = "Catégorie, voir GET /api/tranches/categories-frais. Obligatoire.")
        UUID categorieReglementId,
        @Schema(description = "Ordre (« O.D.T. », ≥ 0) ; absent = rang de la ligne dans la liste.")
        Integer ordre,
        @Schema(description = "Montant HT (≥ 0), lu quand saisiEnTTC est faux ; absent = 0.")
        BigDecimal montantHT,
        @Schema(description = "Taux de TVA en fraction, entre 0 et 1 ; absent = 0.")
        BigDecimal tauxTva,
        @Schema(description = "Montant TTC (≥ 0), lu quand saisiEnTTC est vrai ; absent = 0.")
        BigDecimal montantTTC,
        @Schema(description = "Pourcentage du prix de vente (PercentOf, ≥ 0) ; absent = 0.")
        BigDecimal pourcentageDe,
        @Schema(description = "Vrai : le TTC est saisi et le HT calculé ; faux ou absent : l'inverse.")
        Boolean saisiEnTTC
) {
}
