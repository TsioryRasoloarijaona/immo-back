package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.projet.BordereauDto.TotauxBordereauDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bordereau estimatif d'un lot (PrjMarketLineEstimation hors stock).")
public record BordereauEstimatifDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Vrai si l'appelant ne peut pas enregistrer l'estimatif (droit, statut du lot, multi-fournisseurs).")
        boolean verrouille,
        @Schema(description = "Groupes du bordereau estimatif.") List<GroupeBordereauDto> groupes,
        @Schema(description = "Lignes, tri par ordre puis code.") List<LigneBordereauDto> lignes,
        @Schema(description = "Totaux des lignes.") TotauxBordereauDto totaux,
        @Schema(description = "TVA par défaut du lot en pourcentage (DefaultVat × 100), null si vide.", example = "20")
        BigDecimal tvaParDefaut,
        @Schema(description = "Catégorie de dépense du lot (StlCategoryId), défaut d'une nouvelle ligne.") UUID categorieParDefautId
) {
}
