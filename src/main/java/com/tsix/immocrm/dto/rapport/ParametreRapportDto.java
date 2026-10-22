package com.tsix.immocrm.dto.rapport;

import com.tsix.immocrm.domain.rapport.TypeParametre;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paramètre d'un rapport, à envoyer en paramètre de requête de /resultat et /export.")
public record ParametreRapportDto(
        @Schema(description = "Nom du paramètre de requête.", example = "trancheId") String nom,
        @Schema(description = "Libellé du champ.", example = "Tranches") String libelle,
        @Schema(description = "Type : fixe le champ du formulaire et le contrôle de la valeur.") TypeParametre type,
        @Schema(description = "Vrai si une valeur est exigée (400 sinon) et qu'aucun défaut n'existe.")
        boolean obligatoire,
        @Schema(description = "Vrai si le paramètre se répète (trancheId=…&trancheId=…).") boolean multiple,
        @Schema(description = "Défaut appliqué si absent (date aaaa-MM-jj), null si aucun.", example = "2026-01-01")
        String valeurParDefaut,
        @Schema(description = "Valeurs proposées : CHOIX, TRANCHES et COMMERCIAUX (ces deux-là remplies par le serveur).")
        List<OptionRapportDto> options,
        @Schema(description = "ENTIER : borne basse incluse, null si aucune.") Integer min,
        @Schema(description = "ENTIER : borne haute incluse, null si aucune.") Integer max) {
}
