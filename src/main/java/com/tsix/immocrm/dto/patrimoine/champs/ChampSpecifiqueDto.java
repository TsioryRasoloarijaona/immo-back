package com.tsix.immocrm.dto.patrimoine.champs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Champ spécifique (AdmSpecificField) d'un porteur, avec sa valeur pour l'objet lu.")
public record ChampSpecifiqueDto(
        @Schema(description = "Id de la définition (AdmSpecificField.PKey), à renvoyer en champId au PUT.")
        UUID champId,
        @Schema(description = "Groupe d'affichage (Category).", example = "Juridique")
        String categorie,
        @Schema(description = "Ordre du groupe (CategorySorting).", example = "1")
        Integer ordreCategorie,
        @Schema(description = "Libellé du champ (Caption).", example = "N° du titre foncier")
        String libelle,
        @Schema(description = "Ordre du champ dans son groupe (Sorting).", example = "2")
        Integer ordre,
        @Schema(description = "Type : String, Date, Decimal, Money, Int, Percentage, Bit ou Drop down.", example = "String")
        String typeDonnee,
        @Schema(description = "Valeur par défaut de la définition (DefaultValue), null si aucune.")
        String valeurDefaut,
        @Schema(description = "Vrai si le champ ne peut pas être enregistré vide (Required).")
        boolean obligatoire,
        @Schema(description = "Choix d'une liste déroulante (ComboValue découpée sur « ; ») ; vide pour les autres types.")
        List<String> choix,
        @Schema(description = "Valeur de l'objet (Value), sinon la valeur par défaut ; formats du PUT (ISO, point, fraction).")
        String valeur) {
}
