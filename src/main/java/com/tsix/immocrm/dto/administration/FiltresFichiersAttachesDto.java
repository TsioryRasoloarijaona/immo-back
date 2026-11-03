package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Valeurs proposées par les filtres de la liste des fichiers attachés.")
public record FiltresFichiersAttachesDto(
        @Schema(description = "Types de pièce (AdmAttachedFileType), triés par libellé.") List<TypePieceFiltre> types,
        @Schema(description = "Catégories normalisées, liste fixe dans l'ordre d'affichage.")
        List<ChoixFiltreFichier> categories,
        @Schema(description = "Groupes distincts non vides (FoderGroup sans espaces de bord), triés.")
        List<String> groupes,
        @Schema(description = "Choix « Non défini » (code NON_DEFINI à passer dans groupes) ; null si aucune ligne "
                + "n'a de groupe vide.") ChoixFiltreFichier groupeNonDefini) {

    @Schema(description = "Type de pièce proposé au filtre.")
    public record TypePieceFiltre(
            @Schema(description = "AdmAttachedFileType.PKey, à passer dans typeIds.") UUID id,
            @Schema(description = "AdmAttachedFileType.Description.") String libelle) {
    }

    @Schema(description = "Choix à code et libellé.")
    public record ChoixFiltreFichier(
            @Schema(description = "Code à renvoyer au filtre.", example = "DOSSIER") String code,
            @Schema(description = "Libellé affiché.", example = "Dossier de vente") String libelle) {
    }
}
