package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

/** Un statut atteignable par l'appelant, et la colonne jalon où {@code PATCH .../statut} écrira sa {@code date}. */
@Schema(description = "Transition que l'utilisateur connecté peut exécuter depuis le statut courant du dossier.")
public record EtapeSuivanteDto(
        @Schema(description = "Code du statut cible (StrStatus.Code).", example = "CONCRETISATION")
        String statut,
        @Schema(description = "Champ du dossier (camelCase) qui reçoit la date du changement, null si aucun.",
                example = "advanceDate")
        String champDate,
        @Schema(description = "Libellé de ce champ, ou explication s'il n'y en a pas.", example = "Date concrétisation")
        String libelleChampDate,
        @Schema(description = "CONFIRME (date associée paramétrée ou confirmée), PLAUSIBLE ou AUCUN.",
                example = "CONFIRME")
        String confiance,
        @Schema(description = "Libellé de la transition (StrWorkFlow.Description), le code cible en repli.",
                example = "CONCRETISATION")
        String transition,
        @Schema(description = "Transition « avec proposition du passage automatique » (StrWorkFlow.WithAutomaticTransition).")
        boolean passageAutomatique
) {
}
