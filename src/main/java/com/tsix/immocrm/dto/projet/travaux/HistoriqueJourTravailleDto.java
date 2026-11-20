package com.tsix.immocrm.dto.projet.travaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Ligne StrHistory du jour travaillé (création et transitions), la plus récente en tête.")
public record HistoriqueJourTravailleDto(
        @Schema(description = "StrHistory.DocDate : instant de l'écriture.") LocalDateTime date,
        @Schema(description = "StrHistory.AdmUserDescription : auteur.") String utilisateur,
        @Schema(description = "« Création » ou « transition: départ=>arrivée(login & second) ».") String description,
        @Schema(description = "StrHistory.Notes : commentaire saisi.") String notes,
        @Schema(description = "StrHistory.UserDate.") LocalDateTime dateUtilisateur) {
}
