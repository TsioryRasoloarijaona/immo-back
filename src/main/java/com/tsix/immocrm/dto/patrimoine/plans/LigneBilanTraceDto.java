package com.tsix.immocrm.dto.patrimoine.plans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Résultat de l'import d'un `<path>` (un tracé d'article).")
public record LigneBilanTraceDto(
        @Schema(description = "Attribut `id` du `<path>` tel que lu dans le fichier.") String code,
        @Schema(description = "Article retrouvé dans la tranche ; null si introuvable.") UUID articleId,
        @Schema(description = "AJOUTE, MODIFIE, INCHANGE ou INTROUVABLE (ce dernier n'empêche pas les autres tracés).")
        Resultat resultat,
        @Schema(description = "Message OMRAN de la ligne (« l'article : X est ajouté », « … n'a pas été modifié »…).")
        String message
) {
    public enum Resultat { AJOUTE, MODIFIE, INCHANGE, INTROUVABLE }
}
