package com.tsix.immocrm.dto.patrimoine.impressions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Aperçu de la Relance 2 par statut d'une tranche (lecture seule).")
public record ApercuRelance2Dto(
        @Schema(description = "Id de la tranche (CfgTranche.PKey).")
        UUID trancheId,
        @Schema(description = "Code de la tranche.", example = "APF")
        String trancheCode,
        @Schema(description = "Date du jour (date de la lettre quand Revival2 est absente).")
        LocalDate dateImpression,
        @Schema(description = "Codes de statut retenus (les 5 par défaut, ou le filtre `statut`).")
        List<String> statutsRetenus,
        @Schema(description = "Dossiers retenus, écartés compris.", example = "80")
        int dossiers,
        @Schema(description = "Lettres imprimées : une par acquéreur des dossiers non écartés.", example = "90")
        int lettres,
        @Schema(description = "Dossiers retenus sans lettre : reste du prix ≤ 0.")
        List<EcarteRelance2Dto> ecartes,
        @Schema(description = "Vrai si un modèle RELANCE_2 actif existe ; sinon le PDF utilise le texte livré.")
        boolean modeleActif,
        @Schema(description = "Compteurs par statut présent, dans l'ordre de la liste des 5 statuts.")
        List<GroupeStatutRelance2Dto> parStatut,
        @Schema(description = "Une ligne par lettre, dans l'ordre des pages du PDF.")
        List<LigneRelance2Dto> lignes) {
}
