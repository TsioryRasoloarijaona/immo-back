package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Position d'un fournisseur dans une société sur les mouvements visibles ; calcul PositionFournisseur.")
public record PositionFournisseurDto(
        @Schema(description = "Fournisseur (CfgTier.id, type FOU) ; null sur le total.") UUID fournisseurId,
        @Schema(description = "Nom du fournisseur (CfgTier.Description) ; null sur le total.") String fournisseurNom,
        @Schema(description = "Société (CfgCompany.id) ; null sur le total.") UUID societeId,
        @Schema(description = "Code de la société (CfgCompany.Code) ; null sur le total.") String societeCode,
        BigDecimal soldeDebut,
        BigDecimal netAPayer,
        BigDecimal regle,
        BigDecimal soldeFin) {
}
