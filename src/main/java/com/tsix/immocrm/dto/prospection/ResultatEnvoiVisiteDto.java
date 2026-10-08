package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResultatEnvoiVisiteDto(

        @Schema(example = "true")
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
