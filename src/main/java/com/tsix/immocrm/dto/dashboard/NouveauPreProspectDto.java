package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Pré-prospection = {@code StrEntity.Code = 'ComPreProspection'}, voir {@code DiscriminantPreProspection}. */
public record NouveauPreProspectDto(

        @Schema(description = "Pré-prospection créée. La route de détail est celle des "
                + "prospections : `GET /api/prospections/{id}`.")
        UUID prospectionId,

        @Schema(description = "Code légataire de la pré-prospection, s'il a été attribué.")
        String code,

        @Schema(description = "Nom ou raison sociale saisis au premier contact "
                + "(`TempDescription`). `null` tant que le lead est anonyme.")
        String nom,

        String telephone,

        @Schema(description = "E-mail saisi au premier contact (`TempEMail`).")
        String email,

        @Schema(description = "Horodatage de création, qui sert aussi de critère du jour.")
        LocalDateTime dateCreation
) {
}
