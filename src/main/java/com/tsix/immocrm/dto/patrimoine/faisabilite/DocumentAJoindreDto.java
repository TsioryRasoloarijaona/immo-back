package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Document à joindre d'un statut ou d'une transition d'étude.")
public record DocumentAJoindreDto(
        @Schema(description = "Type de document (AdmAttachedFileType.PKey, liste /api/referentiels/types-piece-jointe), "
                + "obligatoire.") UUID typeDocumentId,
        @Schema(description = "Lecture : libellé du type de document.") String typeDocumentLibelle,
        @Schema(description = "IsMandatory, stocké sans effet : toute ligne d'une transition est exigée.") Boolean estObligatoire,
        @Schema(description = "WithTitle, stocké sans effet.") Boolean avecTitre) {
}
