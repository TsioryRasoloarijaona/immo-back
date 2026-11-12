package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Transition de statut d'une pièce d'achat (BuyDocumentWorkFlow), propre à une gestion, en lecture seule.")
public record TransitionAchatDto(
        @Schema(description = "BuyDocumentWorkFlow.PKey.") UUID id,
        @Schema(description = "Ordre dans la gestion (Sorting), null si absent.", example = "1") Integer ordre,
        @Schema(description = "Libellé de la transition.", example = "Validation pour paiement") String description,
        @Schema(description = "Code de la gestion (StrEntity.Code), null sans gestion.", example = "PrjInvoiceDebit")
        String gestionCode,
        @Schema(description = "Libellé de la gestion (StrEntity.Description), « Sans gestion » si aucune.")
        String gestionLibelle,
        @Schema(description = "Statut de départ (BuyDocumentStatus.PKey).") UUID statutDepartId,
        @Schema(description = "Code du statut de départ.", example = "EN COURS") String statutDepartCode,
        @Schema(description = "Statut d'arrivée (BuyDocumentStatus.PKey).") UUID statutArriveeId,
        @Schema(description = "Code du statut d'arrivée.", example = "VALIDE POUR PAIEMENT") String statutArriveeCode,
        @Schema(description = "Commentaire obligatoire (NotesMandatory = 1).") boolean noteObligatoire,
        @Schema(description = "Validée par le déclencheur de la pièce (IsValidatedByCfgTierTrigger = 1).")
        boolean valideeParDeclencheur,
        @Schema(description = "Rôles autorisés, triés par code ; vide si aucun rôle.") List<RoleTransitionDto> roles,
        @Schema(description = "Lignes d'historique (BuyDocumentStatusHistory) portant cette transition.", example = "1655")
        long nombrePassages,
        @Schema(description = "Date du dernier passage, null si jamais empruntée.") LocalDate dernierPassage,
        @Schema(description = "Gestion (StrEntity.PKey), null sans gestion ; à renvoyer tel quel dans un PUT.") UUID gestionId
) {
}
