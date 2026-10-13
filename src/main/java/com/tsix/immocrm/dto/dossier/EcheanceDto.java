package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code montantRegle}/{@code reste}/{@code statutCalcule} : calculés à la lecture depuis les
 * {@code StlSettlementLine}, jamais stockés ; un décaissement compte en négatif. {@code statutCalcule} :
 * PAYE (reste &lt;= 0), EN_RETARD (reste &gt; 0, échéance aujourd'hui ou passée), PARTIEL (entamée, à venir),
 * A_VENIR (rien réglé, à venir).
 */
public record EcheanceDto(
        UUID id,
        String code,
        LocalDateTime dateEcheance,
        BigDecimal montant,
        BigDecimal montantHT,
        BigDecimal montantTTC,
        BigDecimal tauxTVA,
        String description,
        UUID categorieId,
        String categorie,
        Boolean exclureDuPrix,
        String notes,
        BigDecimal montantRegle,
        BigDecimal reste,
        String statutCalcule,

        @Schema(example = "25/00167")
        String decompte,

        LocalDateTime creeLe
) {
}
