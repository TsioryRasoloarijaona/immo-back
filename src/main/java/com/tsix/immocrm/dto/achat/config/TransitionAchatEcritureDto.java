package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Transition de statut d'achat (POST et PUT) ; un PUT remplace la transition entière, rôles compris.")
public record TransitionAchatEcritureDto(
        @Schema(description = "Ordre dans la gestion (Sorting).", example = "1") Integer ordre,
        @Schema(description = "Libellé, ≤ 255.", example = "Validation pour paiement") String description,
        @Schema(description = "Statut de départ (BuyDocumentStatus.PKey), obligatoire.") UUID statutDepartId,
        @Schema(description = "Statut d'arrivée (BuyDocumentStatus.PKey), obligatoire, différent du départ.") UUID statutArriveeId,
        @Schema(description = "Gestion (StrEntity.PKey) ; absent = « Sans gestion ».") UUID gestionId,
        @Schema(description = "Commentaire obligatoire (NotesMandatory) ; absent = false.") Boolean noteObligatoire,
        @Schema(description = "Validée par le déclencheur (IsValidatedByCfgTierTrigger) ; absent = false.")
        Boolean valideeParDeclencheur,
        @Schema(description = "Rôles autorisés (AdmRole.PKey), remplacés en bloc ; vide ou absent = aucun rôle.")
        List<UUID> roleIds,
        @Schema(description = "Champs obligatoires de la pièce (noms du catalogue des statuts) ; absent = inchangés.")
        List<String> champsObligatoires,
        @Schema(description = "Champs obligatoires du fournisseur (noms du catalogue) ; absent = inchangés.")
        List<String> champsObligatoiresFournisseur,
        @Schema(description = "Avec notification (WithNotification) ; absent = inchangé (false à la création).")
        Boolean avecNotification,
        @Schema(description = "Avec envoi des e-mails (SendEmailToUsers) ; absent = inchangé (false à la création).")
        Boolean envoiEmails,
        @Schema(description = "Notifier le déclencheur (NotifyCfgTierTrigger) ; absent = inchangé (false à la création).")
        Boolean notifierDeclencheur,
        @Schema(description = "Envoyer un e-mail au déclencheur (SendEMailToCfgTierTrigger) ; absent = inchangé.")
        Boolean emailDeclencheur
) {
    public TransitionAchatEcritureDto {
        noteObligatoire = Boolean.TRUE.equals(noteObligatoire);
        valideeParDeclencheur = Boolean.TRUE.equals(valideeParDeclencheur);
        roleIds = Objects.requireNonNullElse(roleIds, List.of());
    }
}
