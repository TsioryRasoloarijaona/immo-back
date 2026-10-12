package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Fiche d'un programme (CfgProject).")
@Builder
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
        @Schema(description = "« Courrier fait à » (CfgProject.SignatureCity).")
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
        @Schema(description = "Entité juridique (CfgCompany.id) ; null si non renseignée.")
        UUID entiteId,
        @Schema(description = "Code de l'entité juridique.")
        String entiteCode,
        @Schema(description = "Libellé de l'entité juridique.")
        String entiteLibelle,
        @Schema(description = "Pays (AdmCountry.id) ; null si non renseigné.")
        UUID paysId,
        @Schema(description = "Libellé du pays.")
        String paysLibelle,
        @Schema(description = "Auteur de la création (UserCreate, libellé du compte).")
        String creePar,
        @Schema(description = "Date de création (UserCreateDateTime).")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification (UserUpdate).")
        String modifiePar,
        @Schema(description = "Date de la dernière modification (UserUpdateDateTime).")
        LocalDateTime modifieLe,
        @Schema(description = "« Courrier signataire » (CfgTier.id) ; null si non renseigné.")
        UUID signataireId,
        @Schema(description = "Nom du signataire.")
        String signataireLibelle,
        @Schema(description = "Responsable technique (CfgTier.id) ; null si non renseigné.")
        UUID responsableTechniqueId,
        @Schema(description = "Nom du responsable technique.")
        String responsableTechniqueLibelle,
        @Schema(description = "Responsable commercial (CfgTier.id) ; null si non renseigné.")
        UUID responsableCommercialId,
        @Schema(description = "Nom du responsable commercial.")
        String responsableCommercialLibelle,
        @Schema(description = "« Représentant légal » : représentant de l'entité juridique (CfgCompany.CfgTierId), lecture seule.")
        String representantLegal
) {
}
