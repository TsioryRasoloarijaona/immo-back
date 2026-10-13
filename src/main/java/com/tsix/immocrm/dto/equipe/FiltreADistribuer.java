package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Filtre de la carte « À distribuer » : mêmes sens que `GET /api/equipe/a-distribuer/lignes`.")
public record FiltreADistribuer(

        @Schema(description = "Premier jour pris en compte, inclus. Défaut : il y a 30 jours.", example = "2026-09-12")
        LocalDate du,

        @Schema(description = "Dernier jour pris en compte, inclus. Défaut : aujourd'hui.", example = "2026-10-11")
        LocalDate au,

        @Schema(description = "Projet à isoler ; doit être dans le périmètre de l'appelant (sinon 403).")
        UUID projet,

        @Schema(description = "`true` : aussi les pré-prospections déjà traitées ; `false` ou absent : jamais traitées seulement.")
        Boolean toutes
) {
}
