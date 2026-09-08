package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Issue d'un envoi de la confirmation de visite. Renvoyé avec un code 200 même lorsque rien
        n'est parti : un prospect sans adresse email n'est pas une erreur d'appel, et le
        commercial a besoin de savoir *pourquoi* le message n'a pas été expédié pour pouvoir y
        remédier — ce qu'un code 4xx générique ne dirait pas.""")
public record ResultatEnvoiVisiteDto(

        @Schema(description = "Vrai si le message a été remis au service d'envoi. Faux n'implique "
                + "aucune anomalie technique : voir `motif`.", example = "true")
        boolean envoye,

        @Schema(description = "Raison de l'issue. `DESTINATAIRE_ABSENT` se corrige en saisissant "
                + "l'email du prospect puis en rappelant cet endpoint ; `SERVICE_INACTIF` relève "
                + "de la configuration du serveur.")
        MotifEnvoiVisite motif,

        @Schema(description = "Adresse retenue, en clair — le commercial doit pouvoir vérifier "
                + "qu'elle est la bonne. `null` lorsque aucune adresse exploitable n'a été trouvée.",
                example = "client@exemple.ma", nullable = true)
        String destinataire,

        @Schema(description = "Nombre de plans effectivement joints au message. Vaut le plus "
                + "souvent 0 : le lot précis n'est connu que dans 3 % des prises de rendez-vous, "
                + "et aucune plaquette n'est encore déposée au niveau des projets.", example = "1")
        int nombrePiecesJointes
) {

    public static ResultatEnvoiVisiteDto nonEnvoye(MotifEnvoiVisite motif, String destinataire) {
        return new ResultatEnvoiVisiteDto(false, motif, destinataire, 0);
    }
}
