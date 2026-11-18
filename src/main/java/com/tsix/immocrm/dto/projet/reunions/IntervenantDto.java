package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Intervenant d'une réunion (BulDelegate) : un fournisseur, sur un lot, avec ses contacts.")
public record IntervenantDto(
        @Schema(description = "BulDelegate.PKey.") UUID id,
        @Schema(description = "O.D.T.") Integer ordre,
        @Schema(description = "Effectif présent sur le chantier.") Integer effectif,
        @Schema(description = "Lot (PrjMarket.PKey) ; null si aucun.") UUID lotId,
        @Schema(description = "Libellé du lot.") String lot,
        @Schema(description = "Fournisseur (CfgTier.PKey) ; null si aucun.") UUID fournisseurId,
        @Schema(description = "Raison sociale du fournisseur.") String fournisseur,
        @Schema(description = "Note (BulDelegate.Notes).") String note,
        @Schema(description = "Contacts de l'intervenant (BulDelegateContact), par O.D.T.") List<ContactReunionDto> contacts) {
}
