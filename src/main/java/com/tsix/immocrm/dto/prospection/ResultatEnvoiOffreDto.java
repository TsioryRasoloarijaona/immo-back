package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultatEnvoiOffreDto(

        @Schema(description = "Vrai dès qu'**au moins un** des deux canaux a abouti.", example = "true")
        boolean envoye,

        @Schema(description = "Identifiant de l'offre créée ou réutilisée (`StlQuote`).")
        UUID offreId,

        @Schema(description = "Numéro de l'offre, au format legacy `{année}/{numéro}`.",
                example = "26/00314")
        String offreCode,

        @Schema(example = "false")
        boolean offreReutilisee,

        @Schema(description = "Prix de vente TTC après remise, tel qu'annoncé au prospect sur les "
                + "deux canaux.", example = "1282500.00")
        BigDecimal prixFinalTTC,

        @Schema(description = "Issue du canal email. Seul canal à porter le PDF de l'offre.")
        CanalEnvoiDto email,

        CanalEnvoiDto whatsapp
) {

    public static ResultatEnvoiOffreDto de(UUID offreId, String offreCode, boolean offreReutilisee,
                                           BigDecimal prixFinalTTC,
                                           CanalEnvoiDto email, CanalEnvoiDto whatsapp) {
        return new ResultatEnvoiOffreDto(email.envoye() || whatsapp.envoye(), offreId, offreCode,
                offreReutilisee, prixFinalTTC, email, whatsapp);
    }
}
