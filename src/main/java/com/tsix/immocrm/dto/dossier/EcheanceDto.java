package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shape for entries in {@code GET /api/dossiers/{id}/echeancier} (backed by {@code ComDocument}).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo).
 *
 * {@code montantRegle}/{@code reste}/{@code statutCalcule} are computed on read from the linked
 * {@code StlSettlement}s (via {@code StlSettlementLine.ComDocumentId}), never stored.
 * {@code statutCalcule}: PAYE (reste &lt;= 0), PARTIEL (partially réglé, reste &gt; 0),
 * EN_RETARD (nothing réglé and {@code dateEcheance} is past), A_VENIR (nothing réglé, future).
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
        String statutCalcule
) {
}
