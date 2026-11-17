package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Proposition de points « Prévisions » à recopier dans la grille ; rien n'est enregistré.")
public record PropositionPointsDto(
        @Schema(description = "Nouvelle date fin ; null si la date fin ne change pas.") LocalDate fin,
        @Schema(description = "Nouvelle durée en jours (fin − début) ; null si la date fin ne change pas.") Integer duree,
        @Schema(description = "Points proposés, triés par date ; remplacent toute la grille « Prévisions ».")
        List<PointSaisieDto> points
) {
}
