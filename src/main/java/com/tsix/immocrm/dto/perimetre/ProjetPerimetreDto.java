package com.tsix.immocrm.dto.perimetre;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Un projet du périmètre commercial d'un utilisateur (CfgProject). Forme "
        + "volontairement minimale — pour la fiche complète d'un projet, voir GET /api/projets.")
public record ProjetPerimetreDto(

        @Schema(description = "Identifiant du projet (CfgProject.PKey) — la valeur à renvoyer dans "
                + "`projetIds` de PUT /api/utilisateurs/{admUserId}/projets.")
        UUID id,

        @Schema(description = "Code court du projet (CfgProject.Code), ex. \"ARS\", \"MBR\" — c'est "
                + "ce code que le bandeau \"Périmètre limité à : ARS, MBR\" affiche côté front.",
                example = "ARS")
        String code,

        @Schema(description = "Libellé du projet (CfgProject.Description).",
                example = "AYAD RESIDENCE BEL AIR")
        String libelle
) {
}
