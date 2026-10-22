package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Groupe de rapports de la liste (AdmReportData.GroupName).")
public record GroupeRapportsDto(
        @Schema(description = "Nom du groupe, sans espaces de bord ; « Autres rapports » si vide ou absent.",
                example = "Autres rapports") String nom,
        @Schema(description = "Plus petit ordre (Sorting) des rapports du groupe : les groupes sont triés dessus.",
                example = "810") int ordre,
        @Schema(description = "Rapports visibles du groupe, triés par ordre puis libellé.")
        List<RapportResumeDto> rapports) {
}
