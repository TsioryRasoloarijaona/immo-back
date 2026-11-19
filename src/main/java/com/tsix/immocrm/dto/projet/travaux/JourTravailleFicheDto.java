package com.tsix.immocrm.dto.projet.travaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Fiche « Jour travaillé » : valeurs enregistrées (forme de la saisie), statut et totaux calculés.")
public record JourTravailleFicheDto(
        @Schema(description = "GrhWorkedDay.Pkey.") UUID id,
        StatutJourTravailleDto statut,
        @Schema(description = "Valeurs enregistrées, aux noms de la saisie (prix et TVA toujours renseignés).")
        JourTravailleEcritureDto valeurs,
        @Schema(description = "Calculé : prix unitaire HT × quantité.") BigDecimal totalHT,
        @Schema(description = "Calculé : prix unitaire HT 1 × quantité 1.") BigDecimal totalHT1,
        @Schema(description = "Facture (StlInvoiceCode), lecture seule, null si non facturé.") String facture,
        @Schema(description = "Facture proforma (StlInvoiceProformaCode), lecture seule.") String factureProforma) {
}
