package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** À ne pas confondre avec {@link RelanceDto} (dossiers, colonnes Revival mortes) — voir FEATURE-dashboard-relances.md §7. */
public record RelanceProspectionDto(

        @Schema(example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
        UUID interactionId,

        @Schema(description = "Prospection ou pré-prospection concernée.")
        UUID prospectionId,

        boolean preProspection,

        String prospectNom,

        String telephone,

        @Schema(example = "2026-09-15")
        LocalDate dateRelance,

        @Schema(description = "Jours écoulés depuis l'échéance : `0` pour `echeance=AUJOURDHUI`, "
                + "de 1 à 30 pour `echeance=RETARD`.", example = "3")
        long joursDeRetard,

        @Schema(description = "Motif saisi par le commercial (`AlertMessage`). Fréquemment `null` "
                + "— une relance sans motif reste une relance à passer.")
        String motif,

        UUID commercialId,

        @Schema(description = "Nom du commercial résolu. `null` en même temps que `commercialId`.")
        String commercialNom,

        boolean nonAffectee,

        LocalDateTime derniereInteractionDate
) {
}
