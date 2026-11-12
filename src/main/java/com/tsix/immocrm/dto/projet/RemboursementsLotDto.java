package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Sous-fenêtre Remboursements du lot : quatre listes.")
public record RemboursementsLotDto(
        @Schema(description = "Échéances de remboursement de la retenue de garantie, par ordre.") List<EcheanceRgLotDto> echeances,
        @Schema(description = "Réceptions du lot, par ordre.") List<ReceptionLotDto> receptions,
        @Schema(description = "Remboursements de retenue de garantie (MARKET_WARRANTY).") List<AvanceLotDto> remboursementsRg,
        @Schema(description = "Remboursements du compte prorata (MARKET_PROPORTION).") List<AvanceLotDto> remboursementsProrata
) {

    @Schema(description = "Échéance de remboursement de la retenue de garantie (PrjMarketWarrantyLine).")
    public record EcheanceRgLotDto(
            @Schema(description = "PrjMarketWarrantyLine.PKey.") UUID id,
            @Schema(description = "Ordre (Sorting).") Integer ordre,
            @Schema(description = "Description.") String description,
            @Schema(description = "Délai en jours (Delay).") Integer delaiJours,
            @Schema(description = "Date d'échéance (EcheanceDate).") LocalDate echeance,
            @Schema(description = "Pourcentage remboursé (Ratio × 100).", example = "50") BigDecimal pourcentage
    ) {
    }

    @Schema(description = "Réception du lot (PrjMarketReception).")
    public record ReceptionLotDto(
            @Schema(description = "PrjMarketReception.PKey.") UUID id,
            @Schema(description = "Ordre (Sorting).") Integer ordre,
            @Schema(description = "Code de la réception.") String code,
            @Schema(description = "Date de réception (DocDate).") LocalDate dateReception,
            @Schema(description = "Réception demandée par le fournisseur (RequiredBySupplier).") boolean demandeeParFournisseur,
            @Schema(description = "Lieu (MeetingPlace).") String lieu,
            @Schema(description = "Type de réception (PrjMarketReceptionType.Description).") String typeDescription,
            @Schema(description = "Genre de réception (PrjMarketReceptionKind.Description).") String genreDescription,
            @Schema(description = "Genre définitif (PrjMarketReceptionKind.IsFinal).") boolean definitive
    ) {
    }
}
