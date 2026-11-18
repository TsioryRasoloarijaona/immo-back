package com.tsix.immocrm.dto.achat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Suivi des avances : lignes de la période (une pièce peut en donner deux) et totaux hors annulées.")
public record SuiviAvancesDto(List<LigneSuiviAvanceDto> lignes, TotauxSuiviAvancesDto totaux) {

    @Schema(description = "Ligne du suivi des avances : colonnes de la pièce et trois montants.")
    public record LigneSuiviAvanceDto(
            @JsonUnwrapped PieceSuiviAchatDto piece,
            @Schema(description = "Avance : à payer (AmountToPay) d'une pièce de nature avance, 0 sinon.") BigDecimal avance,
            @Schema(description = "Ajustement : régularisation (AmountRegul) d'une pièce de nature avance, 0 sinon.")
            BigDecimal ajustement,
            @Schema(description = "Amortissement : avance + avance sur décompte retenues sur la pièce, négatif pour un avoir.")
            BigDecimal amortissement) {
    }

    @Schema(description = "Totaux du suivi des avances, pièces annulées exclues.")
    public record TotauxSuiviAvancesDto(
            @Schema(description = "Σ des avances.") BigDecimal avance,
            @Schema(description = "Σ des ajustements.") BigDecimal ajustement,
            @Schema(description = "Σ des amortissements.") BigDecimal amortissement) {
    }
}
