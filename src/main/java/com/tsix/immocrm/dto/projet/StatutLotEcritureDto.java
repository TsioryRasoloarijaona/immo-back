package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Statut de lot (POST et PUT) ; un PUT remplace la fiche entière, cases et champs obligatoires compris.")
public record StatutLotEcritureDto(
        @Schema(description = "Genre de lot (StrEntity.PKey, un des 5 genres) ; obligatoire, fixe après création.")
        UUID genreId,
        @Schema(description = "O.D.T (Sorting), obligatoire.", example = "3") Integer ordre,
        @Schema(description = "Code, obligatoire, unique dans le genre sans casse, ≤ 255.", example = "ADJUDICATION")
        String code,
        @Schema(description = "Libellé, obligatoire, ≤ 255.") String description,
        @Schema(description = "Type (StrStatusType.PKey, voir le catalogue), obligatoire.") UUID typeId,
        @Schema(description = "Durée de vie en jours, ≥ 0 ; 0 ou absent = infinie.", example = "0") Integer dureeVieJours,
        @Schema(description = "Nom de couleur .NET du catalogue (sans casse) ; vide ou absent = aucune.", example = "LimeGreen")
        String couleur,
        @Schema(description = "Image (CfgImage.PKey), absent = aucune.") UUID imageId,
        @Schema(description = "Notes, ≤ 1000.") String notes,
        @Schema(description = "Numéros des cases cochées, pris dans le catalogue du genre ; absent = aucune.") List<Integer> cases,
        @Schema(description = "Champs obligatoires (StrField.PKey du catalogue du genre) ; absent = aucun.")
        List<UUID> champsObligatoires
) {
    public StatutLotEcritureDto {
        dureeVieJours = Objects.requireNonNullElse(dureeVieJours, 0);
        cases = Objects.requireNonNullElse(cases, List.of());
        champsObligatoires = Objects.requireNonNullElse(champsObligatoires, List.of());
    }
}
