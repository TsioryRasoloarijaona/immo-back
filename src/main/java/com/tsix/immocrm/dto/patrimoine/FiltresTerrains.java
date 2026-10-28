package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.UUID;

/** Filtres de {@code GET /api/terrains} et {@code GET /api/lotissements} ; absent = pas de filtre. */
public record FiltresTerrains(
        @Parameter(description = "Recherche sans casse ni espaces : code, description, titre foncier, adresse, notes…",
                example = "TER-01") String search,
        @Parameter(description = "Société (CfgCompanyId).") UUID entiteId,
        @Parameter(description = "Statut (StrStatus).") UUID statutId,
        @Parameter(description = "Vocation (StkVocation).") UUID vocationId,
        @Parameter(description = "Tranche (StkHierarchyId) : lots de cette tranche.") UUID trancheId) {
}
