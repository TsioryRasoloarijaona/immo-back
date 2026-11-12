package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totaux de la liste des pièces d'achat, mêmes filtres, toutes pages.")
public record TotauxDocumentsAchatDto(
        @Schema(description = "Nombre de pièces retenues (= totalElements de la liste).") long nombre,
        @Schema(description = "Σ des montants TTC.") BigDecimal montantTTC,
        @Schema(description = "Σ des montants à payer.") BigDecimal montantAPayer,
        @Schema(description = "Σ des réglés (règlements annulés exclus).") BigDecimal montantRegle,
        @Schema(description = "Σ à payer − Σ réglé : reste signé de la sélection.") BigDecimal resteARegler,
        @Schema(description = "Nombre de pièces dont le reste dépasse 0,05.") long nombreARegler,
        @Schema(description = "Σ des restes supérieurs à 0,05 seulement : ce qui est vraiment dû.") BigDecimal resteDu,
        @Schema(description = "Σ des montants HT.") BigDecimal montantHT,
        @Schema(description = "Σ des montants de TVA.") BigDecimal montantTVA) {
}
