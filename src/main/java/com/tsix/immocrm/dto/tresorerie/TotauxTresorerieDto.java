package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Totaux des listes de trésorerie, toujours hors règlements annulés (FEATURE-n3-recettes-depenses.md D5). */
public final class TotauxTresorerieDto {

    private TotauxTresorerieDto() {
    }

    @Schema(description = "Σ de toute la liste filtrée (pas seulement la page), hors règlements annulés.")
    public record TotauxDecaissementsDto(BigDecimal montant, BigDecimal montantAffecte, BigDecimal resteAAffecter) {
    }

    @Schema(description = "Σ de toute la liste filtrée, hors annulés ; montantReglement compte chaque règlement une fois.")
    public record TotauxAffectationsDto(
            BigDecimal montantAffecte,
            BigDecimal montantReglement,
            @Schema(description = "Σ montantAffecte des décaissements (IsCredit = 0), rouge à l'écran.") BigDecimal debit,
            @Schema(description = "Σ montantAffecte des encaissements (IsCredit = 1), vert à l'écran.") BigDecimal credit) {
    }

    @Schema(description = "Σ du net à payer des pièces, ou du montant des encaissements hors annulés.")
    public record TotauxRecettesDto(BigDecimal montant) {
    }
}
