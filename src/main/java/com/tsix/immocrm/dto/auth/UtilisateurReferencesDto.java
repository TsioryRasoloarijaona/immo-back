package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix de la fiche utilisateur.")
public record UtilisateurReferencesDto(
        List<ReferentielItemDto> roles,

        @Schema(description = "AdmWebCultures : code seul.")
        List<ReferentielItemDto> cultures,

        @Schema(description = "Tiers de type Employé.")
        List<ReferentielItemDto> personnels,

        @Schema(description = "StlDeposit.")
        List<ReferentielItemDto> comptesDepot
) {
}
