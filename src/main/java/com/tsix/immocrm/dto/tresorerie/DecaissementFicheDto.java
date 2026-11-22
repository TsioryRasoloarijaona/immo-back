package com.tsix.immocrm.dto.tresorerie;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fiche en lecture d'un décaissement : en-tête et pièces affectées visibles.")
public record DecaissementFicheDto(
        @Schema(description = "En-tête, mêmes champs que la ligne de liste ; montants sur toutes les lignes.")
        DecaissementLigneDto decaissement,
        @Schema(description = "Notes du règlement.") String notes,
        @Schema(description = "Code reçu (ReceiptCode).") String codeRecu,
        @Schema(description = "Code de la devise.", example = "MAD") String devise,
        @Schema(description = "Lignes d'affectation dont la pièce est visible, tri par code de pièce.")
        List<LigneDecaissementDto> lignes,
        @Schema(description = "Nombre de lignes d'affectation masquées (pièce d'un projet ou d'une société non habilités).")
        int lignesMasquees,
        @Schema(description = "Corps qu'un PUT renverrait tel quel : lignes visibles seulement, module nul s'il est inconnu.")
        SaisieDecaissementDto saisie) {

    @Schema(description = "Une affectation du décaissement à une pièce.")
    public record LigneDecaissementDto(
            @Schema(description = "StlSettlementLine.PKey.") UUID id,
            @Schema(description = "Pièce affectée (BuyDocument ou StlDocument).") PieceTresorerieDto piece,
            @Schema(description = "Nom du fournisseur / tiers de la pièce.") String fournisseur,
            UUID projetId,
            @Schema(description = "Code du projet de la pièce.") String projet,
            UUID lotId,
            @Schema(description = "Code du lot de marché de la pièce.") String lot,
            @Schema(description = "Montant affecté (StlSettlementLine.Amount).") BigDecimal montantAffecte) {
    }
}
