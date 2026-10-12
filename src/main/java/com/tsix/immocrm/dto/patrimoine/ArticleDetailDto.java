package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Marche aussi pour un sous-article (même table StkItem) : tranche et parent toujours renseignés. */
public record ArticleDetailDto(
        UUID id,
        String code,
        String description,
        String adresse,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        // Bruts, sans repli : le prix de référence (repli sur le plancher) est prixSuggere.
        BigDecimal salePriceFloor,
        BigDecimal comSalePriceFloor,
        StatutArticle statut,
        UUID trancheId,
        String trancheCode,
        String trancheDescription,
        UUID lotParentId,
        String lotParentCode,
        String lotParentDescription,
        // Tous statuts confondus.
        List<ArticleListItemDto> sousLots,
        String titreFoncier,
        // StkItemCategory (pas StkKind, voir nature*).
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        // StkItemType.
        UUID typeId,
        String typeCode,
        String typeLibelle,
        // StkItem.ComFolderId ; dossierStatut = StrStatus.Code brut, comme DossierDetailDto.statut.
        UUID dossierId,
        String dossierCode,
        String dossierStatut,
        UUID clientId,
        String clientNom,
        // StkBuilding.
        UUID immeubleId,
        String immeubleCode,
        String immeubleLibelle,
        // StkFloor : un référentiel, pas une colonne libre.
        UUID etageId,
        String etageCode,
        String etageLibelle,
        // StkKind : nature physique du bien (ex. TER), distincte de Catégorie/Type.
        UUID natureId,
        String natureCode,
        String natureLibelle,
        // StkItem.Vat, ratio brut (0.20 = 20 %).
        BigDecimal tauxTVA,
        // StkPresentationType, propre à chaque tranche.
        UUID presentationTypeId,
        String presentationTypeCode,
        String presentationTypeLibelle,
        BigDecimal superficieSousArticles,
        @Schema(description = "Prix de référence TTC : comSalePrice s'il est > 0, sinon comSalePriceFloor s'il est > 0, sinon null.",
                nullable = true)
        BigDecimal prixSuggere,
        @Schema(description = "Vrai quand prixSuggere est le prix plancher (prix commercial à 0).")
        boolean prixSuggereEstPlancher,

        // -- Saisies et prix stockés, mêmes noms que ArticleEcritureDto (FEATURE-patrimoine-ecriture.md §10) --
        @Schema(description = "Superficie provisoire en m² (AreaFloor) ; la définitive est `area`.")
        BigDecimal surfaceProvisoire,
        @Schema(description = "Prix du m² provisoire HT (ComSaleUnitCostFloorHT).")
        BigDecimal prixM2ProvisoireHT,
        @Schema(description = "Prix du m² provisoire TTC (ComSaleUnitCostFloor), calculé.")
        BigDecimal prixM2ProvisoireTTC,
        @Schema(description = "Prix du m² définitif HT (ComSaleUnitCostHT).")
        BigDecimal prixM2DefinitifHT,
        @Schema(description = "Prix du m² définitif TTC (ComSaleUnitCost), calculé.")
        BigDecimal prixM2DefinitifTTC,
        @Schema(description = "Prix de l'article seul, provisoire HT (SalePriceFloorHT).")
        BigDecimal prixArticleProvisoireHT,
        @Schema(description = "Prix de l'article seul, provisoire TTC (SalePriceFloor).")
        BigDecimal prixArticleProvisoireTTC,
        @Schema(description = "Prix de l'article seul, définitif HT (SalePriceHT).")
        BigDecimal prixArticleDefinitifHT,
        @Schema(description = "Prix de l'article seul, définitif TTC (SalePrice).")
        BigDecimal prixArticleDefinitifTTC,
        @Schema(description = "Prix de vente provisoire HT = article + sous-articles rattachés (ComSalePriceFloorHT).")
        BigDecimal prixVenteProvisoireHT,
        @Schema(description = "Prix de vente définitif HT = article + sous-articles rattachés (ComSalePriceHT).")
        BigDecimal prixVenteDefinitifHT,
        @Schema(description = "TVA en fraction (Vat), même valeur que tauxTVA.")
        BigDecimal tauxTva,
        @Schema(description = "Niveau de remise 1 (DiscountMax), fraction.")
        BigDecimal remiseMax1,
        @Schema(description = "Niveau de remise 2 (DiscountMax2), fraction.")
        BigDecimal remiseMax2,
        @Schema(description = "Titré (IsTitled).")
        Boolean estTitre,
        @Schema(description = "Toujours affecté à son article (AlwaysAffected).")
        Boolean toujoursAffecte,
        @Schema(description = "Lot annexe (IsUnderItem) ; peut être libre, sans parent.")
        Boolean estSousArticle,
        @Schema(description = "Affecté à un dossier de vente ou de location, ou rattaché à un article qui l'est.")
        boolean occupe,
        @Schema(description = "Vocation (StkVocation.id).")
        UUID vocationId,
        @Schema(description = "Libellé de la vocation.")
        String vocationLibelle,
        @Schema(description = "Notes (StkItem.Notes).")
        String notes,
        @Schema(description = "Superficie intérieure en m² (InteriorArea).")
        BigDecimal surfaceInterieure,
        @Schema(description = "Superficie des murs en m² (WallsArea).")
        BigDecimal surfaceMurs,
        @Schema(description = "Superficie des parties communes en m² (CommonPartsArea).")
        BigDecimal surfacePartiesCommunes,
        @Schema(description = "Auteur de la création (UserCreate).")
        String creePar,
        @Schema(description = "Date de création (UserCreateDateTime).")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification (UserUpdate).")
        String modifiePar,
        @Schema(description = "Date de la dernière modification (UserUpdateDateTime).")
        LocalDateTime modifieLe
) {
}
