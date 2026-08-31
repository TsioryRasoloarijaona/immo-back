package com.tsix.immocrm.dto.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/articles/{id}}, including sub-lots (via {@code lotParentId}
 * / {@code StkItem.parent}) — e.g. a cave/parking annexed to a main lot. Works identically for a
 * sous-article id (recherche inverse): {@code trancheId}/{@code lotParentId} (+ their labels) are
 * always populated when applicable, without a further call.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code StkItem}. Adjust once a spec is available.
 */
public record ArticleDetailDto(
        UUID id,
        String code,
        String description,
        String adresse,
        BigDecimal area,
        BigDecimal salePrice,
        BigDecimal comSalePrice,
        // -- Prix plancher (StkItem.SalePriceFloor/ComSalePriceFloor) — jusqu'ici jamais exposés
        // ni lus nulle part dans l'app malgré leur mapping JPA préexistant. Ajoutés suite à un bug
        // signalé sur ARS0804 : salePrice/comSalePrice y valent 0 (le prix commercial n'a jamais
        // été saisi pour ce lot) alors qu'un prix plancher réel existe (743 400.00/619 500.00 HT) —
        // confirmé sur données réelles que ce n'est pas un cas isolé (99 autres articles
        // "disponibles" dans le même cas). Volontairement des champs bruts additionnels plutôt
        // qu'un fallback silencieux dans salePrice/comSalePrice : ces deux derniers restent
        // strictement `StkItem.SalePrice/ComSalePrice` partout ailleurs dans l'app (Devis,
        // création de Dossier) — au front de décider d'afficher le plancher quand le prix
        // commercial est à 0.
        BigDecimal salePriceFloor,
        BigDecimal comSalePriceFloor,
        StatutArticle statut,
        UUID trancheId,
        // -- Libellé de la tranche (CfgTranche.Code/Description), pour affichage/dropdown sans
        // second appel à GET /api/tranches/{trancheId}.
        String trancheCode,
        String trancheDescription,
        UUID lotParentId,
        // -- Libellé de l'article parent, même logique que trancheCode/trancheDescription —
        // null si cet article n'est pas un sous-article (lotParentId == null).
        String lotParentCode,
        String lotParentDescription,
        // -- Sous-articles rattachés (StkItem.ParentId), quel que soit leur statut — chaque
        // entrée porte déjà son `description` (le "nom" du sous-article, pas que son `code`) via
        // ArticleListItemDto.
        List<ArticleListItemDto> sousLots,
        String titreFoncier,
        // -- Nature du bien (StkItemCategory) — même paire id/libellé que dans ArticleListItemDto.
        UUID categorieId,
        String categorieCode,
        String categorieLibelle,
        // -- Typologie (StkItemType, ex. "1 CHAMBRES + SALON") — même paire id/libellé.
        UUID typeId,
        String typeCode,
        String typeLibelle,
        // -- Lien inverse Article → Dossier (StkItem.ComFolderId), demande #5 de
        // instructions/from_front.md : null tant que l'article n'est pas réservé/vendu. Même
        // sémantique que sur ArticleListItemDto — dossierStatut reprend la valeur brute de
        // DossierDetailDto.statut (StrStatus.Code), permet à la fiche article d'afficher une
        // carte "Dossier de vente" (le front appelle GET /api/dossiers/{dossierId} avec cet id).
        UUID dossierId,
        String dossierCode,
        String dossierStatut,
        UUID clientId,
        String clientNom,
        // -- Immeuble (StkBuilding, StkItem.StkBuildingId) — null si l'article n'est pas rattaché
        // à un immeuble (ex. villa, terrain).
        UUID immeubleId,
        String immeubleCode,
        String immeubleLibelle,
        // -- Étage (StkFloor, StkItem.StkFloorId) — PAS une colonne texte/numérique libre malgré
        // ce qu'on pouvait croire (voir instructions/from_front.md #4) : un vrai référentiel,
        // même paire code/libellé que Catégorie/Type (ex. code "RDC", libellé "Rez de chaussée").
        UUID etageId,
        String etageCode,
        String etageLibelle,
        // -- Nature du bien (StkKind, StkItem.StkKindId) — répond à instructions/from_front.md #4
        // ("Nature — aucun référentiel nature trouvé... à confirmer") : c'est bien un référentiel
        // distinct de Catégorie/Type, la nature physique du bien (ex. "TER" pour terrain vs un lot
        // bâti), pas une valeur marketing.
        UUID natureId,
        String natureCode,
        String natureLibelle,
        // -- Taux de TVA (StkItem.Vat, ex. 0.20 = 20%) — instructions/from_front.md #4, colonne
        // "TVA (%)" absente jusqu'ici. Ratio brut, non multiplié par 100.
        BigDecimal tauxTVA,
        // -- "Type Présentation" (StkPresentationType) — voir ArticleListItemDto.presentationType*
        // pour le contexte complet (résout la question ouverte de la demande #4).
        UUID presentationTypeId,
        String presentationTypeCode,
        String presentationTypeLibelle,
        // -- Superficie totale des sous-articles (voir ArticleListItemDto.superficieSousArticles) —
        // 0 si aucun sous-article.
        BigDecimal superficieSousArticles
) {
}
