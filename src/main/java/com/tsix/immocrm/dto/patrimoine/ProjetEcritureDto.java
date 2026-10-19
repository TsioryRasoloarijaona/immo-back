package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création ou modification d'un programme (CfgProject). En PATCH, champ absent ou null = inchangé, "
        + "\"\" = vidé.")
public record ProjetEcritureDto(
        @Schema(description = "Entité (CfgCompany) active. Obligatoire à la création.") UUID entiteId,
        @Schema(description = "Code, unique sur tous les programmes (comparé sans casse ni espaces). Obligatoire.")
        String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Description longue (500 caractères au plus).") String descriptionLongue,
        @Schema(description = "Adresse, ligne 1.") String adresse,
        @Schema(description = "Adresse, ligne 2.") String adresse2,
        @Schema(description = "Adresse, ligne 3.") String adresse3,
        @Schema(description = "Code postal.") String codePostal,
        @Schema(description = "Ville, texte libre.") String ville,
        @Schema(description = "Ville de signature des actes.") String villeSignature,
        @Schema(description = "Téléphone.") String telephone,
        @Schema(description = "Mobile.") String mobile,
        @Schema(description = "Fax.") String fax,
        @Schema(description = "Email.") String email,
        @Schema(description = "Site web.") String siteWeb,
        @Schema(description = "Pays (AdmCountry), facultatif.") UUID paysId,
        @Schema(description = "Début des travaux.") LocalDateTime dateDebutTravaux,
        @Schema(description = "Fin des travaux, postérieure ou égale au début.") LocalDateTime dateFinTravaux,
        @Schema(description = "Début de commercialisation.") LocalDateTime dateDebutCommercialisation,
        @Schema(description = "Fin de commercialisation, postérieure ou égale au début.")
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "« Courrier signataire » (AdmSignatoryId → CfgTier), voir /api/referentiels-patrimoine/personnels.")
        UUID signataireId,
        @Schema(description = "Responsable technique (CfgTechnicalManagerId → CfgTier), un personnel.")
        UUID responsableTechniqueId,
        @Schema(description = "Responsable commercial (CfgSalesManagerId → CfgTier), un personnel.")
        UUID responsableCommercialId,
        @Schema(description = "PATCH : noms de champs à vider (date, pays, personnel → NULL ; texte → \"\").")
        List<String> champsAVider
) implements SaisieCoordonnees {
}
