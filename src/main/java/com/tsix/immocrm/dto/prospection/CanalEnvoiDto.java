package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import io.swagger.v3.oas.annotations.media.Schema;

public record CanalEnvoiDto(

        @Schema(description = "Vrai si le message est parti sur ce canal.", example = "true")
        boolean envoye,

        @Schema(description = "Raison de l'issue sur ce canal.")
        MotifEnvoiVisite motif,

        @Schema(example = "+212661224857", nullable = true)
        String destinataire,

        @Schema(example = "0")
        int nombrePiecesJointes
) {

    public static CanalEnvoiDto nonEnvoye(MotifEnvoiVisite motif, String destinataire) {
        return new CanalEnvoiDto(false, motif, destinataire, 0);
    }

    public static CanalEnvoiDto envoye(String destinataire, int nombrePiecesJointes) {
        return new CanalEnvoiDto(true, MotifEnvoiVisite.ENVOYE, destinataire, nombrePiecesJointes);
    }
}
