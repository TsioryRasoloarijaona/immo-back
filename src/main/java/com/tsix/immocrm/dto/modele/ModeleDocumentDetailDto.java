package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Modèle et contenu de sa version courante, pour l'éditeur.")
public record ModeleDocumentDetailDto(
        @Schema(description = "Identifiant du modèle (AppModeleDocument.PKey).")
        UUID id,
        @Schema(description = "Code de la catégorie.", example = "DOSSIER_VENTE")
        String categorie,
        @Schema(description = "Code du modèle, immuable.", example = "ACTE_RESERVATION")
        String code,
        @Schema(description = "Nom affiché.", example = "Acte de réservation")
        String libelle,
        @Schema(description = "Rang dans le menu « Imprimer » (croissant).", example = "10")
        int ordre,
        @Schema(description = "Faux = absent du menu « Imprimer », conservé.")
        boolean actif,
        @Schema(description = "Numéro de la version courante ; null si aucune.", example = "3")
        Integer versionCourante,
        @Schema(description = "HTML restreint de la version courante ; chaîne vide si aucune.")
        String contenu,
        @Schema(description = "Mise en page de la version courante, valeurs par défaut posées.")
        MiseEnPageDto miseEnPage,
        @Schema(description = "Date de la dernière modification.")
        LocalDateTime modifieLe,
        @Schema(description = "Auteur de la dernière modification.")
        String modifiePar) {
}
