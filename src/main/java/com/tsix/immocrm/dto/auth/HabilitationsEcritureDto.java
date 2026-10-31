package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.domain.TypeHabilitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record HabilitationsEcritureDto(
        @Schema(description = "Nœuds cochés. Absent ou null = 400. Un nœud figé est ignoré, envoyé ou non.")
        List<Cible> noeuds
) {

    @Schema(name = "CibleHabilitationDto", description = "Nœud de l'arbre : le couple type et id doit exister.")
    public record Cible(TypeHabilitation type, UUID id) {
    }
}
