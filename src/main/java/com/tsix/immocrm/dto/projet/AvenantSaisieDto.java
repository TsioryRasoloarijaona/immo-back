package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche avenant envoyée entière (en-tête, 4 retenues, lignes).")
public record AvenantSaisieDto(
        @Schema(description = "Date de l'avenant (DocDate), obligatoire.") LocalDate date,
        @Schema(description = "Référence interne (DocRef), 255 caractères au plus.") String referenceInterne,
        @Schema(description = "Notes, 1000 caractères au plus.") String notes,
        @Schema(description = "Retenue de garantie.") RetenueAvenantDto garantie,
        @Schema(description = "Retenue du compte prorata.") RetenueAvenantDto prorata,
        @Schema(description = "Assurance DEC.") RetenueAvenantDto assuranceDEC,
        @Schema(description = "Assurance TRC.") RetenueAvenantDto assuranceTRC,
        @Schema(description = "Toutes les lignes : id inconnu = insertion, id présent = modification, absente = suppression.")
        List<LigneAvenantSaisieDto> lignes
) {

    @Schema(description = "Ligne d'avenant saisie (PrjClauseLine).")
    public record LigneAvenantSaisieDto(
            @Schema(description = "Id de la ligne ; UUID v4 généré par le front pour une ligne neuve.") UUID id,
            @Schema(description = "Ordre (Sorting), obligatoire.") Integer ordre,
            @Schema(description = "Code, 255 caractères au plus.") String code,
            @Schema(description = "Désignation (Description), obligatoire, 500 caractères au plus.") String designation,
            @Schema(description = "Unité, 255 caractères au plus.") String unite,
            @Schema(description = "Quantité ≥ 0, stockée à 6 décimales.") BigDecimal quantite,
            @Schema(description = "Prix unitaire HT ≥ 0.") BigDecimal prixUnitaireHT,
            @Schema(description = "TVA en pourcentage 0–100.", example = "20") BigDecimal tauxTVA,
            @Schema(description = "FODEC en pourcentage 0–100.", example = "1") BigDecimal tauxFODEC,
            @Schema(description = "Catégorie (StlCategory.PKey), obligatoire.") UUID categorieId,
            @Schema(description = "Ligne du bordereau définitif de ce lot (hors stock), facultative.") UUID ligneBordereauId,
            @Schema(description = "Notes, 1000 caractères au plus.") String notes
    ) {
    }
}
