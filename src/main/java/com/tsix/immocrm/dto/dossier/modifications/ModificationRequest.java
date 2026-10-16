package com.tsix.immocrm.dto.dossier.modifications;

import com.tsix.immocrm.domain.EtatModification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Saisie d'une demande de modification ; l'enregistrement remplace tous les champs.")
public record ModificationRequest(
        String description,
        @Schema(description = "Date de demande ; absente, aujourd'hui.")
        LocalDateTime dateDemande,
        LocalDateTime dateValidationTechnique,
        LocalDateTime dateValidationClient,
        @Schema(description = "Coût ; absent, 0.")
        BigDecimal cout,
        @Schema(description = "Prix de vente HT ; absent, 0.")
        BigDecimal prixHT,
        @Schema(description = "Prix de vente TTC ; absent, 0.")
        BigDecimal prixTTC,
        @Schema(description = "État ; absent, ENCOURS.")
        EtatModification etat,
        String notes
) {
}
