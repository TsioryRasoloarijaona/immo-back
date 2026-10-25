package com.tsix.immocrm.dto.patrimoine.relances;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Aperçu des premières relances d'une tranche : ce que le POST écrira, sans rien écrire.")
public record ApercuPremieresRelancesDto(
        @Schema(description = "Date de 1re relance de la tranche (CfgTranche.Revival1Date).", example = "2026-03-01")
        LocalDate dateRelance,
        @Schema(description = "1er délai de la tranche en jours (AgreementFirstDelay) ; null = 0 jour.", example = "60")
        Integer premierDelai,
        @Schema(description = "Nouvelle échéance = dateRelance + premierDelai jours.", example = "2026-04-30")
        LocalDate echeance,
        @Schema(description = "Nombre de dossiers qui recevront les dates de relance.", example = "12")
        int dossiers,
        @Schema(description = "Nombre d'alertes système qui seront créées.", example = "9")
        int alertes,
        @Schema(description = "Avertissement à afficher (1er délai absent), null sinon.")
        String avertissement,
        @Schema(description = "Dossiers retenus, triés par code.")
        List<LigneApercuRelanceDto> lignes) {
}
