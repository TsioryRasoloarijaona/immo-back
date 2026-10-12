package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Taux et remises en fraction (0.2 = 20 %), comme OMRAN. */
@Schema(description = "Création (POST) d'un article principal ou modification partielle (PATCH) d'un article ou "
        + "sous-article (StkItem).")
public record ArticleEcritureDto(
        @Schema(description = "Tranche (CfgTranche.id). Obligatoire à la création, non modifiable ensuite.")
        UUID trancheId,
        @Schema(description = "Code saisi, unique dans la tranche (sous-articles compris). Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé. Obligatoire à la création.")
        String description,
        @Schema(description = "Vocation (StkVocation.id) ; défaut VENTE ; PROJET refusée. Celle du parent pour un sous-article.")
        UUID vocationId,
        @Schema(description = "Catégorie (StkItemCategory.id), nature Bâtiment, hors « Pour les sous articles ».")
        UUID categorieId,
        @Schema(description = "Type (StkItemType.id), même filtre que la catégorie.")
        UUID typeId,
        @Schema(description = "Immeuble (StkBuilding.id), obligatoire.")
        UUID immeubleId,
        @Schema(description = "Étage (StkFloor.id), obligatoire.")
        UUID etageId,
        @Schema(description = "Superficie provisoire en m² (AreaFloor), > 0, défaut 1.")
        BigDecimal surfaceProvisoire,
        @Schema(description = "Superficie définitive en m² (Area), > 0, défaut = surfaceProvisoire.")
        BigDecimal surfaceDefinitive,
        @Schema(description = "Prix du m² provisoire HT (ComSaleUnitCostFloorHT), ≥ 0.")
        BigDecimal prixM2ProvisoireHT,
        @Schema(description = "Prix du m² définitif HT (ComSaleUnitCostHT), ≥ 0.")
        BigDecimal prixM2DefinitifHT,
        @Schema(description = "TVA en fraction ∈ [0 ; 1] (Vat), défaut 0.")
        BigDecimal tauxTva,
        @Schema(description = "Numéro du titre foncier (TitleNumber).")
        String titreFoncier,
        @Schema(description = "Titré (IsTitled) ; défaut true pour un article, false pour un sous-article.")
        Boolean estTitre,
        @Schema(description = "Niveau de remise 1 (DiscountMax), fraction ∈ [0 ; 1].")
        BigDecimal remiseMax1,
        @Schema(description = "Niveau de remise 2 (DiscountMax2), fraction ∈ [0 ; 1].")
        BigDecimal remiseMax2,
        @Schema(description = "Adresse (500 caractères au plus).")
        String adresse,
        @Schema(description = "Notes (1000 caractères au plus).")
        String notes,
        @Schema(description = "Superficie intérieure en m², ≥ 0.")
        BigDecimal surfaceInterieure,
        @Schema(description = "Superficie des murs en m², ≥ 0.")
        BigDecimal surfaceMurs,
        @Schema(description = "Superficie des parties communes en m², ≥ 0.")
        BigDecimal surfacePartiesCommunes
) {
}
