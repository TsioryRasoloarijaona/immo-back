package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Colonne Décomptes, Factures ou Avoirs de l'onglet Suivi ; null = case absente de la colonne.")
public record ColonneSuiviDto(
        @Schema(description = "Montant HT.") BigDecimal montantHT,
        @Schema(description = "TVA.") BigDecimal tva,
        @Schema(description = "Montant TTC.") BigDecimal montantTTC,
        @Schema(description = "Approvisionnements TTC ; null pour les avoirs.") BigDecimal approTTC,
        @Schema(description = "Dépassements TTC ; null pour les avoirs.") BigDecimal depassementsTTC,
        @Schema(description = "Travaux supplémentaires TTC ; null pour les avoirs.") BigDecimal travauxSuppTTC,
        @Schema(description = "Fluctuations TTC ; null pour les avoirs.") BigDecimal fluctuationsTTC,
        @Schema(description = "Total TTC = montant + appro + travaux supp. + fluctuations ; null pour les avoirs.")
        BigDecimal totalTTC,
        @Schema(description = "Avenants TTC ; null pour les avoirs.") BigDecimal avenantsTTC,
        @Schema(description = "Coût TTC = total + avenants ; null pour les avoirs.") BigDecimal coutTTC,
        @Schema(description = "Retenues non remboursables ; null pour les avoirs.") BigDecimal retenues,
        @Schema(description = "Remboursement du compte prorata ; null pour les avoirs.") BigDecimal remboursement,
        @Schema(description = "Régularisations et timbres ; null pour les décomptes.") BigDecimal regulation
) {
}
