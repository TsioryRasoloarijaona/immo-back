package com.tsix.immocrm.dto.parcours;

import com.tsix.immocrm.dto.prospect.CritereDoublon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche prospect ou autre prospection au même numéro (9 derniers chiffres) ou au même e-mail.")
public record DoublonTriDto(
        @Schema(description = "PROSPECT = fiche CfgTier ; PROSPECTION = autre ComProspection (pré-prospection comprise).")
        NatureDoublon nature,
        @Schema(description = "Identifiant de la fiche (CfgTier) ou de la prospection (ComProspection).")
        UUID id,
        String code,
        String nom,
        @Schema(description = "Critères communs : TELEPHONE et/ou EMAIL.")
        List<CritereDoublon> criteres,
        @Schema(description = "Prospection clôturée ; toujours null pour une fiche prospect.")
        Boolean fermee
) {

    public enum NatureDoublon {
        PROSPECT,
        PROSPECTION
    }
}
