package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.domain.TypeHabilitation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Société > Projet > Tranche et Société > Projet travaux, sociétés inactives comprises.")
public record NoeudHabilitationDto(
        TypeHabilitation type,

        UUID id,

        String libelle,

        @Schema(description = "Une ligne AdmUserHabilitation du compte vise ce nœud.")
        boolean coche,

        @Schema(description = "Porté par une ligne à plusieurs cibles, gérée dans OMRAN : coché, non modifiable.")
        boolean figee,

        List<NoeudHabilitationDto> enfants
) {
}
