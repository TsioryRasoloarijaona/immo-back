package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création (POST) ou modification partielle (PATCH) d'un programme (CfgProject).")
@Builder
public record ProjetEcritureDto(
        @Schema(description = "Entité juridique (CfgCompany.id), active. Obligatoire à la création.")
        UUID entiteId,
        @Schema(description = "Code saisi, unique sur toute la table (casse ignorée). Obligatoire à la création.",
                example = "LAZ")
        String code,
        @Schema(description = "Libellé du programme. Obligatoire à la création.", example = "VILLA LAZRAK")
        String description,
        @Schema(description = "Description détaillée (500 caractères au plus).")
        String descriptionLongue,
        @Schema(description = "Adresse, ligne 1.")
        String adresse,
        @Schema(description = "Adresse, ligne 2.")
        String adresse2,
        @Schema(description = "Adresse, ligne 3.")
        String adresse3,
        @Schema(description = "Code postal.")
        String codePostal,
        @Schema(description = "Ville, texte libre (pas de référentiel, comme OMRAN).")
        String ville,
        @Schema(description = "« Courrier fait à » : ville des courriers.")
        String villeSignature,
        @Schema(description = "Téléphone fixe.")
        String telephone,
        @Schema(description = "Téléphone mobile.")
        String mobile,
        @Schema(description = "Fax.")
        String fax,
        @Schema(description = "Adresse email.")
        String email,
        @Schema(description = "Site web.")
        String siteWeb,
        @Schema(description = "Pays (AdmCountry.id, voir GET /api/referentiels/pays).")
        UUID paysId,
        @Schema(description = "Date de lancement des travaux.")
        LocalDateTime dateDebutTravaux,
        @Schema(description = "Date de fin des travaux, postérieure ou égale au lancement.")
        LocalDateTime dateFinTravaux,
        @Schema(description = "Date de début de commercialisation.")
        LocalDateTime dateDebutCommercialisation,
        @Schema(description = "Date de fin de commercialisation, postérieure ou égale au début.")
        LocalDateTime dateFinCommercialisation,
        @Schema(description = "« Courrier signataire » (AdmSignatoryId) : un personnel, voir /api/referentiels-patrimoine/personnels.")
        UUID signataireId,
        @Schema(description = "Responsable technique (CfgTechnicalManagerId) : un personnel.")
        UUID responsableTechniqueId,
        @Schema(description = "Responsable commercial (CfgSalesManagerId) : un personnel.")
        UUID responsableCommercialId,
        @ArraySchema(arraySchema = @Schema(description = "Noms des champs à vider (PATCH) ; un champ obligatoire ou inconnu → 400."),
                schema = @Schema(example = "paysId"))
        List<String> champsAVider
) {
}
