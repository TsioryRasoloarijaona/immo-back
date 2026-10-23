package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche d'une prospection terrain : les champs de la liste, plus les ids saisissables et les zones.")
public record ProspectionTerrainDetailDto(
        @JsonUnwrapped ProspectionTerrainListeDto ligne,
        UUID categorieId,
        UUID typeId,
        UUID niveauId,
        UUID vocationId,
        UUID paysId,
        UUID villeId,
        UUID courtierId,
        UUID notaireId,
        @Schema(description = "Notaire (CfgLawyerId).") ReferentielItemDto notaire,
        UUID orientationId,
        @Schema(description = "Orientation (StkOrientation).") ReferentielItemDto orientation,
        @Schema(description = "GmapLink ; commence par https://www.google s'il est saisi.") String lienGoogle,
        @Schema(description = "ZoneIdentification (terrain brut).") String identificationZone,
        @Schema(description = "DealerConnectionConditions (terrain brut).") String conditionsRaccordement,
        @Schema(description = "LandBase (terrain brut).") String assietteFonciere,
        @Schema(description = "IsTopographicSurvey (terrain brut).") Boolean leveeTopographique,
        @Schema(description = "Zones du terrain brut (LndLandZone).") List<ZoneTerrainDto> zones,
        @Schema(description = "Terrain (StkItem) issu de cette prospection, sinon null.") UUID terrainId,
        @Schema(description = "Code de ce terrain, sinon null.") String terrainCode,
        @Schema(description = "Vrai si « Transformer en terrain » est possible pour l'appelant.") boolean peutTransformer,
        @Schema(description = "Motif du refus quand peutTransformer est faux, sinon null.") String motifNonTransformable
) {
}
