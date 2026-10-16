package com.tsix.immocrm.dto.dossier.liste;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

/** Σ en tête des colonnes de montants par nature, sur toutes les lignes filtrées de la liste des dossiers. */
public record TotauxDossiersDto(
        @Schema(description = "Nombre de dossiers additionnés (lots libres exclus).", example = "312")
        long nombreDossiers,

        @Schema(description = "Σ par colonne (clé = nom du catalogue) : les 22 montants additionnables, jamais les taux.",
                example = "{\"avance\": 12500000.0, \"avanceReglee\": 9800000.0}")
        Map<String, BigDecimal> sommes
) {
}
