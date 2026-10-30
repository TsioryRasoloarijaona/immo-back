package com.tsix.immocrm.dto.referentiel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Référentiel administrable : de quoi générer sa grille et son dialogue de saisie.")
public record DescripteurReferentielDto(
        @Schema(description = "Clé de l'URL /api/referentiels-admin/{slug}.", example = "orientations")
        String slug,
        @Schema(description = "Libellé OMRAN du référentiel.", example = "Orientations")
        String libelle,
        @Schema(description = "Menu OMRAN où il se range.",
                allowableValues = {"ARTICLES", "PROSPECTIONS_TERRAINS", "AUTRES", "TACHES"})
        String famille,
        @Schema(description = "Vrai si chaque ligne porte une tranche : la liste accepte trancheId (et global).")
        boolean parTranche,
        @Schema(description = "Codes protégés (ni supprimés ni recodés), vide si aucun.", example = "[\"VENTE\", \"PROJET\"]")
        List<String> codesProteges,
        @Schema(description = "Champs, dans l'ordre d'affichage.")
        List<ChampReferentielDto> champs
) {
}
