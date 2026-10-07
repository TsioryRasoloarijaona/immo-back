package com.tsix.immocrm.dto.encaissement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Une {@code StlSettlementLine} : l'échéance ({@code ComDocument}) qu'elle solde et son dossier. */
public record AffectationDto(

        @Schema(description = "Identifiant de la ligne d'affectation (`StlSettlementLine`).")
        UUID id,

        @Schema(example = "8400.000000")
        BigDecimal montantAffecte,

        @Schema(description = "Identifiant de l'échéance soldée (`ComDocument`).")
        UUID echeanceId,

        @Schema(description = "Code de l'échéance, tel qu'il apparaît dans l'échéancier du dossier.")
        String echeanceCode,

        @Schema(description = "Libellé de l'échéance (`ComDocument.Description`).",
                example = "2ème tranche")
        String echeanceDescription,

        @Schema(description = "Date de l'échéance — même champ que `dateEcheance` de l'échéancier "
                + "(`ComDocument.DocDate`).")
        LocalDateTime dateEcheance,

        BigDecimal montantEcheance,

        @Schema(description = "Identifiant de la catégorie de l'échéance (`ComSettlementCategory`).")
        UUID categorieId,

        @Schema(description = "Code de la catégorie de l'échéance (AVANCE, FRAIS SYNDIC…).",
                example = "AVANCE")
        String categorie,

        UUID dossierId,

        @Schema(description = "Code du dossier portant l'échéance.")
        String dossierCode
) {
}
