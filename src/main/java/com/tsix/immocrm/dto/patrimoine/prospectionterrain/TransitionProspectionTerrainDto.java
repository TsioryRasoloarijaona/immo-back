package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition sortante du statut courant (StrWorkFlow de l'entité LndLand).")
public record TransitionProspectionTerrainDto(
        @Schema(description = "StrWorkFlow.PKey, à renvoyer dans POST …/transition.") UUID id,
        String code,
        String description,
        @Schema(description = "Statut d'arrivée.") ReferentielItemDto statutCible,
        @Schema(description = "WithComment : commentaire obligatoire (400 sinon).") boolean commentaireObligatoire,
        @Schema(description = "WithUserDate : dateUtilisateur saisie devient la date de l'historique.") boolean avecDateUtilisateur,
        @Schema(description = "WithPassword : second utilisateur exigé, non pris en charge (409).") boolean secondUtilisateur
) {
}
