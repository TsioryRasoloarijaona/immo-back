package com.tsix.immocrm.dto.parcours;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "« Ma journée » d'un commercial : quatre blocs calculés au jour de Casablanca.")
public record JourneeDto(
        @Schema(description = "Jour servi (aujourd'hui à Casablanca).")
        LocalDate date,
        @Schema(description = "Commercial dont les lignes sont servies (CfgTier) ; null si le compte n'a pas d'employé.")
        UUID commercialId,
        @Schema(description = "Prénom du commercial, sinon son nom.", example = "Khadija")
        String prenom,
        BlocLeadsDto nouveauxLeads,
        BlocRelancesDto relancesDuJour,
        BlocRelancesDto visitesDuJour,
        BlocRelancesDto enRetard
) {
}
