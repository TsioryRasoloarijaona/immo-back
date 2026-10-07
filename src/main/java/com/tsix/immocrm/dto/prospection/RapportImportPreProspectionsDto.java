package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** Même forme pour l'aperçu et l'import : voir {@code instructions/FEATURE-creation-pre-prospection.md} §5. */
public record RapportImportPreProspectionsDto(
        @Schema(description = "`false` pour l'aperçu (rien n'est écrit), `true` après l'import.")
        boolean importe,
        @Schema(description = "Tranche à laquelle **toutes** les lignes ont été rattachées. Null "
                + "dans l'aperçu.")
        UUID trancheId,
        @Schema(description = "Code de cette tranche. Null dans l'aperçu.", example = "APF")
        String trancheCode,
        UUID trancheSuggereeId,
        @Schema(description = "Code de la tranche proposée.", example = "APF")
        String trancheSuggereeCode,
        @Schema(description = "Noms de formulaires distincts trouvés dans le fichier.")
        List<String> formulaires,
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
