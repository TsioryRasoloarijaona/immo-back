package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de la liste des réunions ; absent = pas de filtre, la liste reste limitée aux projets habilités. */
public record FiltresReunions(
        @Parameter(description = "Société (CfgCompany.PKey) du projet.") UUID societeId,
        @Parameter(description = "Projets (PrjProject.PKey) ; un projet non habilité est ignoré.") List<UUID> projetIds,
        @Parameter(description = "Date de réunion minimale (incluse).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date de réunion maximale (incluse).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Type de réunion (BulMeetingType.PKey).") UUID typeId,
        @Parameter(description = "Référence P.V., note ou surveillant ; espaces et casse ignorés.") String recherche) {

    public FiltresReunions {
        projetIds = projetIds == null ? List.of() : projetIds;
    }
}
