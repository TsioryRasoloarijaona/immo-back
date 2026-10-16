package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Leads dont on veut l'état d'activité.")
public record LeadsActiviteRequest(

        @Schema(description = "Ids de pré-prospections et/ou de prospections (ComProspection.PKey), de 1 à 200.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty @Size(max = 200)
        List<@NotNull UUID> ids
) {
}
