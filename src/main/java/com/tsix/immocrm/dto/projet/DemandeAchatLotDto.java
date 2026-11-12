package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Demande interne d'achat du lot (BuySelection), onglet « Demande interne d'achat » de la fiche lot.")
public record DemandeAchatLotDto(
        @Schema(description = "BuySelection.PKey.") UUID id,
        @Schema(description = "Procédure d'achat (BuySelectionKind.Description), null si absente.") String procedureAchat,
        @Schema(description = "Libellé du statut (StrStatus.Description), null si absent.") String statutDescription,
        @Schema(description = "Nom .NET de la couleur du statut (StrStatus.ColorName), null si absent.") String statutCouleur,
        @Schema(description = "Code de la demande.") String code,
        @Schema(description = "Date de la demande (DocDate).") LocalDate date,
        @Schema(description = "Description de la demande.") String description,
        @Schema(description = "Fournisseur (CfgTier.PKey), null si absent.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur, null si absent.") String fournisseurNom,
        @Schema(description = "Notes de la demande.") String notes
) {
}
