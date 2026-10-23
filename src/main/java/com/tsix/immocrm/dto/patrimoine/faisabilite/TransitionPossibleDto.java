package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Transition empruntable depuis le statut courant de l'étude par un rôle de l'appelant.")
public record TransitionPossibleDto(
        @Schema(description = "Transition (StkFeasibilityStudyWorkFlow.PKey).") UUID id,
        @Schema(description = "Libellé de la transition.") String description,
        @Schema(description = "Statut de départ (= statut courant).") UUID statutDepartId,
        @Schema(description = "Libellé du statut de départ.") String statutDepartLibelle,
        @Schema(description = "Statut d'arrivée.") UUID statutArriveeId,
        @Schema(description = "Libellé du statut d'arrivée.") String statutArriveeLibelle,
        @Schema(description = "Champs obligatoires de la transition, ordre du catalogue.") List<ChampTransitionDto> champsObligatoires,
        @Schema(description = "Documents exigés : une pièce jointe de l'étude de ce type pour chacun.")
        List<DocumentTransitionDto> documentsRequis,
        @Schema(description = "Vrai si l'étude n'est pas bloquée, tous les champs renseignés et tous les documents fournis.")
        boolean possible) {

    @Schema(description = "Champ obligatoire d'une transition et son état sur l'étude.")
    public record ChampTransitionDto(
            @Schema(description = "Champ du CRM (nom de la fiche).", example = "superficie") String champ,
            @Schema(description = "Libellé OMRAN du champ.") String libelle,
            @Schema(description = "Faux si vide : texte vide, date absente ; un nombre (même 0) est toujours renseigné.")
            boolean renseigne) {
    }

    @Schema(description = "Document à fournir pour une transition.")
    public record DocumentTransitionDto(
            @Schema(description = "Type de document (AdmAttachedFileType.PKey).") UUID typeId,
            @Schema(description = "Libellé du type de document.") String typeLibelle,
            @Schema(description = "Vrai si l'étude a une pièce jointe vivante de ce type.") boolean fourni) {
    }
}
