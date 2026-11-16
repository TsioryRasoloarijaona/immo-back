package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.achat.config.RoleTransitionDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Transition de statut de lot (StrWorkFlow d'un des 5 genres de lot), telle que l'éditeur la montre.")
public record TransitionStatutLotDto(
        @Schema(description = "StrWorkFlow.PKey.") UUID id,
        @Schema(description = "Genre de lot (StrEntity.PKey).") UUID genreId,
        @Schema(description = "Libellé du genre de lot.", example = "Lot Entrepreneurs") String genre,
        @Schema(description = "O.D.T (Sorting), null si absent.") Integer ordre,
        @Schema(description = "Code de la transition.") String code,
        @Schema(description = "Libellé de la transition.") String description,
        @Schema(description = "Statut de départ (StrStatus.PKey).") UUID statutDepartId,
        @Schema(description = "Code du statut de départ.") String statutDepartCode,
        @Schema(description = "Statut d'arrivée (StrStatus.PKey).") UUID statutArriveeId,
        @Schema(description = "Code du statut d'arrivée.") String statutArriveeCode,
        @Schema(description = "Commentaire obligatoire (WithComment = 1).") boolean avecCommentaire,
        @Schema(description = "Mot de passe demandé (WithPassword = 1).") boolean avecMotDePasse,
        @Schema(description = "Transition automatique (WithAutomaticTransition = 1).") boolean transitionAutomatique,
        @Schema(description = "Rôles autorisés (intervenants StrWorkFlowIntervener → StrIntervener.AdmRoleId), par code.")
        List<RoleTransitionDto> roles
) {
}
