package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.planification.TypeDependance;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Prédécesseur d'une tâche ; stocké en « <rang>|<type> », réécrit par le serveur quand les rangs changent.")
public record PredecesseurDto(
        @Schema(description = "Tâche prédécesseur, de la même planification.") UUID tacheId,
        @Schema(description = "Type de dépendance ; absent = FIN_DEBUT.") TypeDependance type) {
}
