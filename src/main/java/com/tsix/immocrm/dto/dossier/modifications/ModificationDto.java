package com.tsix.immocrm.dto.dossier.modifications;

import com.tsix.immocrm.domain.EtatModification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Demande de modification du dossier (`ComModification`).")
public record ModificationDto(
        UUID id,
        String description,
        LocalDateTime dateDemande,
        LocalDateTime dateValidationTechnique,
        LocalDateTime dateValidationClient,
        @Schema(description = "Coût interne de la modification.")
        BigDecimal cout,
        @Schema(description = "Prix de vente HT facturé au client.")
        BigDecimal prixHT,
        @Schema(description = "Prix de vente TTC facturé au client ; compté dans le dossier si l'état est ACCEPTEE ou REALISED.")
        BigDecimal prixTTC,
        @Schema(description = "État ; null si la base porte une valeur hors énumération.")
        EtatModification etat,
        @Schema(description = "Libellé français de l'état.")
        String etatLibelle,
        String notes
) {
}
