package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Une étape de l'entonnoir : leads de la cohorte ayant atteint au moins cette étape.")
public record EtapeEntonnoirDto(

        @Schema(description = "Code stable : ARRIVES, PRIS_EN_CHARGE, QUALIFIES, VISITES, OFFRES, DOSSIERS.",
                example = "QUALIFIES")
        String code,

        @Schema(description = "Libellé affiché.", example = "Qualifiés")
        String libelle,

        @Schema(description = "Nombre de leads de la cohorte arrivés au moins jusqu'ici.", example = "193")
        long nombre,

        @Schema(description = "`nombre` / celui de l'étape précédente, entre 0 et 1. `null` pour la première étape "
                + "ou si la précédente vaut 0.", example = "0.120")
        BigDecimal tauxPassage
) {
}
