package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Statut de tâche (StrStatus) et ses transitions sortantes (StrWorkFlow).")
public record StatutTacheDto(
        @Schema(description = "PKey du statut.") UUID id,
        @Schema(description = "Code du statut.", example = "Non commencée") String code,
        @Schema(description = "Ordre d'affichage (Sorting).") Integer ordre,
        @Schema(description = "Transitions permises depuis ce statut, triées.") List<TransitionTacheDto> transitions) {

    @Schema(description = "Transition permise vers un statut.")
    public record TransitionTacheDto(
            @Schema(description = "Statut d'arrivée, à envoyer dans POST …/statut.") UUID statutId,
            @Schema(description = "Code du statut d'arrivée.", example = "En cours de réalisation") String code,
            @Schema(description = "Vrai si la transition exige un commentaire (WithComment).") boolean commentaireRequis) {
    }
}
