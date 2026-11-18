package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Grille entière des prévisions chantier : remplace les lignes du projet.")
public record PrevisionsChantierRequest(
        @Schema(description = "Lignes, une par type de travaux au plus ; liste vide = tout supprimer.")
        List<LignePrevisionChantierSaisie> lignes
) {

    @Schema(description = "Ligne saisie de la grille.")
    public record LignePrevisionChantierSaisie(
            @Schema(description = "Type de travaux (GrhWorkedDayType.PKey), obligatoire et unique dans la grille.")
            UUID typeTravauxId,
            @Schema(description = "Quantité ≥ 0.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT ≥ 0.") BigDecimal prixUnitaireHT,
            @Schema(description = "TVA en pourcentage ≥ 0 (stockée en ratio).", example = "20") BigDecimal tva,
            @Schema(description = "Notes (≤ 1000 caractères).") String notes
    ) {
    }
}
