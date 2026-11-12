package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Pièce liée visible (bon de livraison ou facture d'un bon de commande), mêmes valeurs que la liste.")
public record PieceLieeAchatDto(
        @Schema(description = "Id de la pièce liée (BuyDocument.PKey).") UUID id,
        @Schema(description = "Code de la pièce.") String code,
        @Schema(description = "Date de la pièce (DocDate).") LocalDate dateDocument,
        @Schema(description = "Code du statut.") String statutCode,
        @Schema(description = "Libellé du statut.") String statutDescription,
        @Schema(description = "Couleur du statut (BuyDocumentStatus.ColorName).", example = "Green") String statutCouleur,
        @Schema(description = "Référence interne (DocRef).") String reference,
        @Schema(description = "Total HT (AmountHT).") BigDecimal montantHT,
        @Schema(description = "Total TVA (AmountVat).") BigDecimal montantTVA,
        @Schema(description = "Total TTC (AmountTTC).") BigDecimal montantTTC,
        @Schema(description = "Code du projet.") String projetCode,
        @Schema(description = "Code du lot de marché.") String lotCode) {

    public static PieceLieeAchatDto de(DocumentAchatListItemDto p) {
        return new PieceLieeAchatDto(p.id(), p.code(), p.dateDocument(), p.statutCode(), p.statutDescription(),
                p.statutCouleur(), p.reference(), p.montantHT(), p.montantTVA(), p.montantTTC(), p.projetCode(),
                p.lotCode());
    }
}
