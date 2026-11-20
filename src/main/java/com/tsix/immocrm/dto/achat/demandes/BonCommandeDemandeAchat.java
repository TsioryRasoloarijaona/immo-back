package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Pièce d'achat liée à la demande, autre qu'un devis (onglet Bons de commande).")
public record BonCommandeDemandeAchat(
        @Schema(description = "PKey de la pièce (BuyDocument).") UUID id,
        @Schema(description = "Code de la pièce.") String code,
        @Schema(description = "Nature (DocType) : ORDER, DELIVRY, INVOICE…") String nature,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate date,
        @Schema(description = "Fournisseur (CfgTier), null si absent.") ElementDemandeAchat fournisseur,
        @Schema(description = "Statut de la pièce, null si absent.") StatutDemandeAchat statut,
        @Schema(description = "Total TTC (AmountTTC).") BigDecimal montantTTC) {
}
