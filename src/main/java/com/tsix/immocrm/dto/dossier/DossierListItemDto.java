package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/dossiers}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComFolder}. Adjust once a spec is available.
 */
public record DossierListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        BigDecimal prix,
        BigDecimal totalAPayer,
        /**
         * Jalon "Concrétisation" ({@code ComFolder.advanceDate}) — exposé en plus de
         * {@code dateDocument} (qui sert de proxy pour "Date de réservation", voir
         * {@code ChampDateDossier}) pour le filtre par période, point 3 de
         * {@code instructions/filtres-avances.md}. Null tant que le dossier n'a pas atteint ce
         * statut.
         */
        LocalDateTime dateConcretisation,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        @Schema(description = "Code de la prospection à l'origine du dossier — pour le lien cliquable vers la prospection depuis le dossier (`GET /api/prospections/{prospectionId}`).")
        String prospectionCode
) {
}
