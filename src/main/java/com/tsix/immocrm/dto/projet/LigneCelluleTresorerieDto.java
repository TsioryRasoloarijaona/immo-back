package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ligne source d'une cellule de la Trésorerie projet (prévision de lot ou reste d'une facture).")
public record LigneCelluleTresorerieDto(
        @Schema(description = "Date retenue : aujourd'hui si la ligne est reportée, sinon la date d'origine.") LocalDate date,
        @Schema(description = "Date d'origine (PaymentDate du lot, MaturityDate de la facture).") LocalDate dateOrigine,
        @Schema(description = "Libellé de la ligne (lot ou pièce).") String libelle,
        @Schema(description = "Groupe : fournisseur, sinon description du lot ou du fournisseur saisi.") String groupe,
        @Schema(description = "Montant compté dans la cellule (après ventilation).") BigDecimal montant,
        @Schema(description = "Type de source.", example = "PrjMarketPrevision") String type,
        @Schema(description = "true si la date d'origine est passée et la ligne reportée à aujourd'hui.") boolean reporte,
        @Schema(description = "Identifiant de la source (PrjMarketPaymentPrevision.PKey ou BuyDocument.PKey).") UUID objetId,
        @Schema(description = "Espace de nom de la source, pour ouvrir sa fiche.") String objetNamespace) {
}
