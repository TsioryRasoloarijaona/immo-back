package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Nœud de l'arbre du bilan, à plat (ordre Sorting puis libellé, parent avant enfants).")
public record LignePrevisionDto(
        @Schema(description = "Catégorie (StlCategory.PKey).") UUID categorieId,
        @Schema(description = "Catégorie parente dans l'arbre ; null pour les racines Dépense / Recette.") UUID parentId,
        @Schema(description = "Code de la catégorie.") String code,
        @Schema(description = "Libellé de la catégorie.") String libelle,
        @Schema(description = "Côté recette (IsCredit).") boolean estRecette,
        @Schema(description = "Profondeur : 0 pour une racine.") int profondeur,
        @Schema(description = "Ligne d'unité de l'étude (mode par unité).") UUID uniteEtudeId,
        @Schema(description = "Code de l'unité de base.") String uniteCode,
        @Schema(description = "Catégorie de référence (mode pourcentage).") UUID categorieReferenceId,
        @Schema(description = "Libellé de la catégorie de référence.") String categorieReferenceLibelle,
        @Schema(description = "Prix unitaire ou fraction.") BigDecimal ratio,
        @Schema(description = "TVA (fraction).") BigDecimal tauxTva,
        @Schema(description = "Montant HT propre de la ligne.") BigDecimal montantHtPropre,
        @Schema(description = "Montant TTC propre de la ligne.") BigDecimal montantPropre,
        @Schema(description = "Total HT du nœud (propre + enfants).") BigDecimal montantHt,
        @Schema(description = "Total TTC du nœud (propre + enfants).") BigDecimal montant,
        @Schema(description = "Total TTC / total TTC de la racine du même côté ; 0 si racine nulle.") BigDecimal poids,
        @Schema(description = "Visible dans le module Projet.") boolean estPourProjet,
        @Schema(description = "Date « Traiter au début du projet », null sinon.") LocalDateTime dateApplication,
        @Schema(description = "Indivise : dateApplication renseignée (hors dépense d'exécution).") boolean indivise,
        @Schema(description = "Montant imposé par l'étude (terrain, frais, intérêts) : non modifiable.") boolean verrouillee,
        @Schema(description = "Total du nœud ventilé par tranche de l'étude (ordre des tranches).")
        List<VentilationPrevisionDto> ventilation) {
}
