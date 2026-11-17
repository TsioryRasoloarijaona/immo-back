package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche réception envoyée entière, avec ses réserves et ses tiers invités.")
public record ReceptionSaisieDto(
        @Schema(description = "Ordre (sorting), facultatif.") Integer ordre,
        @Schema(description = "Code, obligatoire, 255 caractères au plus.") String code,
        @Schema(description = "Description, 255 caractères au plus.") String description,
        @Schema(description = "Date de réception (DocDate), obligatoire.") LocalDate dateReception,
        @Schema(description = "Lieu (MeetingPlace), 255 caractères au plus.") String lieu,
        @Schema(description = "Demandée par le fournisseur (RequiredBySupplier).") boolean demandeeParFournisseur,
        @Schema(description = "Type (PrjMarketReceptionType.PKey), obligatoire.") UUID typeId,
        @Schema(description = "Genre (PrjMarketReceptionKind.PKey), obligatoire.") UUID genreId,
        @Schema(description = "Toutes les réserves : id inconnu = insertion, présent = modification, absente = suppression.")
        List<ReserveSaisieDto> reserves,
        @Schema(description = "Tous les tiers invités, même règle ; un tiers une seule fois.") List<TiersInviteSaisieDto> tiersInvites
) {

    @Schema(description = "Réserve d'une réception (PrjMarketReceptionReserve).")
    public record ReserveSaisieDto(
            @Schema(description = "Id ; UUID v4 généré par le front pour une ligne neuve.") UUID id,
            @Schema(description = "Ordre, facultatif.") Integer ordre,
            @Schema(description = "Description, obligatoire, 255 caractères au plus.") String description,
            @Schema(description = "Réserve levée (IsFixed).") boolean levee
    ) {
    }

    @Schema(description = "Tiers invité à une réception (PrjMarketReceptionCfgTier).")
    public record TiersInviteSaisieDto(
            @Schema(description = "Id ; UUID v4 généré par le front pour une ligne neuve.") UUID id,
            @Schema(description = "Ordre, facultatif.") Integer ordre,
            @Schema(description = "Tiers existant (CfgTier.PKey), toute famille.") UUID tiersId
    ) {
    }
}
