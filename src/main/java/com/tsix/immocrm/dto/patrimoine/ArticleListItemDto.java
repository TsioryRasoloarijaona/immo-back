package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/articles}, {@code GET /api/tranches/{id}/articles},
 * {@code GET /api/articles/{id}/sous-articles} and {@code ArticleDetailDto.sousLots}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code StkItem}. Adjust once a spec is available.
 */
public record ArticleListItemDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        // -- Prix plancher (StkItem.SalePriceFloor/ComSalePriceFloor) — repli suggéré pour la
        // colonne "Prix de vente" de la grille legacy quand comSalePrice vaut 0 (prix commercial
        // jamais saisi pour ce lot, confirmé sur données réelles — voir instructions/from_front.md
        // #4). salePrice/comSalePrice restent strictement bruts, aucun repli n'est appliqué ici :
        // au front de décider s'il affiche le plancher.
        BigDecimal salePriceFloor,
        BigDecimal comSalePriceFloor,
        StatutArticle statut,
        String titreFoncier,
        // -- Nature du bien (StkItemCategory) — ex. "APPARTEMENT", "VILLA", "PARKING". `id` sert
        // de valeur pour le filtre `categorieId` des endpoints de listing ; `libelle` est le texte
        // à afficher (ex. dans les cases à cocher "Catégorie" du formulaire de qualification).
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        // -- Typologie (StkItemType) — ex. "1 CHAMBRES + SALON" (T2), "STUDIO". Même logique que
        // categorieId/categorieLibelle, pour le filtre `typeId`.
        UUID typeId,
        String typeCode,
        String typeLibelle,
        // -- Distingue un article "racine" d'un sous-article (StkItem.ParentId), et permet de
        // remonter à son parent sans un second appel — utile quand /api/tranches/{id}/articles
        // renvoie les deux mélangés (un sous-article porte sa propre StkHierarchyId).
        boolean sousArticle,
        UUID lotParentId,
        // -- Lien inverse Article → Dossier (StkItem.ComFolderId), demande #5 de
        // instructions/from_front.md : null tant que l'article n'est pas réservé/vendu (statut
        // DISPONIBLE). dossierStatut reprend exactement la même valeur brute que
        // DossierListItemDto.statut/DossierDetailDto.statut (StrStatus.Code) — le front réutilise
        // canonicalStatusLabel/toneForStatus sans nouvelle logique de mapping.
        UUID dossierId,
        String dossierCode,
        String dossierStatut,
        // -- Client rattaché au dossier ci-dessus (ComFolder.CfgCustomerId), même paire id/nom
        // que DossierListItemDto.clientId/clientNom.
        UUID clientId,
        String clientNom,
        // -- Taux de TVA (StkItem.Vat, ex. 0.20 = 20%) — demande #4, colonne "TVA (%)" absente
        // jusqu'ici. Ratio brut (à multiplier par 100 pour l'affichage), même convention que
        // CfgFolder.VATRatio/PartPercent ailleurs dans l'app.
        BigDecimal tauxTVA,
        // -- "Type Présentation" (StkPresentationType) — colonne à code court (F/C/D/H...) de la
        // grille legacy, confirmée distincte de Catégorie/Type (instructions/from_front.md #4) :
        // vraie table dédiée, scoped par tranche (StkPresentationType.StkHierarchyId).
        UUID presentationTypeId,
        String presentationTypeCode,
        String presentationTypeLibelle,
        // -- Superficie totale des sous-articles rattachés (somme StkItem.Area des lignes dont
        // ParentId = cet article) — demande #4, distincte de `area` (superficie de l'article
        // lui-même). 0 si aucun sous-article.
        BigDecimal superficieSousArticles,
        // -- Nature du bien (StkKind) — référentiel distinct de Catégorie/Type (voir
        // ArticleDetailDto), désormais aussi sur la liste (demande #4 : "reste ouvert pour la
        // partie filtre/colonne liste").
        UUID natureId,
        String natureCode,
        String natureLibelle,
        // -- Étage (StkFloor), même situation que Nature ci-dessus.
        UUID etageId,
        String etageCode,
        String etageLibelle,
        // -- Immeuble (StkBuilding) — bonus non demandé explicitement mais cohérent avec Nature/
        // Étage ci-dessus (déjà sur ArticleDetailDto).
        UUID immeubleId,
        String immeubleCode,
        String immeubleLibelle
) {
}
