package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Résultat d'une distribution ou d'une réaffectation : toutes les lignes ont été écrites.")
public record ResultatLotDto(

        @Schema(description = "Nombre de lignes écrites.", example = "12")
        int nombre,

        @Schema(description = "Employé (CfgTier) destinataire.")
        UUID commercialId,

        @Schema(description = "Nom du destinataire.", example = "Maha JAKHA")
        String commercialNom,

        @Schema(description = "Une entrée par ligne écrite, avec son commercial précédent.")
        List<LigneLotDto> lignes
) {

    @Schema(description = "Une ligne du lot.")
    public record LigneLotDto(

            UUID id,

            @Schema(example = "26/05948")
            String code,

            @Schema(description = "Commercial avant l'opération. `null` s'il n'y en avait pas.", example = "Issam EL KAMRI")
            String ancienCommercialNom
    ) {
    }
}
