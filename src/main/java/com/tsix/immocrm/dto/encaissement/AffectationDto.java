package com.tsix.immocrm.dto.encaissement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une ligne d'affectation d'un règlement — « ce règlement a payé cette échéance-là, pour ce
 * montant-là ».
 *
 * <p>Porte une {@code StlSettlementLine} et l'échéance ({@code ComDocument}) qu'elle solde, plus le
 * dossier auquel cette échéance appartient. C'est l'unité que l'écran légataire liste dans le
 * détail d'un règlement, sous le menu qu'il nomme lui-même « Affectations ».
 */
@Schema(description = "Imputation d'une part d'un règlement sur une échéance précise. Un règlement "
        + "en porte de une à trois sur les données réelles.")
public record AffectationDto(

        @Schema(description = "Identifiant de la ligne d'affectation (`StlSettlementLine`).")
        UUID id,

        @Schema(description = """
                Part du règlement imputée à **cette** échéance.

                **Ce n'est pas le montant du règlement**, et c'est la distinction que cette \
                ressource existe pour rendre visible : un règlement qui couvre trois échéances \
                porte trois lignes dont la somme fait son montant brut.""",
                example = "8400.000000")
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

        @Schema(description = "Montant total de l'échéance, toutes imputations confondues. À "
                + "comparer à `montantAffecte` pour savoir si cette échéance est soldée par ce "
                + "seul règlement.")
        BigDecimal montantEcheance,

        @Schema(description = "Identifiant de la catégorie de l'échéance (`ComSettlementCategory`).")
        UUID categorieId,

        @Schema(description = "Code de la catégorie de l'échéance (AVANCE, FRAIS SYNDIC…).",
                example = "AVANCE")
        String categorie,

        @Schema(description = "Dossier de vente portant l'échéance. **Peut différer d'une ligne à "
                + "l'autre** : un même règlement peut solder des échéances de plusieurs dossiers.")
        UUID dossierId,

        @Schema(description = "Code du dossier portant l'échéance.")
        String dossierCode
) {
}
