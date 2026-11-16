package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Listes de choix de la fiche statut de lot pour un genre : types, couleurs, cases et champs obligatoires.")
public record CatalogueStatutLotDto(
        @Schema(description = "Types de statut (StrStatusType), par ordre.") List<ChoixStatutLotDto> types,
        @Schema(description = "Noms de couleur .NET acceptés, graphie canonique.") List<String> couleurs,
        @Schema(description = "Cases du genre : seules celles qui ont un libellé (StrEntity.BoolFieldNNNLabel).")
        List<CaseStatutLotDto> cases,
        @Schema(description = "Champs du genre (StrField) qu'un statut peut rendre obligatoires, par ordre.")
        List<ChoixStatutLotDto> champs
) {

    @Schema(description = "Élément d'une liste de choix (type ou champ).")
    public record ChoixStatutLotDto(
            @Schema(description = "PKey de la ligne.") UUID id,
            @Schema(description = "Code.") String code,
            @Schema(description = "Libellé (Description, à défaut Code).") String libelle) {
    }

    @Schema(description = "Case à cocher d'un statut de lot.")
    public record CaseStatutLotDto(
            @Schema(description = "Numéro de la case (1-20 = BoolField001…020).", example = "1") int numero,
            @Schema(description = "Libellé du genre.", example = "Est un marché effectif") String libelle) {
    }
}
