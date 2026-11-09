package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Listes de choix des filtres de pièces d'achat ; projets et fournisseurs limités au périmètre.")
public record ReferentielsAchatDto(
        @Schema(description = "Natures, dans l'ordre d'affichage.") List<NatureAchatDto> natures,
        @Schema(description = "Statuts de pièce, par ordre (Sorting).") List<StatutAchatDto> statuts,
        @Schema(description = "Projets visibles de l'appelant (habilitations projet).") List<ProjetAchatDto> projets,
        @Schema(description = "Fournisseurs des pièces visibles, par nom.") List<FournisseurAchatDto> fournisseurs) {

    @Schema(description = "Nature de pièce (DocType).")
    public record NatureAchatDto(
            @Schema(description = "Code stocké (DocType).", example = "INVOICE") String code,
            @Schema(description = "Libellé.", example = "Facture") String libelle) {
    }

    @Schema(description = "Statut de pièce (BuyDocumentStatus).")
    public record StatutAchatDto(
            @Schema(description = "Id du statut.") UUID id,
            @Schema(description = "Code.") String code,
            @Schema(description = "Libellé.") String description,
            @Schema(description = "Drapeau « OK pour paiement » (IsOkForPayement).") boolean okPourPaiement,
            @Schema(description = "Drapeau « Validé comptabilité » (IsValidatedOnAccounting).") boolean valideComptabilite,
            @Schema(description = "Ordre d'affichage (Sorting) ; null si absent.") Integer ordre) {
    }

    @Schema(description = "Projet de chantier (PrjProject).")
    public record ProjetAchatDto(
            @Schema(description = "Id du projet.") UUID id,
            @Schema(description = "Code.") String code,
            @Schema(description = "Libellé.") String description) {
    }

    @Schema(description = "Fournisseur (CfgTier) d'au moins une pièce visible.")
    public record FournisseurAchatDto(
            @Schema(description = "Id du fournisseur.") UUID id,
            @Schema(description = "Nom (CfgTier.Description).") String nom) {
    }
}
