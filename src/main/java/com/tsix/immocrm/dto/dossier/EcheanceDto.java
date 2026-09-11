package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

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
        String statutCalcule,

        @Schema(description = "Numéro de décompte rattaché à l'échéance "
                + "(`ComDocument.QuittanceCode`) — la colonne « Décompte » de l'écran légataire. "
                + "**16,5 %** des 921 échéances réelles en portent un.\n\n"
                + "`null` quand il n'y en a pas. En base la colonne n'est **jamais** `NULL` — elle "
                + "porte une chaîne vide, normalisée ici. Le logiciel de bureau affiche « N.A. » "
                + "dans ce cas ; c'est un choix d'affichage, à refaire côté client.",
                example = "25/00167")
        String decompte,

        @Schema(description = "Date de création de la ligne d'échéancier "
                + "(`ComDocument.UserCreateDateTime`) — colonne « Créé le » de l'écran légataire. "
                + "Renseignée sur **100 %** des échéances.\n\n"
                + "À ne pas confondre avec `dateEcheance`, qui est la date d'exigibilité : une "
                + "échéance créée le 10/12/2025 peut être exigible le 10/12/2026.")
        LocalDateTime creeLe
) {
}
