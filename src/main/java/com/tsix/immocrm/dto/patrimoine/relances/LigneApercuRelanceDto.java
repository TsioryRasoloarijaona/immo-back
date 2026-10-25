package com.tsix.immocrm.dto.patrimoine.relances;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Un dossier de vente retenu par les premières relances.")
public record LigneApercuRelanceDto(
        @Schema(description = "Id du dossier (ComFolder.PKey).")
        UUID dossierId,
        @Schema(description = "Code du dossier.", example = "APF 2022/0012")
        String code,
        @Schema(description = "Nom du client.")
        String client,
        @Schema(description = "Code de l'article principal (StkItem).")
        String article,
        @Schema(description = "Libellé du statut du dossier.")
        String statut,
        @Schema(description = "Échéance de 1re relance actuelle du dossier (Revival1ValidityDate), null si aucune.")
        LocalDate echeanceActuelle,
        @Schema(description = "Vrai si une alerte sera créée : échéance changée et prospection présente.")
        boolean alerte) {
}
