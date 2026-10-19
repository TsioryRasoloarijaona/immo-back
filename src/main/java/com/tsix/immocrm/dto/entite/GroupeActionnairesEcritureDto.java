package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Création ou modification d'un groupe ; en PATCH, absent ou null = inchangé (lignes comprises).")
public record GroupeActionnairesEcritureDto(
        @Schema(description = "Code, unique (409). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Lignes ; présentes = remplacent les anciennes. Σ ≠ 100 % ou actionnaire en double → 400.")
        List<LigneGroupeDto> lignes
) {
}
