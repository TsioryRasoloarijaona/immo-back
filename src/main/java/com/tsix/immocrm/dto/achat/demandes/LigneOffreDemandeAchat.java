package com.tsix.immocrm.dto.achat.demandes;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne d'une offre (BuyDocumentLine).")
public record LigneOffreDemandeAchat(
        @Schema(description = "PKey de la ligne (BuyDocumentLine).") UUID id,
        @Schema(description = "Ordre (Sorting).") Integer ordre,
        @Schema(description = "Code, égal à celui de la ligne du bordereau de la demande.") String code,
        @Schema(description = "Désignation.") String designation,
        @Schema(description = "Unité.") String unite,
        @Schema(description = "Quantité.") BigDecimal quantite,
        @Schema(description = "Prix unitaire HT saisi.") BigDecimal prixUnitaireHT,
        @Schema(description = "Remise en fraction (0,1 = 10 %).") BigDecimal remise,
        @Schema(description = "Prix unitaire HT après remise : prixUnitaireHT × (1 − remise).") BigDecimal prixApresRemiseHT,
        @Schema(description = "Taux de TVA en fraction.") BigDecimal tauxTVA,
        @Schema(description = "Total HT après remise : prixApresRemiseHT × quantite.") BigDecimal totalHT) {
}
