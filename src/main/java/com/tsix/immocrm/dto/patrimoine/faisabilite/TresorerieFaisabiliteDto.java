package com.tsix.immocrm.dto.patrimoine.faisabilite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Onglets Analyse et Données : série mensuelle et lignes de trésorerie de l'étude.")
public record TresorerieFaisabiliteDto(
        @Schema(description = "Un point par mois, du premier au dernier mois portant une ligne.") List<MoisTresorerieDto> mois,
        @Schema(description = "Onglet « Données » : lignes par date.") List<LigneTresorerieDto> lignes) {
}
