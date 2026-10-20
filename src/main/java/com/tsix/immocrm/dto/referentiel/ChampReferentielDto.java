package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Champ d'un référentiel administrable ; son nom est la clé du corps JSON et de la ligne lue.")
public record ChampReferentielDto(
        @Schema(description = "Clé JSON du champ.", example = "code")
        String nom,
        @Schema(description = "Libellé à afficher.", example = "Code")
        String libelle,
        @Schema(description = "Type de saisie.", allowableValues = {"TEXTE", "ENTIER", "DECIMAL", "FRACTION", "BOOLEEN",
                "COULEUR", "REFERENCE", "OPERATEUR", "LIGNES"})
        String type,
        @Schema(description = "Vrai si le champ est requis à la création et ne peut pas être vidé.")
        boolean obligatoire,
        @Schema(description = "Vrai si le champ entre dans la clé d'unicité de la table (seul ou avec d'autres).")
        boolean unique,
        @Schema(description = "Longueur maximale d'un texte, null sinon.", example = "255")
        Integer longueurMax,
        @Schema(description = "REFERENCE : route GET de la liste de choix (ReferentielItemDto[]), null sinon.",
                example = "/api/referentiels/natures")
        String choix,
        @Schema(description = "OPERATEUR : valeurs permises, null sinon.")
        List<OptionDto> options,
        @ArraySchema(arraySchema = @Schema(description = "LIGNES : champs de chaque ligne enfant, null sinon."),
                schema = @Schema(implementation = ChampReferentielDto.class))
        List<ChampReferentielDto> champs
) {

    @Schema(description = "Valeur permise d'un champ OPERATEUR.")
    public record OptionDto(
            @Schema(description = "Valeur envoyée et lue.", example = "1") int valeur,
            @Schema(description = "Libellé à afficher.", example = ">=") String libelle) {
    }
}
