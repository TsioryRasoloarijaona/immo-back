package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issue de l'envoi sur un canal donné (email ou WhatsApp). Les deux canaux "
        + "sont indépendants : l'un peut aboutir pendant que l'autre échoue, et c'est le cas "
        + "attendu plus d'une fois sur deux — 99 % des prospects ont un numéro exploitable, "
        + "seulement 57 % une adresse email.")
public record CanalEnvoiDto(

        @Schema(description = "Vrai si le message est parti sur ce canal.", example = "true")
        boolean envoye,

        @Schema(description = "Raison de l'issue sur ce canal.")
        MotifEnvoiVisite motif,

        @Schema(description = "Destinataire retenu, en clair — le commercial doit pouvoir "
                + "vérifier que c'est le bon. Adresse email ou numéro au format international "
                + "selon le canal. `null` si aucun destinataire exploitable.",
                example = "+212661224857", nullable = true)
        String destinataire,

        @Schema(description = "Nombre de plans joints. Toujours 0 sur le canal WhatsApp : "
                + "l'envoi d'un document par UltraMsg exige une URL publique, or les URL du "
                + "stockage expirent en quinze minutes.", example = "1")
        int nombrePiecesJointes
) {

    public static CanalEnvoiDto nonEnvoye(MotifEnvoiVisite motif, String destinataire) {
        return new CanalEnvoiDto(false, motif, destinataire, 0);
    }

    public static CanalEnvoiDto envoye(String destinataire, int nombrePiecesJointes) {
        return new CanalEnvoiDto(true, MotifEnvoiVisite.ENVOYE, destinataire, nombrePiecesJointes);
    }
}
