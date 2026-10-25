package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Descripteur d'un rapport : de quoi construire son formulaire de filtres.")
public record DescripteurRapportDto(
        @Schema(description = "Code du rapport.", example = "ComTdbRepartitionClient") String code,
        @Schema(description = "Slug du module.", example = "vente") String module,
        @Schema(description = "Libellé du rapport.", example = "REPARTITION DES CLIENTS") String libelle,
        @Schema(description = "Groupe de la liste.", example = "Autres rapports") String groupe,
        @Schema(description = "Paramètres acceptés, dans l'ordre d'affichage.") List<ParametreRapportDto> parametres,
        @Schema(description = "Formats d'export acceptés par /export.", example = "[\"xlsx\",\"pdf\"]")
        List<String> formatsExport) {
}
