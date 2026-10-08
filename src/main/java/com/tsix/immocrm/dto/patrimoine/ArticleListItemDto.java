package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.UUID;

public record ArticleListItemDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        // comSalePrice peut valoir 0 alors qu'un prix plancher existe : aucun repli appliqué ici.
        BigDecimal salePriceFloor,
        BigDecimal comSalePriceFloor,
        StatutArticle statut,
        String titreFoncier,
        // StkItemCategory (pas StkKind, voir nature*).
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        // StkItemType.
        UUID typeId,
        String typeCode,
        String typeLibelle,
        // Un sous-article porte sa propre StkHierarchyId : les listes de tranche mélangent les deux.
        boolean sousArticle,
        UUID lotParentId,
        // StkItem.ComFolderId ; dossierStatut = StrStatus.Code brut, comme DossierListItemDto.statut.
        UUID dossierId,
        String dossierCode,
        String dossierStatut,
        UUID clientId,
        String clientNom,
        // StkItem.Vat, ratio brut (0.20 = 20 %).
        BigDecimal tauxTVA,
        // StkPresentationType, propre à chaque tranche.
        UUID presentationTypeId,
        String presentationTypeCode,
        String presentationTypeLibelle,
        BigDecimal superficieSousArticles,
        // StkKind : nature physique du bien, distincte de Catégorie/Type.
        UUID natureId,
        String natureCode,
        String natureLibelle,
        // StkFloor.
        UUID etageId,
        String etageCode,
        String etageLibelle,
        // StkBuilding.
        UUID immeubleId,
        String immeubleCode,
        String immeubleLibelle
) {
}
