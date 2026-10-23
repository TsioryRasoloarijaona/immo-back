package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Transition d'étude (StkFeasibilityStudyWorkFlow) ; en PATCH, un champ absent ou null reste inchangé.")
public record TransitionEtudeConfigDto(
        @Schema(description = "PKey (lecture ; ignorée en écriture).") UUID id,
        @Schema(description = "Ordre d'affichage (Sorting), défaut 1.") Integer sorting,
        @Schema(description = "Libellé, obligatoire.") String description,
        @Schema(description = "Statut de départ, obligatoire, différent de l'arrivée.") UUID statutDepartId,
        @Schema(description = "Lecture : libellé du statut de départ.") String statutDepartLibelle,
        @Schema(description = "Statut d'arrivée, obligatoire.") UUID statutArriveeId,
        @Schema(description = "Lecture : libellé du statut d'arrivée.") String statutArriveeLibelle,
        @Schema(description = "Noms OMRAN des champs exigés pour l'emprunter (catalogue /champs-obligatoires).")
        List<String> champsObligatoires,
        @Schema(description = "NotesMandatory, défaut faux, stocké sans effet.") Boolean noteObligatoire,
        @Schema(description = "WithNotification, défaut faux, stocké sans effet.") Boolean avecNotification,
        @Schema(description = "SendEmailToUsers, défaut faux, stocké sans effet (aucun e-mail).") Boolean envoyerEmail,
        @Schema(description = "Rôles autorisés (AdmRole.PKey, liste /api/referentiels/roles) ; doublons ignorés ; vide permis.")
        List<UUID> roles,
        @Schema(description = "Documents exigés : une pièce jointe de l'étude de chaque type ; une liste reçue remplace l'existante.")
        List<DocumentAJoindreDto> documents) {
}
