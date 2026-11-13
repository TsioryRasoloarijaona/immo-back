package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bordereau définitif d'un lot ; lignes vides = lot au montant forfaitaire.")
public record BordereauDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Lignes, tri par ordre.") List<LigneBordereauDto> lignes,
        @Schema(description = "Totaux des lignes.") TotauxBordereauDto totaux,
        @Schema(description = "Groupes du bordereau hors approvisionnement (arbre plat).") List<GroupeBordereauDto> groupes,
        @Schema(description = "Vrai si l'appelant peut enregistrer ce bordereau (mêmes droits que PUT …/bordereau).")
        boolean modifiable,
        @Schema(description = "TVA par défaut du lot en pourcentage (DefaultVat × 100), null si vide.", example = "20")
        BigDecimal tvaParDefaut,
        @Schema(description = "Catégorie de dépense du lot (StlCategoryId), défaut d'une nouvelle ligne.") UUID categorieParDefautId
) {

    @Schema(description = "Totaux du bordereau ; égaux au marché du lot quand il a des lignes.")
    public record TotauxBordereauDto(
            @Schema(description = "Σ quantité × prix unitaire HT.") BigDecimal totalHT,
            @Schema(description = "Σ quantité × prix unitaire HT × taux de TVA.") BigDecimal totalTVA,
            @Schema(description = "Σ quantité × prix unitaire TTC.") BigDecimal totalTTC,
            @Schema(description = "Nombre de lignes.") long nbLignes
    ) {
    }
}
