package com.tsix.immocrm.dto.rapport;

import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Colonne d'une section de rapport.")
public record ColonneRapportDto(
        @Schema(description = "Clé de la valeur dans chaque ligne.", example = "nbArticles") String nom,
        @Schema(description = "En-tête affiché.", example = "Articles") String libelle,
        @Schema(description = "Type : alignement et format (POURCENTAGE vaut 0–100).") TypeColonne type,
        @Schema(description = "En-tête de groupe de colonnes, null si aucun.", example = "Surfaces") String groupe,
        @Schema(description = "Total calculé par le serveur (totaux, sousTotaux) ; AUCUNE sinon.")
        FonctionTotal fonctionTotal,
        @Schema(description = "NOMBRE : décimales à afficher (0 par défaut).", example = "2") int decimales) {
}
