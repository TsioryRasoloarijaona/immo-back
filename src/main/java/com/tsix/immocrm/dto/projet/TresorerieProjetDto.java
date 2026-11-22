package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Trésorerie projet d'une année civile : arbre mensuel des prévisions, onglets Projet et Fournisseur.")
public record TresorerieProjetDto(
        @Schema(description = "Année civile affichée.", example = "2026") int annee,
        @Schema(description = "Projets proposés au sélecteur : projets habilités de la société.") List<ProjetChoixDto> projets,
        @Schema(description = "Libellés des 12 mois, janvier en premier.") List<String> mois,
        @Schema(description = "Les deux onglets de l'arbre.") OngletsTresorerieDto onglets) {

    @Schema(description = "Onglets de la Trésorerie projet ; mêmes lignes, total identique.")
    public record OngletsTresorerieDto(
            @Schema(description = "Racines Encaissement / Décaissement → projet → fournisseur.")
            List<NoeudTresorerieDto> projet,
            @Schema(description = "Racines Encaissement / Décaissement → fournisseur, sans niveau projet.")
            List<NoeudTresorerieDto> fournisseur) {
    }
}
