package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

/** Filtres du solde fournisseurs, partagés par la liste et l'export ; liste vide ou absente = pas de filtre. */
public record FiltresSoldesFournisseurs(
        @Parameter(description = "Projets (PrjProject.id), paramètre répété ; un projet hors périmètre est ignoré.")
        List<UUID> projetIds,
        @Parameter(description = "Sociétés (CfgCompany.Code), paramètre répété.")
        List<String> societeCodes,
        @Parameter(description = "Nom du fournisseur, espaces et casse ignorés.")
        String search,
        @Parameter(description = "true : masque les lignes soldées (|reste| ≤ 0,05).")
        Boolean nonSoldes) {

    public FiltresSoldesFournisseurs {
        projetIds = projetIds == null ? List.of() : projetIds;
        societeCodes = societeCodes == null ? List.of() : societeCodes;
        nonSoldes = Boolean.TRUE.equals(nonSoldes);
    }
}
