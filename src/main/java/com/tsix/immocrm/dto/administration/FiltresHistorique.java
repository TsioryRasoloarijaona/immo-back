package com.tsix.immocrm.dto.administration;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** Filtres communs à la liste et à l'export de l'historique. */
public record FiltresHistorique(
        @Parameter(description = "Premier jour inclus, ISO ; absent = dateFin moins 30 jours.", example = "2026-10-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @Parameter(description = "Dernier jour inclus, ISO ; absent = aujourd'hui.", example = "2026-10-30")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @Parameter(description = "Sous-chaîne littérale cherchée dans l'entité ou son libellé, sans tenir compte de la casse.")
        String texte,
        @Parameter(description = "Utilisateur exact, valeur de GET /api/administration/historique/utilisateurs.")
        String utilisateur,
        @Parameter(description = "Action exacte ; vide = toutes.",
                schema = @Schema(allowableValues = {"Added", "Modified", "Deleted"}))
        String action) {

    private static final int JOURS_PAR_DEFAUT = 30;
    private static final Set<String> ACTIONS = Set.of("Added", "Modified", "Deleted");

    /** Période toujours bornée : la table n'a aucun index sur la date. */
    public Periode periode(Clock clock) {
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now(clock);
        LocalDate debut = dateDebut != null ? dateDebut : fin.minusDays(JOURS_PAR_DEFAUT);
        if (debut.isAfter(fin)) {
            throw new InvalidRequestException("dateDebut doit précéder ou égaler dateFin.");
        }
        return new Periode(debut.atStartOfDay(), fin.plusDays(1).atStartOfDay());
    }

    /** {@code null} pour une action vide. */
    public String actionValidee() {
        if (action == null || action.isEmpty()) {
            return null;
        }
        if (!ACTIONS.contains(action)) {
            throw new InvalidRequestException("action doit valoir Added, Modified ou Deleted.");
        }
        return action;
    }

    public record Periode(LocalDateTime debut, LocalDateTime finExclue) {
    }
}
