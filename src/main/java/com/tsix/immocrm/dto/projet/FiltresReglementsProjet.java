package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/projet/depenses/reglements} ; liste vide ou absente = pas de filtre. */
public record FiltresReglementsProjet(
        @Parameter(description = "Projets (PrjProject.PKey), paramètre répété ; un projet hors périmètre est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "Lot de marché (PrjMarket.PKey) de la facture réglée.")
        UUID lotId,
        @Parameter(description = "Facture réglée (BuyDocument.PKey).")
        UUID factureId,
        @Parameter(description = "Date du règlement à partir de ce jour inclus.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date du règlement jusqu'à ce jour inclus.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "false : lignes des règlements annulés (CANCELED) exclues ; absent = true.")
        Boolean inclureAnnules) {

    public FiltresReglementsProjet {
        projetIds = projetIds == null ? List.of() : projetIds;
        inclureAnnules = !Boolean.FALSE.equals(inclureAnnules);
    }
}
