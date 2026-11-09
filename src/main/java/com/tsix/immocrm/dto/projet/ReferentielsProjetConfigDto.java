package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix du formulaire projet, par code.")
public record ReferentielsProjetConfigDto(
        @Schema(description = "Sociétés (CfgCompany), actives ou non.") List<ReferentielItemDto> societes,
        @Schema(description = "Statuts de projet (lecture : le statut n'est pas saisi).") List<ReferentielItemDto> statutsProjet,
        @Schema(description = "Personnel (CfgTier de type EMP), choix du gestionnaire ; description = nom.")
        List<ReferentielItemDto> gestionnaires
) {
}
