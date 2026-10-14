package com.tsix.immocrm.dto.dossier.acquereurs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Prospections cochées dans la proposition d'archivage.")
public record ArchiverProspectionsRequest(
        @Schema(description = "Prospections ouvertes des acquéreurs du dossier (`ComProspection.PKey`).")
        List<UUID> prospectionIds
) {
}
