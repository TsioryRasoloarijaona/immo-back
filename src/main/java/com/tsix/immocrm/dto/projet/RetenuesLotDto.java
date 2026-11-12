package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onglet Retenues de la fiche lot.")
public record RetenuesLotDto(
        @Schema(description = "Compte prorata.") BlocRetenueDto compteProrata,
        @Schema(description = "Assurance décennale.") BlocRetenueDto assuranceDecennale,
        @Schema(description = "Assurance tous risques chantier.") BlocRetenueDto assuranceTrc,
        @Schema(description = "Colonne droite, côté décomptes.") ColonneRetenueDto decompte,
        @Schema(description = "Colonne droite, côté factures.") ColonneRetenueDto facture
) {
}
