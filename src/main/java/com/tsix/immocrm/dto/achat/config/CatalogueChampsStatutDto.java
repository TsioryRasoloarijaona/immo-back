package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Champs cochables comme obligatoires d'un statut d'achat : propriétés [IsWorkFlow] d'OMRAN.")
public record CatalogueChampsStatutDto(
        @Schema(description = "Champs de la pièce (onglet « Champs obligatoires »).") List<ChampStatutAchatDto> piece,
        @Schema(description = "Champs du fournisseur (onglet « Champs obligatoires Fournisseur »).")
        List<ChampStatutAchatDto> fournisseur
) {
    @Schema(description = "Champ cochable d'un statut d'achat.")
    public record ChampStatutAchatDto(
            @Schema(description = "Nom OMRAN stocké dans MandatoryFields / SupplierMandatoryFields.", example = "DocRef") String nom,
            @Schema(description = "Libellé OMRAN (DisplayName).", example = "Réf. Interne") String libelle) {
    }
}
