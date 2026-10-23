package com.tsix.immocrm.dto.patrimoine.importation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat d'une ligne de données du classeur importé.")
public record LigneBilanDto(
        @Schema(description = "N° de ligne Excel (1 = en-têtes, 2 = première donnée).", example = "2") int numero,
        @Schema(description = "Pivot lu (code de l'objet dans la tranche) ; null s'il manque.") String pivot,
        @Schema(description = "Code écrit (colonne Code, à défaut le pivot) ; null s'il manque.") String code,
        @Schema(description = "Ce que la ligne a produit (ou produirait, en simulation).") ResultatImport resultat,
        @Schema(description = "Libellé OMRAN (« Création de l'article : X »…) suivi du détail des erreurs.") String message
) {

    @Schema(description = "INCHANGEE = rien ne change ; NON_VERIFIEE = import interrompu avant cette ligne.")
    public enum ResultatImport { CREATION, MISE_A_JOUR, INCHANGEE, ERREUR, NON_VERIFIEE }
}
