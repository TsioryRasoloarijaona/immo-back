package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Nombre d'actions du dossier pour un type (« La liste visualise le nombre d'actions groupées par type »).")
public record CompteurTypeActionDto(
        @Schema(description = "Type d'action (ComActionMessageType.PKey), null pour les actions sans type.")
        UUID typeId,
        @Schema(description = "Libellé du type, « Sans type » si absent.")
        String type,
        @Schema(description = "Nombre d'actions de ce type sur le dossier.")
        long nombre
) {
}
