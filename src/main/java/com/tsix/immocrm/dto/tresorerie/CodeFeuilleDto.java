package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Code de feuille attribué au décaissement (P.P ou C.R.A.S).")
public record CodeFeuilleDto(
        @Schema(description = "Code existant, ou attribué par la séquence s'il était vide.", example = "26/00001")
        String code) {
}
