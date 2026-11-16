package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Schema(description = "Transition de statut de lot (POST et PUT) ; un PUT remplace la transition entière, rôles compris.")
public record TransitionLotEcritureDto(
        @Schema(description = "Genre de lot (StrEntity.PKey, un des 5 genres), obligatoire, celui des deux statuts.")
        UUID genreId,
        @Schema(description = "O.D.T (Sorting) ; absent = 1.", example = "1") Integer ordre,
        @Schema(description = "Code, ≤ 255.") String code,
        @Schema(description = "Libellé, obligatoire, ≤ 255.", example = "Adjuger") String description,
        @Schema(description = "Statut de départ (StrStatus.PKey du genre), obligatoire.") UUID statutDepartId,
        @Schema(description = "Statut d'arrivée (StrStatus.PKey du genre), obligatoire, différent du départ.")
        UUID statutArriveeId,
        @Schema(description = "Commentaire obligatoire (WithComment) ; absent = false.") Boolean avecCommentaire,
        @Schema(description = "Mot de passe demandé (WithPassword) ; absent = false.") Boolean avecMotDePasse,
        @Schema(description = "Transition automatique (WithAutomaticTransition) ; absent = false.") Boolean transitionAutomatique,
        @Schema(description = "Rôles autorisés (AdmRole.PKey), remplacés en bloc ; vide ou absent = aucun.") List<UUID> roleIds
) {
    public TransitionLotEcritureDto {
        avecCommentaire = Boolean.TRUE.equals(avecCommentaire);
        avecMotDePasse = Boolean.TRUE.equals(avecMotDePasse);
        transitionAutomatique = Boolean.TRUE.equals(transitionAutomatique);
        roleIds = Objects.requireNonNullElse(roleIds, List.of());
    }
}
