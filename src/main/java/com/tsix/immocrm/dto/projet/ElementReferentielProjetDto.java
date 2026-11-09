package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Élément d'une liste de choix du module Projet (type de lot, statut de facture, catégorie).")
public record ElementReferentielProjetDto(
        @Schema(description = "PKey de l'élément.") UUID id,
        @Schema(description = "Code.", example = "20 TRAVAUX") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "Ordre d'affichage (Sorting), null si la table n'en a pas.") Integer ordre
) {
}
