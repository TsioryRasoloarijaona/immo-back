package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Tracé d'un article sur un plan, avec l'état de l'article.")
public record TraceDto(
        @Schema(description = "Article tracé (StkItem.PKey).") UUID articleId,
        @Schema(description = "Code de l'article.") String code,
        @Schema(description = "Attribut `d` du tracé SVG, dans le repère largeur × hauteur du plan.") String d,
        @Schema(description = "Code du statut du dossier occupant (StrStatus.Code) ; null si libre.") String statutCode,
        @Schema(description = "Libellé du statut du dossier occupant ; null si libre.") String statutLibelle,
        @Schema(description = "Couleur du statut (StrStatus.ColorName, nom de couleur CSS) ; null si libre ou non renseignée.")
        String couleur,
        @Schema(description = "Dossier occupant : dossier de l'article dont le statut est actif ; null si libre.") UUID dossierId,
        @Schema(description = "Nom du client du dossier occupant ; null si libre.") String client,
        @Schema(description = "Vrai si l'article est un sous-article (StkItem.IsUnderItem) : pas de prospection.")
        boolean estSousArticle,
        @Schema(description = "Vrai si aucun dossier actif n'occupe l'article.") boolean libre
) {
}
