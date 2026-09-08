package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = """
        Issue de l'envoi d'une offre de prix, sur les deux canaux. Renvoyé avec un code 200 même
        lorsque rien n'est parti : un prospect sans adresse ni numéro exploitable n'est pas une
        erreur d'appel, et le commercial a besoin du motif pour y remédier.

        L'offre est créée (ou réutilisée) **avant** l'envoi et le reste quoi qu'il advienne de
        celui-ci : `offreId` et `offreCode` sont donc toujours renseignés, y compris quand les
        deux canaux échouent. C'est délibéré — l'offre existe, elle est numérotée, et la perdre
        parce que le prospect est injoignable obligerait à la régénérer sous un autre numéro.

        Les deux canaux sont tentés indépendamment l'un de l'autre.""")
public record ResultatEnvoiOffreDto(

        @Schema(description = "Vrai dès qu'**au moins un** des deux canaux a abouti.", example = "true")
        boolean envoye,

        @Schema(description = "Identifiant de l'offre créée ou réutilisée (`StlQuote`).")
        UUID offreId,

        @Schema(description = "Numéro de l'offre, au format legacy `{année}/{numéro}`.",
                example = "26/00314")
        String offreCode,

        @Schema(description = "Vrai si une offre existante a été réexpédiée plutôt qu'une nouvelle "
                + "créée. N'arrive que si l'appel ne demandait ni remise ni prix.", example = "false")
        boolean offreReutilisee,

        @Schema(description = "Prix de vente TTC après remise, tel qu'annoncé au prospect sur les "
                + "deux canaux.", example = "1282500.00")
        BigDecimal prixFinalTTC,

        @Schema(description = "Issue du canal email. Seul canal à porter le PDF de l'offre.")
        CanalEnvoiDto email,

        @Schema(description = "Issue du canal WhatsApp. Porte le montant en texte, jamais de "
                + "document — et ne mentionne le mail que s'il est réellement parti.")
        CanalEnvoiDto whatsapp
) {

    public static ResultatEnvoiOffreDto de(UUID offreId, String offreCode, boolean offreReutilisee,
                                           BigDecimal prixFinalTTC,
                                           CanalEnvoiDto email, CanalEnvoiDto whatsapp) {
        return new ResultatEnvoiOffreDto(email.envoye() || whatsapp.envoye(), offreId, offreCode,
                offreReutilisee, prixFinalTTC, email, whatsapp);
    }
}
