package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.domain.EtatFicheVerrouillee;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Filtres de la liste des fiches verrouillées ; tous facultatifs et cumulés. */
public record FiltresFichesVerrouillees(
        @Parameter(description = "VERROUILLEE = State 1, NON_VERROUILLEE = tout le reste ; absent = toutes.")
        EtatFicheVerrouillee etat,
        @Parameter(description = "Premier jour de verrouillage inclus, ISO ; absent = pas de borne basse.", example = "2026-03-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @Parameter(description = "Dernier jour de verrouillage inclus, ISO ; absent = pas de borne haute.", example = "2026-03-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @Parameter(description = "Sous-chaîne de l'entité, du libellé ou de l'utilisateur ; vide ou blanc = pas de filtre.")
        String recherche) {

    void verifierPeriode() {
        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            throw new InvalidRequestException("dateDebut doit précéder ou égaler dateFin.");
        }
    }

    Integer stateEgal() {
        return etat == EtatFicheVerrouillee.VERROUILLEE ? EtatFicheVerrouillee.VERROUILLEE.state() : null;
    }

    Integer stateExclu() {
        return etat == EtatFicheVerrouillee.NON_VERROUILLEE ? EtatFicheVerrouillee.VERROUILLEE.state() : null;
    }

    LocalDateTime debut() {
        return dateDebut == null ? null : dateDebut.atStartOfDay();
    }

    LocalDateTime finExclue() {
        return dateFin == null ? null : dateFin.plusDays(1).atStartOfDay();
    }

    String motif() {
        return StringUtils.hasText(recherche) ? HistoriqueService.motif(recherche) : null;
    }
}
