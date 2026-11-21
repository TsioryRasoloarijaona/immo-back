package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.domain.planification.ContrainteTache;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Saisie d'une tâche : tout champ absent (ou null) reste inchangé, ou prend sa valeur par défaut à la création.")
public record TacheEcritureDto(
        @Schema(description = "Nouveau parent, de la même planification (indenter) ; ni la tâche ni un descendant.") UUID parentId,
        @Schema(description = "true = remonter la tâche en racine (désindenter) ; parentId alors ignoré.") Boolean racine,
        @Schema(description = "Code, non vide ; défaut « n » (racine) ou « <parent>.<n> ».") String code,
        @Schema(description = "Description ; défaut « Tâche <code> » ou « Sous-tâche <code> ».") String description,
        @Schema(description = "Notes.") String notes,
        @Schema(description = "Début ; défaut début de la planification (racine) ou du parent.") LocalDate dateDebut,
        @Schema(description = "Durée en jours entiers, ≥ 0 ; défaut 1 (racine) ou durée du parent.") Integer duree,
        @Schema(description = "Avancement de 0 à 100.") BigDecimal avancement,
        @Schema(description = "Alerte, en jours avant le début, ≥ 0.") Integer alerteAvantJours,
        @Schema(description = "Contrainte ; défaut AU_PLUS_TOT.") ContrainteTache contrainte,
        @Schema(description = "Date de la contrainte.") LocalDate dateContrainte,
        @Schema(description = "Échéance.") LocalDate echeance,
        @Schema(description = "Jalon.") Boolean jalon,
        @Schema(description = "Prédécesseurs (remplace la liste ; [] = aucun).") List<PredecesseurDto> predecesseurs,
        @Schema(description = "Ids d'intervenants de la planification (remplace la liste ; [] = aucun).") List<UUID> intervenants,
        @Schema(description = "Type de tâche (CfgGanttTaskType.PKey).") UUID typeTacheId) {
}
