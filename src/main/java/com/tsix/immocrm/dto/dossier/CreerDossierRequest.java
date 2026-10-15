package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * {@code prix} = total principal + annexes ; avant remise si {@code remisePercent} est fourni, après sinon.
 * {@code modePaieId} et {@code cin} sont requis par décision métier, bien que leurs colonnes soient nullables ;
 * {@code cin} s'écrit sur le {@code CfgTier.IDNumber} du prospect, pas sur le dossier.
 */
public record CreerDossierRequest(
        UUID prospectionId,
        List<UUID> articleIds,
        @Schema(example = "1000000.00")
        BigDecimal prix,
        @Schema(example = "10")
        BigDecimal remisePercent,
        String notes,
        UUID modePaieId,
        String cin,
        @Schema(description = "Offre d'origine (StlQuote) : optionnelle, doit appartenir à la même prospection, sinon 400.")
        UUID offreId
) {
}
