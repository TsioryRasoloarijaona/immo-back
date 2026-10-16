package com.tsix.immocrm.dto.dossier.reajustement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Dossiers cochés à réajuster.")
public record ValiderReajustementRequest(
        @Schema(description = "Identifiants des dossiers cochés, tous de la tranche et avec un écart. Requis, non vide.")
        List<UUID> dossierIds
) {
}
