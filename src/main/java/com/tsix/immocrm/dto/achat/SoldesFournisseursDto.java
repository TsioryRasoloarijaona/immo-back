package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Solde des fournisseurs sur les projets visibles de l'appelant, par fournisseur et société.")
public record SoldesFournisseursDto(
        @Schema(description = "Une ligne par (fournisseur, société), triées par nom de fournisseur puis société.")
        List<SoldeFournisseurDto> lignes,
        @Schema(description = "Total des lignes ; fournisseurId, fournisseurNom et societeCode nuls.")
        SoldeFournisseurDto total) {

    @Schema(description = "Solde d'un fournisseur dans une société : factures et documents à régler visibles, avoirs exclus.")
    public record SoldeFournisseurDto(
            @Schema(description = "Fournisseur (CfgTier.id) ; null sur le total.") UUID fournisseurId,
            @Schema(description = "Nom du fournisseur (CfgTier.Description) ; null sur le total.") String fournisseurNom,
            @Schema(description = "Code de la société (CfgCompany.Code) ; null sur le total.") String societeCode,
            @Schema(description = "Nombre de pièces comptées.") long nombrePieces,
            @Schema(description = "Σ des montants à payer (négatifs compris).") BigDecimal montantAPayer,
            @Schema(description = "Σ des réglés (règlements annulés exclus).") BigDecimal montantRegle,
            @Schema(description = "Σ à payer − Σ réglé, signé.") BigDecimal resteARegler,
            @Schema(description = "Vrai si |reste| ≤ 0,05.") boolean solde,
            @Schema(description = "Nombre de pièces dont le reste dépasse 0,05.") long nombreARegler,
            @Schema(description = "Nombre de pièces « OK pour paiement » dont le reste dépasse 0,05.")
            long nombreValideesARegler,
            @Schema(description = "Σ des restes > 0,05 des pièces « OK pour paiement » : payable tout de suite.")
            BigDecimal resteValideARegler,
            @Schema(description = "Σ des montants à payer négatifs (récupérations d'avance), ≤ 0.")
            BigDecimal montantADeduire) {
    }
}
