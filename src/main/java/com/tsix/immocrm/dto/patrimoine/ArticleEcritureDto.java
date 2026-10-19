package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Article principal ({@code POST /api/articles}), sous-article ({@code POST /api/articles/{id}/sous-articles}) et
 * modification des deux ({@code PATCH}). Prix au m² : HT ou TTC, jamais les deux pour une même surface.
 */
@Schema(description = "Création ou modification d'un article ou d'un sous-article (StkItem). En PATCH, absent ou null "
        + "= inchangé. Taux et remises en fraction (0,2 = 20 %).")
public record ArticleEcritureDto(
        @Schema(description = "Tranche (StkHierarchyId). Obligatoire pour un article, non modifiable, interdit pour un "
                + "sous-article (celle du parent).") UUID trancheId,
        @Schema(description = "Code, unique dans la tranche (sous-articles compris). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Vocation (StkVocation), défaut VENTE, PROJET refusée. Ignorée pour un sous-article.")
        UUID vocationId,
        @Schema(description = "Catégorie nature Bâtiment ; « pour les sous-articles » selon le lot. Obligatoire.")
        UUID categorieId,
        @Schema(description = "Type, même règle que la catégorie. Obligatoire.") UUID typeId,
        @Schema(description = "Immeuble (StkBuilding). Obligatoire ; défaut = celui du parent pour un sous-article.")
        UUID immeubleId,
        @Schema(description = "Étage (StkFloor). Obligatoire.") UUID etageId,
        @Schema(description = "AreaFloor, > 0, défaut 1.") BigDecimal surfaceProvisoire,
        @Schema(description = "Area, > 0, défaut = surface provisoire.") BigDecimal surfaceDefinitive,
        @Schema(description = "ComSaleUnitCostFloorHT ; exclusif avec prixM2ProvisoireTTC.") BigDecimal prixM2ProvisoireHT,
        @Schema(description = "ComSaleUnitCostFloor ; exclusif avec prixM2ProvisoireHT.") BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "ComSaleUnitCostHT ; exclusif avec prixM2DefinitifTTC.") BigDecimal prixM2DefinitifHT,
        @Schema(description = "ComSaleUnitCost ; exclusif avec prixM2DefinitifHT.") BigDecimal prixM2DefinitifTTC,
        @Schema(description = "Vat, fraction entre 0 et 1, défaut 0.") BigDecimal tauxTva,
        @Schema(description = "TitleNumber.") String titreFoncier,
        @Schema(description = "IsTitled ; défaut vrai pour un article, faux pour un sous-article.") Boolean estTitre,
        @Schema(description = "DiscountMax, fraction.") BigDecimal remiseMax1,
        @Schema(description = "DiscountMax2, fraction.") BigDecimal remiseMax2,
        @Schema(description = "Address (500 caractères au plus).") String adresse,
        @Schema(description = "Notes (1000 caractères au plus).") String notes,
        @Schema(description = "InteriorArea, ≥ 0.") BigDecimal surfaceInterieure,
        @Schema(description = "WallsArea, ≥ 0.") BigDecimal surfaceMurs,
        @Schema(description = "CommonPartsArea, ≥ 0.") BigDecimal surfacePartiesCommunes,
        @Schema(description = "AlwaysAffected, à la création d'un sous-article seulement (défaut faux).")
        Boolean toujoursAffecte
) {
}
