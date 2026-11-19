package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Filtres de la liste des demandes internes d'achat, partagés par la liste et l'export ; absent = pas de filtre. */
public record FiltresDemandesAchat(
        @Parameter(description = "Sociétés (CfgCompany.PKey), paramètre répété ; une société non habilitée est ignorée.")
        List<UUID> societeIds,
        @Parameter(description = "Projets (PrjProject.PKey), paramètre répété ; un projet non habilité est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "true : demandes sans projet seulement.")
        Boolean sansProjet,
        @Parameter(description = "Procédure d'achat (BuySelectionKind.PKey).")
        UUID procedureId,
        @Parameter(description = "Statuts (StrStatus.PKey), paramètre répété.")
        List<UUID> statutIds,
        @Parameter(description = "Module (AdmModule.PKey).")
        UUID moduleId,
        @Parameter(description = "Date portée par du / au : une des 14 clés OMRAN (défaut DocDate) ; clé inconnue = 400.",
                example = "DocDate")
        String champDate,
        @Parameter(description = "Date (champDate) à partir de ce jour inclus.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Date (champDate) jusqu'à ce jour inclus.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Raison sociale du fournisseur retenu, contient (insensible à la casse).")
        String fournisseur,
        @Parameter(description = "Code de la demande, contient.")
        String code,
        @Parameter(description = "Prix estimé TTC supérieur ou égal.")
        BigDecimal montantMin,
        @Parameter(description = "Prix estimé TTC inférieur ou égal.")
        BigDecimal montantMax,
        @Parameter(description = "Description ou notes, contient.")
        String recherche) {

    public FiltresDemandesAchat {
        societeIds = Objects.requireNonNullElse(societeIds, List.of());
        projetIds = Objects.requireNonNullElse(projetIds, List.of());
        sansProjet = Boolean.TRUE.equals(sansProjet);
        statutIds = Objects.requireNonNullElse(statutIds, List.of());
        champDate = Objects.requireNonNullElse(champDate, "DocDate");
    }
}
