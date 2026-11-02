package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Filtres du journal des actions ; tous facultatifs et cumulés. */
public record FiltresActions(
        @Parameter(description = "Contient sur code, action, description, créateur, modificateur ; espaces ignorés.")
        String recherche,
        @Parameter(description = "Premier jour de création inclus, ISO ; exclut les lignes sans date.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate creationDebut,
        @Parameter(description = "Dernier jour de création inclus, ISO ; exclut les lignes sans date.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate creationFin,
        @Parameter(description = "Premier jour de modification inclus, ISO ; exclut les lignes sans date.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modificationDebut,
        @Parameter(description = "Dernier jour de modification inclus, ISO ; exclut les lignes sans date.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modificationFin) {

    void verifierPeriodes() {
        verifierPeriode(creationDebut, creationFin, "creationDebut doit précéder ou égaler creationFin.");
        verifierPeriode(modificationDebut, modificationFin, "modificationDebut doit précéder ou égaler modificationFin.");
    }

    private static void verifierPeriode(LocalDate debut, LocalDate fin, String message) {
        if (debut != null && fin != null && debut.isAfter(fin)) {
            throw new InvalidRequestException(message);
        }
    }

    static LocalDateTime debut(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay();
    }

    static LocalDateTime finExclue(LocalDate jour) {
        return jour == null ? null : jour.plusDays(1).atStartOfDay();
    }

    String motif() {
        return StringUtils.hasText(recherche) ? HistoriqueService.motif(recherche.replace(" ", "")) : null;
    }
}
