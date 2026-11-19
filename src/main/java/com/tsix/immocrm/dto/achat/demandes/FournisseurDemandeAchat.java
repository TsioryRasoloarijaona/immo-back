package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Fournisseur présélectionné d'une demande interne d'achat (BuySelectionSupplier).")
public record FournisseurDemandeAchat(
        @Schema(description = "PKey de la présélection (BuySelectionSupplier), pas celle du tiers.") UUID id,
        @Schema(description = "Fournisseur (CfgTier).") ElementDemandeAchat fournisseur,
        @Schema(description = "Référence O.D.T. (OfficeOrderRef).") String referenceOdt,
        @Schema(description = "Lettre de consultation envoyée le (SelectionLetterSendDate).") LocalDate lettreEnvoyee,
        @Schema(description = "Retrait du dossier le (TenderAcquisitionDate).") LocalDate retraitDossier,
        @Schema(description = "Dépôt de l'offre le (TenderDepositDate) ; null = aucune offre ne peut être créée.") LocalDate depot,
        @Schema(description = "Fournisseur écarté (IsRejected).") boolean ecarte,
        @Schema(description = "Notes.") String notes) {
}
