package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/** Colonnes du tableau Liste de la page PROGRAMMES de l'aide OMRAN, sous les noms de {@link ProjetDetailDto}. */
public record ProjetListItemDto(
        UUID id,
        String code,
        String description,
        String ville,
        @Schema(description = "Code de l'entité (CfgCompany) du programme.") String entiteCode,
        String adresse,
        @Schema(description = "« Courrier fait à » (SignatureCity).") String villeSignature,
        @Schema(description = "« Courrier signataire ».") String signataireLibelle,
        @Schema(description = "Représentant légal de l'entité (CfgCompany.CfgTierId).") String representantLegalLibelle,
        String responsableCommercialLibelle,
        String responsableTechniqueLibelle,
        String descriptionLongue,
        String email,
        String fax,
        String mobile,
        String telephone,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation,
        String creePar,
        LocalDateTime creeLe,
        String modifiePar,
        LocalDateTime modifieLe
) {
}
