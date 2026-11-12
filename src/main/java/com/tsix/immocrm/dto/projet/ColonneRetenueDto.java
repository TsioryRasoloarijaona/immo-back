package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Colonne droite de l'onglet Retenues, côté décomptes ou côté factures.")
public record ColonneRetenueDto(
        @Schema(description = "Retenues forfaitaires.") BigDecimal retenueForfaitaire,
        @Schema(description = "Pénalité compte prorata ; null côté décomptes.") BigDecimal penaliteCompteProrata,
        @Schema(description = "Pénalité sur retenue de garantie.") BigDecimal penaliteRg,
        @Schema(description = "Blocage de la retenue de garantie (restitution bloquée).") BigDecimal blocageRg,
        @Schema(description = "Pénalité d'avance (lot terminé seulement, sinon 0).") BigDecimal penaliteAvance,
        @Schema(description = "Total des retenues non remboursables (= retenues de la colonne de l'onglet Suivi).")
        BigDecimal retenues
) {
}
