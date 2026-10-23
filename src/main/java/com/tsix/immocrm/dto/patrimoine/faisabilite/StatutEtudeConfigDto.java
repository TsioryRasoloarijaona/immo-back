package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Statut d'étude (StkFeasibilityStudyStatus) ; en PATCH, un champ absent ou null reste inchangé.")
public record StatutEtudeConfigDto(
        @Schema(description = "PKey (lecture ; ignorée en écriture).") UUID id,
        @Schema(description = "Ordre d'affichage (Sorting), défaut 1.") Integer sorting,
        @Schema(description = "Code, obligatoire et unique.") String code,
        @Schema(description = "Libellé, obligatoire.") String description,
        @Schema(description = "Type d'état (AdmWorkFlowStateType.PKey, liste /choix/types-etat), obligatoire.") UUID typeEtatId,
        @Schema(description = "Lecture : libellé du type d'état.") String typeEtatLibelle,
        @Schema(description = "Durée de vie (Delay), défaut 0.") Integer delai,
        @Schema(description = "Couleur : nom de couleur .NET, défaut vide.") String couleur,
        @Schema(description = "Notes libres.") String notes,
        @Schema(description = "Noms OMRAN des champs obligatoires du statut (catalogue /champs-obligatoires).")
        List<String> champsObligatoires,
        @Schema(description = "Documents du statut, stockés sans effet ; une liste reçue remplace l'existante.")
        List<DocumentAJoindreDto> documents) {
}
