package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rapport de la liste, visible par l'appelant.")
public record RapportResumeDto(
        @Schema(description = "Code du rapport (AdmReportData.Code), clé d'URL.", example = "ComTdbSalesMonitoringGlobal")
        String code,
        @Schema(description = "Libellé (AdmReportData.Description), sans espaces de bord.",
                example = "SUIVI GLOBAL DE VENTE*") String libelle,
        @Schema(description = "Ordre (AdmReportData.Sorting), 0 si absent.", example = "810") int ordre,
        @Schema(description = "Vrai si le rapport accepte au moins un paramètre.") boolean avecParametres) {
}
