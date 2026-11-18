package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Colonnes de pièce d'une ligne de suivi des avances ou des retenues de garantie.")
public record PieceSuiviAchatDto(
        @Schema(description = "Pièce (BuyDocument.PKey) : lien vers la fiche.") UUID pieceId,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate date,
        @Schema(description = "Code de la pièce.") String code,
        @Schema(description = "Description de la pièce.") String description,
        @Schema(description = "Entité fonctionnelle (StrEntity.Description), null sans entité.") String nature,
        @Schema(description = "Libellé du statut, null sans statut.") String statut,
        @Schema(description = "Vrai si le statut est de type Annulation : ligne listée, exclue des totaux.") boolean annulee,
        @Schema(description = "Nom du fournisseur (CfgTier.Description), null sans fournisseur.") String fournisseur,
        @Schema(description = "Description du projet, null sans projet.") String projet,
        @Schema(description = "Description de la société.") String societe,
        @Schema(description = "Description du lot (PrjMarket), null sans lot.") String lot,
        @Schema(description = "Lot (PrjMarket.PKey), null sans lot.") UUID lotId) {
}
