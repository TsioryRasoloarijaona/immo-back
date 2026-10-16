package com.tsix.immocrm.dto.dossier.cadeaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Cadeau du catalogue (`ComGift`) ou raison (`ComGiftReason`, sans prix ni stock).")
public record CadeauCatalogueDto(
        UUID id,
        String code,
        String libelle,
        BigDecimal prix,
        @Schema(description = "Quantités reçues − cadeaux livrés ; null pour une raison.")
        Integer enStock
) {
}
