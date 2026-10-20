package com.tsix.immocrm.services.etatventes;

import com.tsix.immocrm.domain.FamilleArticle;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.UUID;

/** Filtres du tableau Libres / Réservés / Vendus et de son export ; même base que la liste ({@link FiltresEtatVentes}). */
public record FiltresVendusLibres(
        @Parameter(description = "Id(s) de tranche (CfgTranche.id), répétable.") List<UUID> trancheId,
        @Parameter(description = "Id du projet (CfgProject.id) de la tranche.") UUID projetId,
        @Parameter(description = "Famille(s), répétable ; absent = les quatre ; inconnue = 400.", example = "APPARTEMENT")
        List<FamilleArticle> famille) {

    public FiltresEtatVentes versFiltres() {
        return FiltresEtatVentes.synthese(trancheId, projetId, null, famille);
    }
}
