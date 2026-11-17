package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche réception du lot (PrjMarketReception) avec ses réserves et ses tiers invités.")
public record ReceptionDetailDto(
        @Schema(description = "PrjMarketReception.PKey.") UUID id,
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Ordre (sorting).") Integer ordre,
        @Schema(description = "Code.") String code,
        @Schema(description = "Description.") String description,
        @Schema(description = "Date de réception (DocDate).") LocalDate dateReception,
        @Schema(description = "Lieu (MeetingPlace).") String lieu,
        @Schema(description = "Demandée par le fournisseur.") boolean demandeeParFournisseur,
        @Schema(description = "Type (PrjMarketReceptionType.PKey).") UUID typeId,
        @Schema(description = "Libellé du type.") String typeDescription,
        @Schema(description = "Genre (PrjMarketReceptionKind.PKey).") UUID genreId,
        @Schema(description = "Libellé du genre.") String genreDescription,
        @Schema(description = "Genre définitif (IsFinal).") boolean definitive,
        @Schema(description = "Réserves par ordre.") List<ReserveDto> reserves,
        @Schema(description = "Tiers invités par ordre.") List<TiersInviteDto> tiersInvites
) {

    @Schema(description = "Réserve d'une réception.")
    public record ReserveDto(
            @Schema(description = "PrjMarketReceptionReserve.PKey.") UUID id,
            @Schema(description = "Ordre.") Integer ordre,
            @Schema(description = "Description.") String description,
            @Schema(description = "Réserve levée (IsFixed).") boolean levee
    ) {
    }

    @Schema(description = "Tiers invité à une réception.")
    public record TiersInviteDto(
            @Schema(description = "PrjMarketReceptionCfgTier.PKey.") UUID id,
            @Schema(description = "Ordre.") Integer ordre,
            @Schema(description = "CfgTier.PKey.") UUID tiersId,
            @Schema(description = "Nom du tiers : raison sociale, sinon description, sinon prénom et nom.") String tiers
    ) {
    }
}
