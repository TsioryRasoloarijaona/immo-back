package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Réponse de l'aperçu et de l'import CSV des leads — même forme dans les deux cas, pour que
 * l'écran affiche le même tableau avant et après. Voir
 * {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 */
@Schema(description = "Rapport d'import de leads : ce qui a été lu, et ce qui est (aperçu) ou a "
        + "été (import) fait de chaque ligne. Même forme pour l'aperçu et l'import.")
public record RapportImportPreProspectionsDto(
        @Schema(description = "`false` pour l'aperçu (rien n'est écrit), `true` après l'import.")
        boolean importe,
        @Schema(description = "Tranche à laquelle **toutes** les lignes ont été rattachées. Null "
                + "dans l'aperçu.")
        UUID trancheId,
        @Schema(description = "Code de cette tranche. Null dans l'aperçu.", example = "APF")
        String trancheCode,
        @Schema(description = "Tranche **proposée** pour pré-sélectionner la liste déroulante : "
                + "celle dont le code est le premier mot du nom de formulaire (`APF FORM 02*2026` → "
                + "`APF`), si **tous** les formulaires du fichier désignent la même. Null sinon — "
                + "l'utilisateur choisit. Ce n'est qu'une proposition : c'est `trancheId` envoyé à "
                + "l'import qui décide.")
        UUID trancheSuggereeId,
        @Schema(description = "Code de la tranche proposée.", example = "APF")
        String trancheSuggereeCode,
        @Schema(description = "Noms de formulaires distincts trouvés dans le fichier.")
        List<String> formulaires,
        @Schema(description = "Mises en garde à montrer avant de confirmer — p.ex. un fichier dont "
                + "les formulaires désignent plusieurs tranches, alors qu'un import n'en accepte "
                + "qu'une.")
        List<String> avertissements,
        @Schema(description = "Nombre de lignes de données (en-tête et lignes vides exclues).")
        int totalLignes,
        @Schema(description = "Lignes `NOUVELLE`.")
        int nouvelles,
        @Schema(description = "Lignes `DOUBLON` — créées elles aussi.")
        int doublons,
        @Schema(description = "Lignes `DEJA_IMPORTEE` — ignorées.")
        int dejaImportees,
        @Schema(description = "Lignes `ERREUR` — ignorées.")
        int erreurs,
        @Schema(description = "Le détail, ligne par ligne, dans l'ordre du fichier.")
        List<LigneImportDto> lignes
) {
}
