package com.tsix.immocrm.dto.commission;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Tranche de l'écran Commissions : ventes et soldes de commission.")
public record CommissionTrancheDto(
        UUID trancheId,
        @Schema(description = "Code de la tranche.") String code,
        @Schema(description = "Libellé de la tranche.") String description,
        @Schema(description = "Vendu nombre : articles racines vendus (dossier ni annulé ni désisté).") int vendus,
        @Schema(description = "Reste nombre : articles racines libres.") int restants,
        @Schema(description = "Somme des lignes de commission de la tranche, désistements (négatifs) compris.")
        BigDecimal montantGenere,
        @Schema(description = "Part réglée des documents à payer, au prorata des lignes (règlements annulés exclus).")
        BigDecimal montantRegle,
        @Schema(description = "montantGenere − montantRegle.") BigDecimal montantReste,
        @Schema(description = "C.A. Stock : Σ prix de vente des articles VENTE de la tranche.") BigDecimal caStock,
        @Schema(description = "C.A. Réalisé : Σ prix des dossiers ni annulés ni désistés.") BigDecimal caRealise,
        @Schema(description = "C.A. Reste : caStock − caRealise.") BigDecimal caReste) {
}
