package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Modification d'un modèle ; un champ absent reste inchangé.")
public record ModifierModeleDocumentRequest(
        @Schema(description = "Nouveau nom affiché.", example = "Acte de réservation")
        String libelle,
        @Schema(description = "Nouveau rang dans le menu « Imprimer ».", example = "10")
        Integer ordre,
        @Schema(description = "Faux = retiré du menu « Imprimer » (le modèle est conservé).")
        Boolean actif,
        @Schema(description = "Nouveau contenu : présent (avec ou sans `miseEnPage`) = nouvelle version.")
        String contenu,
        @Schema(description = "Nouvelle mise en page : présente (avec ou sans `contenu`) = nouvelle version.")
        MiseEnPageDto miseEnPage) {
}
