package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Statut de pièce d'achat (BuyDocumentStatus), tel que la fiche OMRAN « Statuts Documents Achats » l'affiche.")
public record StatutAchatConfigDto(
        @Schema(description = "BuyDocumentStatus.PKey.") UUID id,
        @Schema(description = "Code du statut.", example = "EN COURS") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre (Sorting, O.D.T. dans OMRAN), null si absent.", example = "10") Integer ordre,
        @Schema(description = "Type d'état (AdmWorkFlowStateType.Description), null si absent.", example = "Nouveau")
        String categorie,
        @Schema(description = "Nom de couleur .NET (ColorName), null si vide (OMRAN l'affiche « White »).", example = "Pink")
        String couleur,
        @Schema(description = "OK pour paiement (IsOkForPayement) ; NULL lu false.") boolean okPourPaiement,
        @Schema(description = "Validé en comptabilité (IsValidatedOnAccounting) ; NULL lu false.") boolean valideComptabilite,
        @Schema(description = "Pièce en lecture seule dans ce statut (IsReadOnly) ; NULL lu false.") boolean lectureSeule,
        @Schema(description = "Durée de vie en jours (Delay), 0 = durée infinie, null si absente.", example = "0")
        Integer dureeVieJours,
        @Schema(description = "Nombre de pièces (BuyDocument) portant ce statut aujourd'hui.", example = "473") long nombrePieces,
        @Schema(description = "Type d'état (AdmWorkFlowStateType.PKey), null si absent.") UUID typeId,
        @Schema(description = "Image de la bibliothèque (CfgImage.PKey), null si aucune ; octets via /images/{id}/contenu.")
        UUID imageId,
        @Schema(description = "Notes du statut, null si vides.") String notes,
        @Schema(description = "Champs obligatoires de la pièce (noms OMRAN du catalogue piece), stockés sans effet.")
        List<String> champsObligatoires,
        @Schema(description = "Champs obligatoires du fournisseur (noms OMRAN du catalogue fournisseur), stockés sans effet.")
        List<String> champsObligatoiresFournisseur,
        @Schema(description = "Documents à joindre (BuyDocumentStatusDocumentToAttach) ; vide si aucun.")
        List<DocumentAJoindreAchatDto> documentsAJoindre,
        @Schema(description = "Transitions citant ce statut en départ ou en arrivée.", example = "12") long nombreTransitions,
        @Schema(description = "Lignes d'historique (BuyDocumentStatusHistory) portant ce statut.", example = "1655")
        long nombreHistoriques
) {
}
