package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Élément d'une liste de choix du module Chantier (type de lot, statut de facture, catégorie).")
public record ReferentielChantierDto(
        @Schema(description = "Identifiant (PKey).") UUID id,
        @Schema(description = "Code.", example = "20") String code,
        @Schema(description = "Libellé.", example = "TRAVAUX") String description,
        @Schema(description = "Ordre d'affichage (Sorting) ; null si la table n'en porte pas (types de lot).")
        Integer ordre) {
}
