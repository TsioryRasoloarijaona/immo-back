package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RolesUtilisateurEcritureDto(
        @Schema(description = "État final des rôles. Absent ou null = 400 ; [] retire tout.")
        List<UUID> roleIds
) {
}
