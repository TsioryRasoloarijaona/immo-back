package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

public record FiltresTaches(
        @Parameter(description = "Statut ; Non commencée inclut les tâches sans statut.") UUID statutId,
        @Parameter(description = "Type de tâche (AdmTaskType).") UUID typeId,
        @Parameter(description = "Priorité (AdmTaskPriority).") UUID prioriteId,
        @Parameter(description = "Tiers gestionnaire (CfgTier).") UUID gestionnaireId,
        @Parameter(description = "Texte cherché dans le code et la description (casse ignorée).") String texte) {
}
