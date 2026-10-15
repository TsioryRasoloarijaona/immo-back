package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dossier créé depuis l'offre (ComFolder.StlQuoteId, ou RntFolder.StlQuoteId en location).")
public record DossierLieDto(
        @Schema(description = "Id du dossier de vente (ComFolder.id) ; null pour un dossier de location.")
        UUID id,
        @Schema(description = "Code du dossier.")
        String code,
        @Schema(description = "true si c'est un dossier de location (RntFolder), sans fiche dans Arpent.")
        boolean location
) {
}
