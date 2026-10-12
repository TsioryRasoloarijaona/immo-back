package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création (POST) ou modification partielle (PATCH) d'un groupe d'actionnaires.")
public record GroupeActionnairesEcritureDto(
        @Schema(description = "Code, unique sur toute la table. Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé. Obligatoire à la création.")
        String description,
        @Schema(description = "Lignes ; présentes = remplacent toutes les lignes (Σ = 100 %), absentes au PATCH = inchangées.")
        List<Ligne> lignes
) {

    @Schema(name = "LigneGroupeActionnairesEcriture", description = "Part d'un actionnaire dans le groupe.")
    public record Ligne(
            @Schema(description = "Actionnaire (CfgShareholder.PKey), une seule fois par groupe. Obligatoire.")
            UUID actionnaireId,
            @Schema(description = "Pourcentage en fraction, entre 0 et 1 : 0.4 = 40 %. Obligatoire.", example = "0.4")
            BigDecimal pourcentage
    ) {
    }
}
