package com.tsix.immocrm.dto.projet.reunions;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Onglet Avancements : lignes = critères d'avancement du projet, colonnes = niveaux de nivellement.")
public record AvancementsDto(
        @Schema(description = "Critères du projet (PrjProjectProgressCriteriaLine), par O.D.T.")
        List<ReferentielItemDto> criteres,
        @Schema(description = "Niveaux de nivellement du projet (CfgTrancheLevelingDefinition), par O.D.T.")
        List<ReferentielItemDto> niveaux,
        @Schema(description = "Cellules renseignées ; une cellule absente vaut 0.") List<Valeur> valeurs) {

    @Schema(name = "ValeurAvancementReunion", description = "Cellule de l'onglet Avancements.")
    public record Valeur(
            @Schema(description = "PrjProjectProgressCriteriaLine.PKey.") UUID critereId,
            @Schema(description = "CfgTrancheLevelingDefinition.PKey.") UUID niveauId,
            @Schema(description = "Avancement en pourcentage (0 à 100).", example = "40") BigDecimal pourcentage) {
    }
}
