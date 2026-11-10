package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut de pièce d'achat (BuyDocumentStatus), en lecture seule.")
public record StatutAchatConfigDto(
        @Schema(description = "BuyDocumentStatus.PKey.") UUID id,
        @Schema(description = "Code du statut.", example = "EN COURS") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre (Sorting), null si absent.", example = "10") Integer ordre,
        @Schema(description = "Type d'état (AdmWorkFlowStateType.Description), null si absent.", example = "Nouveau")
        String categorie,
        @Schema(description = "Nom de couleur .NET (ColorName) tel quel, null si absent.", example = "Pink") String couleur,
        @Schema(description = "OK pour paiement (IsOkForPayement) ; NULL lu false.") boolean okPourPaiement,
        @Schema(description = "Validé en comptabilité (IsValidatedOnAccounting) ; NULL lu false.") boolean valideComptabilite,
        @Schema(description = "Pièce en lecture seule dans ce statut (IsReadOnly) ; NULL lu false.") boolean lectureSeule,
        @Schema(description = "Durée de vie en jours (Delay), null si absente.", example = "0") Integer dureeVieJours,
        @Schema(description = "Nombre de pièces (BuyDocument) portant ce statut aujourd'hui.", example = "473") long nombrePieces
) {
}
