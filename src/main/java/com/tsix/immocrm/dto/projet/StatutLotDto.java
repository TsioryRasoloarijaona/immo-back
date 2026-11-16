package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Statut de lot (StrStatus d'un des 5 genres de lot PrjMarket*), fiche complète de l'éditeur.")
public record StatutLotDto(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "Code du statut, unique dans le genre.", example = "ADJUDICATION") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre dans le workflow (Sorting), null si absent.") Integer ordre,
        @Schema(description = "Nom de couleur .NET (ColorName) tel quel, null ou vide si absent.", example = "LimeGreen")
        String couleur,
        @Schema(description = "Genre de lot (StrEntity.PKey du groupe).") UUID groupeId,
        @Schema(description = "Code du genre de lot.", example = "PrjMarket") String groupeCode,
        @Schema(description = "Libellé du genre de lot (StrEntity.Description).", example = "Lot Entrepreneurs") String genre,
        @Schema(description = "Catégorie du statut (StrStatusType.Description), null si absente.", example = "Nouveau")
        String categorie,
        @Schema(description = "Type du statut (StrStatusType.PKey), null si absent.") UUID typeId,
        @Schema(description = "Durée de vie en jours (Delay) ; 0 = infinie ; null si absente.", example = "0") Integer duree,
        @Schema(description = "Image du statut (CfgImage.PKey), null si aucune.") UUID imageId,
        @Schema(description = "Notes, null ou vide si aucune.") String notes,
        @Schema(description = "Numéros (1-20) des cases cochées (BoolField001…020), croissants.") List<Integer> cases,
        @Schema(description = "Champs obligatoires du statut (StrField.PKey de StrStatusMandatoryField).")
        List<UUID> champsObligatoiresIds
) {
}
