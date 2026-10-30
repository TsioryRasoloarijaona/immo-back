package com.tsix.immocrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Corps du POST et du PATCH. PATCH : absent ou null = inchangé, \"\" = vidé pour les textes.")
public record UtilisateurEcritureDto(
        @Schema(description = "Obligatoire au POST, 100 caractères au plus, unique casse ignorée. Ignoré au PATCH.")
        String code,

        @Schema(description = "Obligatoire au POST, jamais vide, 255 caractères au plus.")
        String description,

        String email,

        String telephone,

        String mobile,

        @Schema(description = "Tiers de type Employé (voir /references). Contrôlé seulement s'il change.")
        UUID personnelId,

        @Schema(description = "POST : vrai si absent. true coupe chat et mobile, et la session du compte.")
        Boolean inactif,

        @Schema(description = "IsVisibleForChat. Toujours faux sur un compte inactif.")
        Boolean avecNotification,

        @Schema(description = "CanDisapearNotification. POST : vrai si absent.")
        Boolean peutMasquerNotification,

        @Schema(description = "IsMobileUser. Toujours faux sur un compte inactif.")
        Boolean accesMobile,

        @Schema(description = "255 caractères au plus.")
        String smtpServeur,

        Integer smtpPort,

        Boolean smtpSsl,

        @Schema(description = "AdmWebCultures (voir /references).")
        UUID cultureWebId,

        @Schema(description = "AdmRole (voir /references). Ne donne aucun droit.")
        UUID roleWebId
) {
}
