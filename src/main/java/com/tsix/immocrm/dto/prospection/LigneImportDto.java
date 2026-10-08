package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.StatutLigneImport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Une ligne du fichier importé : les informations extraites et son sort.")
public record LigneImportDto(
        @Schema(description = "Numéro de ligne dans le fichier, l'en-tête étant la ligne 1 — celui "
                + "qu'affiche un tableur.", example = "2")
        int ligne,
        @Schema(description = "Date de soumission du lead (colonne « Créé »), devenue la date du "
                + "document (`DocDate`). Null si illisible.", example = "2026-09-23T01:45:00")
        LocalDateTime dateCreation,
        @Schema(description = "Nom du contact (`TempDescription`).", example = "Imane Bennis")
        String nom,
        @Schema(example = "+212663434434")
        String telephone,
        @Schema(description = "Email (`TempEMail`).", example = "Imanebennis68@gmail.com")
        String email,
        @Schema(description = "Formulaire publicitaire d'origine (colonne « Formulaire »), recopié "
                + "dans les notes.", example = "APF FORM 02*2026 - GISK")
        String formulaire,
        StatutLigneImport statut,
        @Schema(description = "Explication du statut, en clair — null pour une `NOUVELLE` importée (ou en aperçu).")
        String message,
        @Schema(description = "Pré-prospection créée. Null dans l'aperçu, hors sélection (`statuts`), et pour "
                + "`DEJA_IMPORTEE` / `ERREUR`.")
        UUID preProspectionId,
        @Schema(description = "Code de la pré-prospection créée (`AA/NNNNN`). Null comme "
                + "`preProspectionId`.", example = "26/05949")
        String code,
        List<UUID> doublonIds
) {
}
