package com.tsix.immocrm.dto.projet.travaux;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de la liste des jours travaillés ; absent = pas de filtre (rien coché = tout). */
public record FiltresTravaux(
        @Parameter(description = "Société (CfgCompany.PKey).") UUID societeId,
        @Parameter(description = "Projets (PrjProject.PKey) ; un projet non habilité est ignoré, les jours sans projet exclus.")
        List<UUID> projetIds,
        @Parameter(description = "Date minimale (incluse).") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date maximale (incluse, toute la journée).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Statuts (StrStatus.PKey) cochés ; vide = tous les statuts visibles.") List<UUID> statutIds,
        @Parameter(description = "Type de travaux (GrhWorkedDayType.PKey).") UUID typeId,
        @Parameter(description = "Personnel (CfgTier.PKey).") UUID employeId,
        @Parameter(description = "Code, description, notes, nom du personnel ou du client (contient).") String recherche) {

    public FiltresTravaux {
        projetIds = projetIds == null ? List.of() : projetIds;
        statutIds = statutIds == null ? List.of() : statutIds;
    }

    /** Aucun filtre : sert à relire un seul jour avec la même règle de visibilité que la liste. */
    public static FiltresTravaux aucun() {
        return new FiltresTravaux(null, null, null, null, null, null, null, null);
    }
}
