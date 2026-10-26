package com.tsix.immocrm.dto.patrimoine.importation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Colonne cible d'un import avec mapping (entités, programmes, tranches, prospections terrain).")
public record ColonneImportDto(
        @Schema(description = "Champ du DTO d'écriture de l'écran ; null si la colonne est ignorée.", example = "formeJuridique")
        String propriete,
        @Schema(description = "Libellé OMRAN de la colonne (DisplayName), en-tête attendu dans le fichier.",
                example = "Forme juridique")
        String entete,
        @Schema(description = "Type de la valeur lue.", allowableValues = {"TEXTE", "ENTIER", "DECIMAL", "DATE", "BOOLEEN",
                "REFERENCE"})
        String type,
        @Schema(description = "Vrai pour la colonne de rapprochement (le code), qui doit être mappée.") boolean pivot,
        @Schema(description = "Vrai si la colonne est exigée à la création (règle du service de l'écran).") boolean obligatoire,
        @Schema(description = "Vrai : colonne OMRAN acceptée dans le fichier mais non gérée par l'écran (avertissement).")
        boolean ignoree,
        @Schema(description = "Référentiel d'une colonne REFERENCE (valeur lue par code ou libellé, sans casse) ; null sinon.",
                example = "PAYS")
        String reference
) {
}
