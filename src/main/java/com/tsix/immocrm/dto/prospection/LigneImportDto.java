package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.StatutLigneImport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Une ligne du fichier, telle que lue et classée par l'import CSV des leads. */
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
        @Schema(description = "Téléphone retenu, tel qu'écrit dans le fichier (`TempTel`) : "
                + "« Téléphone », à défaut « Numéro WhatsApp », à défaut le numéro secondaire.",
                example = "+212663434434")
        String telephone,
        @Schema(description = "Email (`TempEMail`).", example = "Imanebennis68@gmail.com")
        String email,
        @Schema(description = "Formulaire publicitaire d'origine (colonne « Formulaire »), recopié "
                + "dans les notes.", example = "APF FORM 02*2026 - GISK")
        String formulaire,
        @Schema(description = """
                Sort de la ligne :
                - `NOUVELLE` — aucun contact identique connu ; créée.
                - `DOUBLON` — même contact (nom, téléphone, email) qu'une prospection existante \
                mais **autre date** de soumission : créée quand même, et signalée (`doublonIds`).
                - `DEJA_IMPORTEE` — même contact **et même date** (à la minute) qu'une ligne \
                existante, en base ou plus haut dans le fichier : c'est le même lead réimporté ; \
                **ignorée**.
                - `ERREUR` — ligne inexploitable (`message`) ; ignorée.""")
        StatutLigneImport statut,
        @Schema(description = "Explication du statut, en clair — null pour `NOUVELLE`.")
        String message,
        @Schema(description = "Pré-prospection créée. **Toujours null dans l'aperçu**, et pour "
                + "`DEJA_IMPORTEE` / `ERREUR`.")
        UUID preProspectionId,
        @Schema(description = "Code de la pré-prospection créée (`AA/NNNNN`). Null comme "
                + "`preProspectionId`.", example = "26/05949")
        String code,
        @Schema(description = "Prospections existantes au même contact (statuts `DOUBLON` et "
                + "`DEJA_IMPORTEE`) — ouvrables par `GET /api/prospections/{id}`. Une ligne en "
                + "double **dans le fichier** n'y figure qu'après l'import réel.")
        List<UUID> doublonIds
) {
}
