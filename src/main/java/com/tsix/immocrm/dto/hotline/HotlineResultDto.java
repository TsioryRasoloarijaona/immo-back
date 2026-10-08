package com.tsix.immocrm.dto.hotline;

import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Plusieurs tiers peuvent partager un numéro (un fixe de couple) ; {@link #appelsSansFiche()} exclut les
 * pré-prospections déjà rattachées à l'un des tiers de {@link #fiches()}.
 */
public record HotlineResultDto(

        @Schema(description = "Quel critère a produit ce résultat : `TELEPHONE` ou `NOM`.",
                example = "TELEPHONE", allowableValues = {"TELEPHONE", "NOM"})
        String critere,

        @Schema(description = "Numéro de téléphone tel que saisi par l'agent, non modifié. "
                + "`null` pour une recherche par nom.",
                example = "06 12 34 56 78")
        String telephoneRecherche,

        @Schema(example = "612345678")
        String telephoneNormalise,

        @Schema(description = "Terme recherché, tel que saisi. `null` pour une recherche par "
                + "téléphone.", example = "alami")
        String nomRecherche,

        boolean tronque,

        boolean connu,

        List<HotlineFicheDto> fiches,

        List<ProspectionListItemDto> appelsSansFiche
) {
}
