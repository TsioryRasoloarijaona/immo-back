package com.tsix.immocrm.dto.desistement;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/** Filtres de {@code GET /api/desistements} ; bornes incluses, absentes = sans borne. */
public record FiltresDesistements(
        @Parameter(description = "Code du désistement, code du dossier ou nom du client, insensible à la casse.")
        String q,
        @Parameter(description = "Tranche de l'article (CfgTranche).")
        UUID trancheId,
        @Parameter(description = "Date de demande, borne inférieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate demandeDu,
        @Parameter(description = "Date de demande, borne supérieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate demandeAu,
        @Parameter(description = "Date de validation, borne inférieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validationDu,
        @Parameter(description = "Date de validation, borne supérieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validationAu,
        @Parameter(description = "Date de légalisation, borne inférieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate legalisationDu,
        @Parameter(description = "Date de légalisation, borne supérieure (aaaa-mm-jj).")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate legalisationAu
) {
}
