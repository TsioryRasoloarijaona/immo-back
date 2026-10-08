package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.domain.PorteeCleApi;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Voir instructions/FEATURE-cle-api-n8n.md §5. */
@Schema(description = "Création d'une clé d'API pour un client machine (n8n, agent).")
public record CreerCleApiRequest(
        @Schema(example = "n8n", requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                example = "[\"PRE_PROSPECTIONS_ECRIRE\", \"REFERENTIELS_LIRE\"]")
        List<PorteeCleApi> portees,
        @Schema(example = "n8n")
        String compteCode
) {
}
