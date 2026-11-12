package com.tsix.immocrm.services.projet;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

/** Filtres de {@code GET /api/projet/lots}, liés une fois par {@code @ParameterObject @ModelAttribute}. */
public record FiltresLots(
        @Parameter(description = "Projet(s) (PrjProject.PKey), répétable ; restreint au périmètre, absent = tous les projets visibles.")
        List<UUID> projetIds,
        @Parameter(description = "Statut(s) du lot (StrStatus.PKey), répétable, tous genres ; absent = tous.") List<UUID> statutIds,
        @Parameter(description = "Entité juridique du projet du lot (CfgCompany.PKey) ; absente = toutes.") UUID entiteId,
        @Parameter(description = "Type de lot (PrjMarketType.PKey).") UUID typeId,
        @Parameter(description = "Fournisseur du lot (CfgTier.PKey).") UUID fournisseurId,
        @Parameter(description = "Sous-chaîne sur le code, la désignation ou le nom du fournisseur.") String search
) {
}
