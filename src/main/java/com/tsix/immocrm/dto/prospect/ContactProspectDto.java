package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Ligne {@code CfgTierContact}, jointe au {@code CfgTier} lié pour l'affichage. Les coordonnées
 * (mobile, téléphone, e-mail) et le nom viennent du {@code CfgTier} de la personne-contact
 * ({@code CfgContactId}), comme dans la requête d'affichage du légataire.
 */
public record ContactProspectDto(
        @Schema(description = "Id du lien CfgTierContact (sert au DELETE).")
        UUID id,
        @Schema(description = "Id du CfgTier de la personne-contact.")
        UUID contactProspectId,
        @Schema(description = "Nom affiché de la personne-contact (CfgTier).")
        String nomContact,
        @Schema(description = "Mobile de la personne-contact (CfgTier.Mobile).")
        String mobileContact,
        @Schema(description = "Téléphone fixe de la personne-contact (CfgTier.Phone).")
        String telephoneContact,
        @Schema(description = "E-mail de la personne-contact (CfgTier.EMail).")
        String emailContact,
        @Schema(description = "Id du type de relation (CfgRelationType).")
        UUID relationTypeId,
        @Schema(description = "Libellé du type de relation, p.ex. « Conjoint ».")
        String relationType
) {
}
