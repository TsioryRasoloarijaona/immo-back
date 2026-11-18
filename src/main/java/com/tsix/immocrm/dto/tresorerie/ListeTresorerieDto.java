package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page d'une liste de trésorerie, avec le nombre total de lignes et les totaux de toute la liste.")
public record ListeTresorerieDto<L, T>(
        @Schema(description = "Lignes de la page demandée ; [] si rien n'est visible.") List<L> lignes,
        @Schema(description = "Nombre de lignes de toute la liste filtrée.") long total,
        T totaux) {
}
