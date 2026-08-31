package com.tsix.immocrm.dto.perimetre;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Corps de PUT /api/utilisateurs/{admUserId}/projets — remplacement intégral "
        + "du périmètre projet d'un utilisateur (le front envoie l'état final de ses cases à "
        + "cocher, pas un delta).")
public record AffecterProjetsRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Identifiants des projets (CfgProject.PKey) désormais affectés à ce "
                + "compte. **Obligatoire** : une liste explicitement vide `[]` retire toute "
                + "restriction (le compte voit tout — un périmètre vide signifie \"voit tout\", "
                + "jamais \"ne voit rien\"), mais un champ absent ou `null` est refusé en 400, "
                + "pour qu'un corps tronqué ne puisse pas lever une restriction en silence. Les "
                + "doublons sont ignorés. 400 également si un identifiant ne correspond à aucun "
                + "projet.")
        List<UUID> projetIds
) {
}
