package com.tsix.immocrm.dto.patrimoine;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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

        // -- Ajouts de l'écriture (FEATURE-patrimoine-ecriture.md §10, §14.7) --
        @Schema(description = "AreaFloor : surface provisoire (area = définitive).") BigDecimal surfaceProvisoire,
        BigDecimal prixM2ProvisoireHT,
        BigDecimal prixM2ProvisoireTTC,
        BigDecimal prixM2DefinitifHT,
        BigDecimal prixM2DefinitifTTC,
        @Schema(description = "SalePriceFloorHT : article seul.") BigDecimal prixArticleProvisoireHT,
        @Schema(description = "SalePriceFloor : article seul.") BigDecimal prixArticleProvisoireTTC,
        @Schema(description = "SalePriceHT : article seul.") BigDecimal prixArticleDefinitifHT,
        @Schema(description = "SalePrice : article seul.") BigDecimal prixArticleDefinitifTTC,
        @Schema(description = "ComSalePriceFloorHT : article + sous-articles rattachés.") BigDecimal prixVenteProvisoireHT,
        @Schema(description = "ComSalePriceFloor : article + sous-articles rattachés.") BigDecimal prixVenteProvisoireTTC,
        @Schema(description = "ComSalePriceHT : article + sous-articles rattachés.") BigDecimal prixVenteDefinitifHT,
        @Schema(description = "ComSalePrice : article + sous-articles rattachés.") BigDecimal prixVenteDefinitifTTC,
        BigDecimal remiseMax1,
        BigDecimal remiseMax2,
        Boolean estTitre,
        @Schema(description = "AlwaysAffected : le sous-article suit son article dans la vente.") Boolean toujoursAffecte,
        @Schema(description = "IsUnderItem : lot annexe (parking, jardin…), rattaché ou libre.") Boolean estSousArticle,
        @Schema(description = "ComFolderId ou RntFolderId renseigné, le sien ou celui de son article : prix, surfaces "
                + "et classification ne se modifient plus.") boolean occupe,
        UUID vocationId,
        String vocationCode,
        String vocationLibelle,
        String notes,
        BigDecimal surfaceInterieure,
        BigDecimal surfaceMurs,
        BigDecimal surfacePartiesCommunes,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe,
        @JsonUnwrapped FicheArticleDto fiche
) {
}
