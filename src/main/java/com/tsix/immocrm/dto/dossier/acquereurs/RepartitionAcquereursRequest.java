package com.tsix.immocrm.dto.dossier.acquereurs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Répartition complète des acquéreurs : remplace les co-acquéreurs du dossier.")
public record RepartitionAcquereursRequest(
        @Schema(description = "Part du client principal, pourcentage > 0.", example = "50",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quotePartPrincipal,

        @Schema(description = "Co-acquéreurs ; absents de la liste = retirés. Liste vide = client seul (100 %).")
        List<CoAcquereur> coAcquereurs
) {
    @Schema(description = "Co-acquéreur et sa part.")
    public record CoAcquereur(
            @Schema(description = "Tiers client ou prospect (`CfgTier.PKey`), autre que le client principal.",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            UUID tiersId,
            @Schema(description = "Part, pourcentage 0–100.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quotePart
    ) {
    }
}
