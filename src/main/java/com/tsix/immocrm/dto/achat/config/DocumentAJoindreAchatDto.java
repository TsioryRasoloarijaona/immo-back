package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Document à joindre d'un statut d'achat (BuyDocumentStatusDocumentToAttach), en lecture comme en écriture.")
public record DocumentAJoindreAchatDto(
        @Schema(description = "Type de document (AdmAttachedFileType.PKey) ; obligatoire en écriture.") UUID typeDocumentId,
        @Schema(description = "Libellé du type de document ; ignoré en écriture.", accessMode = Schema.AccessMode.READ_ONLY)
        String typeDocumentLibelle,
        @Schema(description = "Document obligatoire (IsMandatory) ; absent = false.") Boolean obligatoire,
        @Schema(description = "Titre à saisir (WithTitle) ; absent = false.") Boolean avecTitre
) {
    public DocumentAJoindreAchatDto {
        obligatoire = Boolean.TRUE.equals(obligatoire);
        avecTitre = Boolean.TRUE.equals(avecTitre);
    }
}
