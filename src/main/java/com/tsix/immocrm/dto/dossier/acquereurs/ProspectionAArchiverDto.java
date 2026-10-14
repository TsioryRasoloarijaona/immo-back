package com.tsix.immocrm.dto.dossier.acquereurs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Prospection ouverte d'un acquéreur, proposée à l'archivage comme OMRAN (« Prospection à archiver »).")
public record ProspectionAArchiverDto(
        UUID id,
        String code,
        @Schema(description = "« Tiers => Tranche => Article », libellé d'OMRAN (« Non affecté » sans tranche).")
        String libelle
) {
}
