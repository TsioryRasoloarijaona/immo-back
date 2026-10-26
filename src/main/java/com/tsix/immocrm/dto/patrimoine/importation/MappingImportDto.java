package com.tsix.immocrm.dto.patrimoine.importation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Mapping des colonnes du fichier vers les colonnes cibles, proposé par l'analyse puis renvoyé à l'import.")
public record MappingImportDto(
        @Schema(description = "Propriété pivot (rapprochement création / mise à jour) ; seule valeur admise : code.",
                example = "code")
        String pivot,
        @Schema(description = "Une ligne par colonne cible ; 100 colonnes mappées au plus, une cible mappée une fois.")
        List<ColonneMappeeDto> colonnes
) {

    @Schema(description = "Colonne cible et colonne du fichier qui l'alimente.")
    public record ColonneMappeeDto(
            @Schema(description = "Libellé de la colonne cible (ColonneImportDto.entete).", example = "Forme juridique")
            String entete,
            @Schema(description = "Rang (0 = premier) de la colonne du fichier dans AnalyseImportDto.entetes ; -1 = non mappée.",
                    example = "2")
            int indexColonne,
            @Schema(description = "Propriété cible (ColonneImportDto.propriete).", example = "formeJuridique")
            String propriete
    ) {
    }
}
