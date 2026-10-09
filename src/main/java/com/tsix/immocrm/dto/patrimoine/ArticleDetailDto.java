package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
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
        boolean prixSuggereEstPlancher
) {
}
