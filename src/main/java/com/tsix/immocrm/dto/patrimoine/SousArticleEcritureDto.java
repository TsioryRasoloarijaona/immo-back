package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Tranche, vocation et nature sont celles de l'article parent : absentes du corps. */
@Schema(description = "Création d'un sous-article (lot annexe : parking, jardin, terrasse) rattaché à un article.")
public record SousArticleEcritureDto(
        @Schema(description = "Code saisi, unique dans la tranche du parent. Obligatoire.")
        String code,
        @Schema(description = "Libellé. Obligatoire.")
        String description,
        @Schema(description = "Catégorie (StkItemCategory.id), nature Bâtiment et « Pour les sous articles ». Obligatoire.")
        UUID categorieId,
        @Schema(description = "Type (StkItemType.id), même filtre. Obligatoire.")
        UUID typeId,
        @Schema(description = "Immeuble (StkBuilding.id) ; défaut = immeuble du parent.")
        UUID immeubleId,
        @Schema(description = "Étage (StkFloor.id). Obligatoire.")
        UUID etageId,
        @Schema(description = "Superficie provisoire en m², > 0, défaut 1.")
        BigDecimal surfaceProvisoire,
        @Schema(description = "Superficie définitive en m², > 0, défaut = surfaceProvisoire.")
        BigDecimal surfaceDefinitive,
        @Schema(description = "Prix du m² provisoire HT, ≥ 0.")
        BigDecimal prixM2ProvisoireHT,
        @Schema(description = "Prix du m² définitif HT, ≥ 0.")
        BigDecimal prixM2DefinitifHT,
        @Schema(description = "TVA en fraction ∈ [0 ; 1], défaut 0.")
        BigDecimal tauxTva,
        @Schema(description = "Numéro du titre foncier.")
        String titreFoncier,
        @Schema(description = "Titré ; défaut false.")
        Boolean estTitre,
        @Schema(description = "Niveau de remise 1, fraction ∈ [0 ; 1].")
        BigDecimal remiseMax1,
        @Schema(description = "Niveau de remise 2, fraction ∈ [0 ; 1].")
        BigDecimal remiseMax2,
        @Schema(description = "Adresse.")
        String adresse,
        @Schema(description = "Notes.")
        String notes,
        @Schema(description = "Superficie intérieure en m², ≥ 0.")
        BigDecimal surfaceInterieure,
        @Schema(description = "Superficie des murs en m², ≥ 0.")
        BigDecimal surfaceMurs,
        @Schema(description = "Superficie des parties communes en m², ≥ 0.")
        BigDecimal surfacePartiesCommunes,
        @Schema(description = "Toujours affecté (AlwaysAffected) : suit l'article dans la vente. Défaut false.")
        Boolean toujoursAffecte
) {
}
