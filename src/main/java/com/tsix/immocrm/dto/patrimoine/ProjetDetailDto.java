package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjetDetailDto(
        UUID id,
        String code,
        String description,
        String descriptionLongue,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        String villeSignature,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        LocalDateTime dateDebutTravaux,
        LocalDateTime dateFinTravaux,
        LocalDateTime dateDebutCommercialisation,
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "Entité (CfgCompany) du programme.") UUID entiteId,
        String entiteCode,
        String entiteLibelle,
        @Schema(description = "Pays (AdmCountry), null si non renseigné.") UUID paysId,
        String paysLibelle,
        @Schema(description = "UserCreate : auteur de la création (OMRAN ou Arpent).") String creePar,
        LocalDateTime creeLe,
        @Schema(description = "UserUpdate : auteur de la dernière modification.") String modifiePar,
        LocalDateTime modifieLe,
        @Schema(description = "« Courrier signataire » (AdmSignatoryId), null si non renseigné.") UUID signataireId,
        String signataireLibelle,
        @Schema(description = "Responsable technique (CfgTechnicalManagerId).") UUID responsableTechniqueId,
        String responsableTechniqueLibelle,
        @Schema(description = "Responsable commercial (CfgSalesManagerId).") UUID responsableCommercialId,
        String responsableCommercialLibelle
) {
}
