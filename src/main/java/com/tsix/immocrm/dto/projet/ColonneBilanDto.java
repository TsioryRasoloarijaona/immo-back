package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Colonne du Bilan projet (8, ordre fixe).")
public record ColonneBilanDto(
        @Schema(description = "Code de la colonne, clé de `valeurs` et valeur de colonne1 / colonne2.",
                example = "etudeFaisabilite") String code,
        @Schema(description = "Libellé affiché (celui d'OMRAN).", example = "ÉTUDE DE FAISABILITÉ") String libelle) {
}
