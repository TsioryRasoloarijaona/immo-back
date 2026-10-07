package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Propriété {@code prospectionExistante} du 409 de {@code instructions/FEATURE-doublons-prospect-projet.md} §3. */
// Texte laissé dans l'annotation : ce record n'est pas un schéma de components/ (pas de .md possible).
@Schema(description = "Prospection déjà ouverte pour ce prospect sur ce projet. Un prospect n'a "
        + "qu'une prospection ouverte par projet ; c'est sur elle qu'on ajoute des dossiers.")
public record ProspectionExistanteDto(
        @Schema(description = "Identifiant (ComProspection.PKey) — ouvrable par `GET /api/prospections/{id}`.")
        UUID id,
        @Schema(description = "Code de la prospection, p.ex. `26/05893`.", example = "26/05893")
        String code,
        @Schema(description = "Date du document (`DocDate`).")
        LocalDateTime dateDocument,
        @Schema(description = "Libellé de la nature/étape (ComProspectionKind), null si aucune.",
                example = "3 QUALIFIÉ")
        String etapeLibelle,
        @Schema(description = "Nom du commercial affecté, null si aucun.", example = "Othman DIOURI")
        String commercialNom,
        @Schema(description = "Code de la tranche portée par la prospection existante.", example = "APF")
        String trancheCode,
        @Schema(description = "Code du projet de cette tranche — le projet sur lequel la règle "
                + "s'applique.", example = "APF")
        String projetCode
) {
}
