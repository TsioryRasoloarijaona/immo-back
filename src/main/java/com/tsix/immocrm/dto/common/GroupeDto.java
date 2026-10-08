package com.tsix.immocrm.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record GroupeDto(

        @Schema(example = "CONCRETISATION")
        Object cle,

        @Schema(example = "CONCRETISATION")
        String libelle,

        @Schema(description = "Nombre de lignes du groupe — le « (1 item) » de l'écran légataire.",
                example = "12")
        long nombre,

        @Schema(example = "{\"prix\": 1350000.00, \"prix:AVG\": 675000.00}")
        Map<String, Object> agregats) {
}
