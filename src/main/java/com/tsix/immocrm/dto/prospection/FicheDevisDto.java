package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Fiche d'une offre : l'offre, ses sous-articles et le dossier qu'elle a donné.")
public record FicheDevisDto(

        @Schema(description = "L'offre, mêmes champs qu'une ligne de GET /api/devis.")
        DevisDto offre,

        @Schema(description = "Sous-articles de l'offre (StlQuoteItem), par code croissant. Vide s'il n'y en a pas.")
        List<SousArticleOffreDto> sousArticles,

        DossierLieDto dossierLie,

        boolean schemaFinancementSaisi
) {
}
