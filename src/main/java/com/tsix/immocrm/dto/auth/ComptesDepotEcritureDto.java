package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record ComptesDepotEcritureDto(
        @Schema(description = "État final des comptes de dépôt. Absent ou null = 400 ; [] retire tout.")
        List<UUID> compteIds
) {
}
