package com.tsix.immocrm.dto.rapport;

import com.tsix.immocrm.domain.rapport.TypeGraphique;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Graphique d'une section, tracé sur ses lignes.")
public record GraphiqueRapportDto(
        @Schema(description = "COLONNES (groupées), SECTEURS (une série) ou LIGNES.") TypeGraphique type,
        @Schema(description = "Titre du graphique.", example = "Articles vendus et libres par tranche") String titre,
        @Schema(description = "Colonne texte des catégories (axe ou secteurs).", example = "trancheCode")
        String colonneCategorie,
        @Schema(description = "Colonnes numériques, une série chacune.", example = "[\"nbVendus\", \"nbLibres\"]")
        List<String> colonnesValeurs) {
}
