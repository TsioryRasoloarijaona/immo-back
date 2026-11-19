package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Nœud de la définition de nivellement d'un projet (CfgTrancheLevelingDefinition).")
public record NoeudNivellementDto(
        @Schema(description = "Id du nœud (PKey).") UUID id,
        @Schema(description = "Nœud parent ; null pour une racine.") UUID parentId,
        @Schema(description = "Code affiché dans l'arbre (« BLOC 1 », « ETAGE 2 »…).") String code,
        @Schema(description = "Rang parmi les frères, à partir de 1 (Sorting).") Integer ordre) {
}
