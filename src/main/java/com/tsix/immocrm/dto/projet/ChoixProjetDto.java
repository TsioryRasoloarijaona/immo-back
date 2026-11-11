package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix propres à un projet, par code.")
public record ChoixProjetDto(
        @Schema(description = "Tranches ventilables : celles de la société du projet (CfgTranche → CfgProject.CfgCompanyId).")
        List<ReferentielItemDto> tranches,
        @Schema(description = "Lots du projet (PrjMarket.PrjProjectId), choix du gestionnaire du compte prorata.")
        List<ReferentielItemDto> lots
) {
}
