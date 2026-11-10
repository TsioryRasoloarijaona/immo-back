package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix des filtres Lots et Décomptes ; projets, catégories et fournisseurs limités au périmètre de l'appelant.")
public record ReferentielsProjetDto(
        @Schema(description = "Projets visibles (habilitations de l'appelant), tri par code.") List<ProjetFiltreDto> projets,
        @Schema(description = "Types de lot (PrjMarketType), tri par code.") List<ElementReferentielProjetDto> typesLot,
        @Schema(description = "Statuts de lot, par genre de lot puis ordre.") List<StatutLotProjetDto> statutsLot,
        @Schema(description = "Statuts de facture (BuyDocumentStatus), tri par ordre du circuit (sorting) puis code.") List<ElementReferentielProjetDto> statutsFacture,
        @Schema(description = "Catégories (StlCategory) portées par au moins un lot visible.") List<ElementReferentielProjetDto> categories,
        @Schema(description = "Fournisseurs (CfgTier) d'un lot ou d'un décompte visible, tri par nom ; description = nom affiché.") List<ElementReferentielProjetDto> fournisseurs
) {
}
