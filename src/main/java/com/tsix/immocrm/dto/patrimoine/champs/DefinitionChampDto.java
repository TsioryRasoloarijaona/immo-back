package com.tsix.immocrm.dto.patrimoine.champs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Définition d'un champ spécifique (création ou remplacement complet).")
public record DefinitionChampDto(
        @Schema(description = "Groupe d'affichage (Category).", example = "Juridique")
        String categorie,
        @Schema(description = "Ordre du groupe (CategorySorting) ; absent = 0.", example = "1")
        Integer ordreCategorie,
        @Schema(description = "Libellé du champ (Caption et Name), obligatoire.", example = "N° du titre foncier")
        String libelle,
        @Schema(description = "Ordre du champ dans son groupe (Sorting) ; absent = 0.", example = "2")
        Integer ordre,
        @Schema(description = "Code d'un type de GET /api/champs-specifiques/types, obligatoire.", example = "String")
        String typeDonnee,
        @Schema(description = "Valeur par défaut (DefaultValue), null si aucune.")
        String valeurDefaut,
        @Schema(description = "Vrai si le champ ne peut pas être enregistré vide ; absent = faux.")
        Boolean obligatoire,
        @Schema(description = "Choix d'une liste déroulante (au moins un, sans « ; ») ; ignorés pour les autres types.")
        List<String> choix) {
}
