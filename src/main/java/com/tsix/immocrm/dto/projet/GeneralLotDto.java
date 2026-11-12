package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onglet Général de la fiche lot : montants du marché, estimations et coûts (lecture seule).")
public record GeneralLotDto(
        @Schema(description = "Marché : devis retenu le plus récent (bons de commande validés si multi-fournisseur).")
        TrioDto marche,
        @Schema(description = "Est. de base : forfait saisi (EstimatedPrice*Manual).") TrioDto estimationBase,
        @Schema(description = "Est. confidentielle : estimation calculée (= estimationHT/TVA/TTC de la fiche).")
        TrioDto estimationConfidentielle,
        @Schema(description = "Modification BDPF : marché ajusté − marché.") TrioDto modificationBdpf,
        @Schema(description = "Marché ajusté : Σ des lignes du bordereau (bons de commande validés si multi-fournisseur).")
        TrioDto marcheAjuste,
        @Schema(description = "Coût prévisionnel (net, réglé, reste et total = ceux de la liste des lots).")
        ColonneCoutDto coutPrevisionnel,
        @Schema(description = "Coût réel : factures − avoirs.") ColonneCoutDto coutReel
) {
}
