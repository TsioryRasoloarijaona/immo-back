package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Rôle autorisé à emprunter une transition (BuyDocumentWorkFlowAdmRole → AdmRole).")
public record RoleTransitionDto(
        @Schema(description = "AdmRole.PKey.") UUID id,
        @Schema(description = "Code du rôle (AdmRole.Code).", example = "Comptabilité") String code
) {
}
