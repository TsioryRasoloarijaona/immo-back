package com.tsix.immocrm.dto.projet.modifications;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix de l'écran Modifications.")
public record ReferentielsModificationsDto(
        @Schema(description = "Tranches habilitées de l'appelant (AdmUserHabilitation.CfgTrancheId), par libellé.")
        List<ReferentielItemDto> tranches,
        @Schema(description = "Les 5 états, dans l'ordre d'OMRAN.") List<EtatModificationProjetDto> etats) {
}
