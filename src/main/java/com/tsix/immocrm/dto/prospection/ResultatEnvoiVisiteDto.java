package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Issue d'un envoi de la confirmation de visite, sur les deux canaux. Renvoyé avec un code
        200 même lorsque rien n'est parti : un prospect sans adresse email ni numéro exploitable
        n'est pas une erreur d'appel, et le commercial a besoin de savoir *pourquoi* le message
        n'a pas été expédié pour pouvoir y remédier — ce qu'un code 4xx générique ne dirait pas.

        Les deux canaux sont tentés indépendamment l'un de l'autre : l'échec de l'un n'empêche
        jamais l'autre de partir.""")
public record ResultatEnvoiVisiteDto(

        @Schema(description = "Vrai dès qu'**au moins un** des deux canaux a abouti. C'est la "
                + "seule lecture utile pour un affichage binaire « prévenu / pas prévenu ».",
                example = "true")
        boolean envoye,

        @Schema(description = "Issue du canal email.")
        CanalEnvoiDto email,

        @Schema(description = "Issue du canal WhatsApp.")
        CanalEnvoiDto whatsapp
) {

    public static ResultatEnvoiVisiteDto de(CanalEnvoiDto email, CanalEnvoiDto whatsapp) {
        return new ResultatEnvoiVisiteDto(email.envoye() || whatsapp.envoye(), email, whatsapp);
    }
}
