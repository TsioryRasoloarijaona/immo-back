package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadata for one pièce jointe of a Dossier ({@code AdmAttachedFile}, see
 * {@code AdmAttachedFileSummary} for the confirmed linkage). The binary content is fetched
 * separately via {@code GET /api/dossiers/{id}/pieces-jointes/{pieceId}/fichier} — never
 * inlined here.
 */
@Schema(description = "Métadonnées d'une pièce jointe rattachée à un dossier.")
public record PieceJointeDto(
        @Schema(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey).")
        UUID id,
        @Schema(description = "Légende/description libre de la pièce jointe (ex. \"CIN client\", \"Contrat signé\").")
        String description,
        @Schema(description = "Catégorie legacy brute de la pièce jointe (ex. \"Yes.Com.Sales.Sale\", "
                + "\"Dossier de vente N° ... du ...\"). Purement informatif, sans logique métier dessus côté API.")
        String category,
        @Schema(description = "Type de document (ex. \"PIECE D'IDENTITE\", \"CONTRAT\", \"FICHE PERMISSION "
                + "REMISE DES CLES\"...), référentiel AdmAttachedFileType. Null si non renseigné en base "
                + "(cas de certains dépôts anciens ou faits hors de cette API).")
        String type,
        @Schema(description = "Extension du fichier, avec le point (ex. \".pdf\").")
        String extension,
        @Schema(description = "Date/heure de dépôt du fichier. Peut être absente (null) sur d'anciennes "
                + "pièces jointes créées avant le suivi de cette colonne dans les données legacy.")
        LocalDateTime dateCreation,
        @Schema(description = "Identifiant/login de l'utilisateur ayant déposé le fichier, tel qu'enregistré "
                + "en base. Peut être null si non renseigné (legacy) ou pour un dépôt effectué via cette API.")
        String creePar
) {
}
