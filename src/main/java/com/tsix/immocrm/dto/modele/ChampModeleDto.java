package com.tsix.immocrm.dto.modele;

import com.tsix.immocrm.domain.TypeChamp;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Champ insérable dans un modèle : l'éditeur pose `<span data-champ=\"cle\">libelle</span>`.")
public record ChampModeleDto(
        @Schema(description = "Clé stable du champ, ex. `client.nom`.", example = "client.nom")
        String cle,
        @Schema(description = "Libellé français affiché dans l'éditeur.", example = "Nom du client")
        String libelle,
        @Schema(description = "Type : décide du format à la génération et de la règle d'un bloc conditionnel.")
        TypeChamp type,
        @Schema(description = "Exemple de valeur formatée, pour l'aide de l'éditeur.", example = "Nor CHEMAOU")
        String exemple) {
}
