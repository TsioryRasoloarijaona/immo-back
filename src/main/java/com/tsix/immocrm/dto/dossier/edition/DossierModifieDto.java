package com.tsix.immocrm.dto.dossier.edition;

import com.tsix.immocrm.dto.dossier.fiche.DossierFicheDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Résultat d'une modification de la fiche : la fiche relue, les champs changés et l'écart du schéma.")
public record DossierModifieDto(
        @Schema(description = "Fiche du dossier relue après l'enregistrement.")
        DossierFicheDto fiche,
        @Schema(description = "Noms des champs réellement changés ; vide si le corps ne changeait rien.")
        List<String> champsModifies,
        @Schema(description = "Prix après remise TTC − (avance + crédit + autofinancement), arrondi à 2 ; null si nul ou prix inchangé.")
        BigDecimal ecartSchema
) {
}
