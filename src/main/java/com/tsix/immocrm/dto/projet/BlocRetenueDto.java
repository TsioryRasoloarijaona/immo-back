package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Bloc compte prorata, assurance décennale ou assurance TRC de l'onglet Retenues.")
public record BlocRetenueDto(
        @Schema(description = "Taux en pourcentage 0–100.") BigDecimal taux,
        @Schema(description = "Quote-part saisie (montant d'assurance) ; null pour le compte prorata.") BigDecimal quotePart,
        @Schema(description = "Retenue sur l'Est. de base (quote-part si saisie, sinon taux × forfait TTC).") BigDecimal estBase,
        @Schema(description = "Retenue sur l'Est. confidentielle.") BigDecimal confidentielle,
        @Schema(description = "Retenue sur le marché ajusté.") BigDecimal marcheAjuste,
        @Schema(description = "Côté décomptes.") PartRetenueDto decompte,
        @Schema(description = "Côté factures.") PartRetenueDto facture
) {

    @Schema(description = "Retenue des avenants et retenue totale d'une colonne du bloc.")
    public record PartRetenueDto(
            @Schema(description = "Retenue des avenants.") BigDecimal avenant,
            @Schema(description = "Retenue des décomptes ou des factures.") BigDecimal total
    ) {
    }
}
