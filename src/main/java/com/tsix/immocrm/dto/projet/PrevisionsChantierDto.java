package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Prévisions chantier d'un projet : grille de main-d'œuvre par type de travaux (PrjProjectBuildingPrevision).")
public record PrevisionsChantierDto(
        @Schema(description = "PrjProject.PKey.") UUID projetId,
        @Schema(description = "Code du projet.") String code,
        @Schema(description = "Description du projet.") String description,
        @Schema(description = "Vrai si le projet a un nivellement (StkLevelingId) ; faux = bouton grisé, PUT refusé en 409.")
        boolean actif,
        @Schema(description = "Grille modifiable : projet actif et droit BuildingPrevision (exigé pour lire).") boolean modifiable,
        @Schema(description = "Lignes de la grille, par code du type de travaux.") List<LignePrevisionChantierDto> lignes,
        @Schema(description = "Tous les types de travaux (GrhWorkedDayType), par code : « Copier » et ajout de ligne.")
        List<TypeTravauxDto> typesTravaux
) {

    @Schema(description = "Ligne de prévision chantier ; totaux calculés, jamais stockés.")
    public record LignePrevisionChantierDto(
            @Schema(description = "PrjProjectBuildingPrevision.PKey.") UUID id,
            @Schema(description = "Type de travaux (GrhWorkedDayType.PKey).") UUID typeTravauxId,
            @Schema(description = "Code du type de travaux.") String code,
            @Schema(description = "Description du type de travaux.") String description,
            @Schema(description = "Unité du type de travaux (Unity).") String unite,
            @Schema(description = "Quantité.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT de la ligne.") BigDecimal prixUnitaireHT,
            @Schema(description = "PU HT × quantité (null compté 0).") BigDecimal totalHT,
            @Schema(description = "TVA en pourcentage 0–100 (ratio en base).", example = "20") BigDecimal tva,
            @Schema(description = "PU HT × quantité × (1 + TVA).") BigDecimal totalTTC,
            @Schema(description = "Notes (≤ 1000 caractères).") String notes
    ) {
    }

    @Schema(description = "Type de travaux (GrhWorkedDayType) avec ses valeurs par défaut.")
    public record TypeTravauxDto(
            @Schema(description = "GrhWorkedDayType.PKey.") UUID id,
            @Schema(description = "Code.") String code,
            @Schema(description = "Description.") String description,
            @Schema(description = "Unité (Unity).") String unite,
            @Schema(description = "Prix unitaire HT par défaut.") BigDecimal prixUnitaireHT,
            @Schema(description = "TVA par défaut en pourcentage 0–100.", example = "20") BigDecimal tva
    ) {
    }
}
