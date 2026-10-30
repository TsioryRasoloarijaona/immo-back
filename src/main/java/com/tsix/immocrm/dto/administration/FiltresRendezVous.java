package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/** Filtres communs à la liste et à l'export des rendez-vous. */
public record FiltresRendezVous(
        @Parameter(description = "Cherché dans code, description, déclencheur et source ; espaces et casse ignorés.")
        String texte,
        @Parameter(description = "Personnel (CfgTier).") UUID gestionnaireId,
        @Parameter(description = "Premier jour inclus de la date prévisionnelle, ISO.", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @Parameter(description = "Dernier jour inclus de la date prévisionnelle, ISO.", example = "2026-10-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
}
