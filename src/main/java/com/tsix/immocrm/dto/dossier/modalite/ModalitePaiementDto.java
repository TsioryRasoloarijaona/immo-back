package com.tsix.immocrm.dto.dossier.modalite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la modalité de paiement : le schéma de financement figé à la vente (`ComDocumentPaymentMethod`).")
public record ModalitePaiementDto(
        UUID id,
        LocalDateTime date,
        UUID categorieId,
        String categorie,
        @Schema(description = "Montant TTC.")
        BigDecimal montant,
        BigDecimal montantHT,
        @Schema(description = "Taux de TVA, ratio (0.2 = 20 %).")
        BigDecimal tauxTVA,
        @Schema(description = "Remise ancien client, ratio.")
        BigDecimal remise,
        String notes
) {
}
