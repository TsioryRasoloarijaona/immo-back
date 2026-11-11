package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Transition sortante du statut courant du projet (StrWorkFlow de l'entité PrjProject).")
public record TransitionProjetDto(
        @Schema(description = "StrWorkFlow.PKey, à renvoyer dans POST …/transition.") UUID id,
        @Schema(description = "Code de la transition.") String code,
        @Schema(description = "Libellé de la transition.") String description,
        @Schema(description = "Statut d'arrivée.") ReferentielItemDto statutCible,
        @Schema(description = "WithComment : commentaire obligatoire (400 sinon).") boolean commentaireObligatoire,
        @Schema(description = "WithUserDate : la dateUtilisateur saisie est enregistrée (UserDate).") boolean avecDateUtilisateur,
        @Schema(description = "WithPassword : second utilisateur exigé, non pris en charge (409).") boolean secondUtilisateur
) {
}
