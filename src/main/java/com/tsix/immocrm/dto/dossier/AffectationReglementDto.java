package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une ligne du règlement (`StlSettlementLine`) sur une échéance de ce dossier.")
public record AffectationReglementDto(

        @Schema(description = "Identifiant de la ligne (`StlSettlementLine.PKey`).")
        UUID ligneId,

        @Schema(description = "Échéance réglée (`ComDocument.PKey`).")
        UUID echeanceId,

        @Schema(description = "Libellé affiché par l'échéancier : code de catégorie, à défaut description de l'échéance.",
                example = "AVANCE")
        String echeanceLibelle,

        @Schema(description = "Date de l'échéance (`ComDocument.DocDate`).")
        LocalDateTime echeanceDate,

        @Schema(description = "Montant imputé à cette échéance (`StlSettlementLine.Amount`).", example = "50000.00")
        BigDecimal montant
) {
}
