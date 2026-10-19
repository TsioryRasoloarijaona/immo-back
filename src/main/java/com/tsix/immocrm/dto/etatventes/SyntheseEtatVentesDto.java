package com.tsix.immocrm.dto.etatventes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Tableau tranche × catégorie × statut de l'état des ventes ; pourcentages calculés au front.")
public record SyntheseEtatVentesDto(
        @Schema(description = "Colonnes du tableau : LIBRE, statuts de dossier par StrStatus.Sorting, puis SANS_STATUT "
                + "s'il compte au moins un article.")
        List<StatutSynthese> statuts,
        @Schema(description = "Une ligne par tranche × catégorie, triées par code de tranche puis de catégorie.")
        List<LigneSynthese> lignes,
        TotauxSynthese totaux) {

    static final String PAR_STATUT = "Nombre d'articles par code de statut ; un statut absent vaut 0.";

    public record StatutSynthese(
            @Schema(example = "RESERVATION") String code,
            @Schema(example = "Réservation") String libelle,
            @Schema(description = "Rang de la colonne, croissant.") int ordre) {
    }

    public record LigneSynthese(
            UUID trancheId,
            String trancheCode,
            String trancheLibelle,
            UUID categorieId,
            String categorieCode,
            String categorieLibelle,
            long total,
            @Schema(description = PAR_STATUT)
            Map<String, Long> parStatut) {
    }

    public record TotalCategorieSynthese(
            UUID categorieId,
            String categorieCode,
            String categorieLibelle,
            long total,
            @Schema(description = PAR_STATUT)
            Map<String, Long> parStatut) {
    }

    public record TotalGeneralSynthese(
            long total,
            @Schema(description = PAR_STATUT)
            Map<String, Long> parStatut) {
    }

    public record TotauxSynthese(List<TotalCategorieSynthese> parCategorie, TotalGeneralSynthese general) {
    }
}
