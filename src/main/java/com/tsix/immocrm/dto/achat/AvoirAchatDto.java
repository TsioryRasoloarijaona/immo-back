package com.tsix.immocrm.dto.achat;

import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.LigneDocumentAchatDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Avoir visible d'une facture (InvoiceSourceId = la facture) et ses lignes.")
public record AvoirAchatDto(
        @Schema(description = "Id de l'avoir (BuyDocument.PKey).") UUID id,
        @Schema(description = "Code de l'avoir.") String code,
        @Schema(description = "Date de l'avoir (DocDate).") LocalDate dateDocument,
        @Schema(description = "Lignes de l'avoir, par ordre (Sorting), colonnes calculées comprises.")
        List<LigneDocumentAchatDto> lignes) {
}
