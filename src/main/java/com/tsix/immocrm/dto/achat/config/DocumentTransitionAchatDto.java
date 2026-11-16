package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Document à joindre d'une transition d'achat (BuyDocumentWorkFlowDocumentToAttach), toujours "
        + "obligatoire, en lecture comme en écriture.")
public record DocumentTransitionAchatDto(
        @Schema(description = "Type de document (AdmAttachedFileType.PKey) ; obligatoire en écriture.") UUID typeDocumentId,
        @Schema(description = "Libellé du type de document ; ignoré en écriture.", accessMode = Schema.AccessMode.READ_ONLY)
        String typeDocumentLibelle,
        @Schema(description = "Type de marché (PrjMarketType.PKey) ; obligatoire en écriture.") UUID typeMarcheId,
        @Schema(description = "Libellé du type de marché ; ignoré en écriture.", accessMode = Schema.AccessMode.READ_ONLY)
        String typeMarcheLibelle,
        @Schema(description = "Décompte visé, pour la seule gestion PrjInvoiceDebitWorkingOut (obligatoire) ; null ailleurs.",
                allowableValues = {"PREMIER", "INTERMEDIAIRE", "FINAL"}) String decompte
) {
}
