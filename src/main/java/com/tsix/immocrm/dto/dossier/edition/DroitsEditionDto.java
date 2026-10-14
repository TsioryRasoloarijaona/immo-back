package com.tsix.immocrm.dto.dossier.edition;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Groupes de champs de la fiche que l'appelant peut modifier sur ce dossier.")
public record DroitsEditionDto(
        @ArraySchema(schema = @Schema(allowableValues = {"DATES", "ANNULATION", "CLIENT", "COMMERCIAL", "GESTIONNAIRES",
                "NOTAIRE", "CREDIT", "OPTIONS", "PENALITES", "PRIX", "NOTE"}),
                arraySchema = @Schema(description = "Groupes de champs que l'appelant peut modifier."))
        List<String> groupes,
        @Schema(description = "Vrai si le dossier est annulé ou désisté : seul le groupe ANNULATION reste modifiable.")
        boolean sorti
) {
}
