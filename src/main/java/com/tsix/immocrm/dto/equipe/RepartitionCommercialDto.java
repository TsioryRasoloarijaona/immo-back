package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Nombre de leads attribués à un commercial par la répartition automatique.")
public record RepartitionCommercialDto(
        @Schema(description = "Commercial (employé, CfgTier).")
        UUID commercialId,
        @Schema(description = "Nom du commercial.", example = "Faycal BENALI")
        String commercialNom,
        @Schema(description = "Leads attribués à ce commercial dans ce lot.", example = "4")
        int nombre
) {
}
