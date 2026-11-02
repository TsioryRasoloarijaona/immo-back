package com.tsix.immocrm.dto.administration;

import com.tsix.immocrm.domain.EtatSession;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ligne de la liste des sessions OMRAN.")
public record LigneSessionDto(
        @Schema(description = "Identifiant de la session (AdmWebSessions.PKey).") UUID id,
        @Schema(description = "Utilisateur, tel qu'enregistré par OMRAN (UserDesc).") String utilisateur,
        @Schema(description = "Date d'ouverture, ISO locale.", example = "2026-03-10T09:15:30") LocalDateTime dateOuverture,
        @Schema(description = "Date de fermeture, ISO locale ; null si jamais posée.") LocalDateTime dateFermeture,
        @Schema(description = "État : State 2 = OUVERTE, 8 = VERROUILLEE, toute autre valeur = FERMEE.") EtatSession etat,
        @Schema(description = "Adresse du poste, souvent NOMHOTE/10.0.0.x.") String adresseIp,
        @Schema(description = "Version du client OMRAN (SystemVersion).") String version) {
}
