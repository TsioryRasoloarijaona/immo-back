package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Même triade que {@link CategorieMontantDto}, mais une ligne de synthèse n'est pas une catégorie : elle
 * agrège l'échéancier selon FEATURE-fiche-dossier-alignement-ecran.md §5.
 */
@Schema(description = "Triade Montant / Réglé / Reste d'une ligne de synthèse financière.")
public record SyntheseMontantDto(

        @Schema(description = "Montant dû au titre de cette ligne.", example = "931500.00")
        BigDecimal montant,

        @Schema(description = "Part déjà encaissée.", example = "160000.00")
        BigDecimal regle,

        @Schema(description = "Solde restant — toujours `montant − regle`, vérifié sur les 358 "
                + "dossiers réels.", example = "771500.00")
        BigDecimal reste
) {
}
