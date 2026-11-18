package com.tsix.immocrm.dto.projet.reunions;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix des écrans réunions, lisibles avec le droit de la liste.")
public record ChoixReunionsDto(
        @Schema(description = "Types de réunion (BulMeetingType), par O.D.T.") List<ReferentielItemDto> types,
        @Schema(description = "Statuts de réserve (BulObservationStatus), par O.D.T.") List<ObservationDto> statuts,
        @Schema(description = "Lots du projet demandé (PrjMarket) ; vide sans projet.") List<ReferentielItemDto> lots) {
}
