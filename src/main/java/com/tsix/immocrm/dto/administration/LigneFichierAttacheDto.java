package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste des fichiers attachés (AdmAttachedFile), en lecture seule.")
public record LigneFichierAttacheDto(
        @Schema(description = "AdmAttachedFile.PKey.") UUID id,
        @Schema(description = "Description (nom du fichier).") String description,
        @Schema(description = "Code de catégorie normalisée, parmi les codes de /filtres.", example = "DOSSIER")
        String categorie,
        @Schema(description = "Libellé de l'objet rattaché tel qu'OMRAN l'a écrit (CategoryDescription).")
        String categorieLibelle,
        @Schema(description = "PKey de l'objet rattaché (CategoryId), sans FK : peut ne rien désigner.") UUID objetId,
        @Schema(description = "Écran Arpent de l'objet : dossier, article ou reglement ; null = pas de lien.",
                example = "dossier") String objetCle,
        @Schema(description = "Libellé du type (AdmAttachedFileType.Description) ; null si aucun.") String type,
        @Schema(description = "Extension avec son point (Extention) ; null si vide.", example = ".pdf") String extension,
        @Schema(description = "Dossier de groupement (FoderGroup) ; null si vide.", example = "PLAN") String groupe,
        @Schema(description = "Vrai si un fichier stocké vivant existe (AppFileObject) : ouverture possible.")
        boolean avecFichier,
        @Schema(description = "Auteur de la création (UserCreate).") String creePar,
        @Schema(description = "Date de création (UserCreateDateTime).") LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification (UserUpdate) ; null si jamais modifié.")
        String modifiePar,
        @Schema(description = "Date de la dernière modification (UserUpdateDateTime) ; null si jamais modifié.")
        LocalDateTime modifieLe) {
}
