package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Entité juridique (CfgCompany) : les 39 colonnes, les libellés des clés étrangères et la répartition. "
        + "Mêmes noms qu'en écriture.")
public record EntiteJuridiqueDto(
        UUID id,
        @Schema(description = "Code, unique sur toutes les entités, actives ou non.") String code,
        @Schema(description = "Libellé.") String description,
        @Schema(description = "« Description détaillée » (onglet Autres).") String descriptionLongue,
        @Schema(description = "« Active » : false ou null masque l'entité et tout ce qui en dépend dans le CRM.")
        Boolean isActive,
        @Schema(description = "Forme juridique, texte libre.") String formeJuridique,
        @Schema(description = "« Matricule fiscale » (FiscalNumber).") String identifiantFiscal,
        @Schema(description = "Registre du commerce.") String registreCommerce,
        @Schema(description = "Date de création de la société.") LocalDateTime dateCreation,
        BigDecimal capital,
        String cnss,
        String patente,
        @Schema(description = "« Identifiant commun de l'entreprise ».") String ice,
        @Schema(description = "« Nombre actionnaires » (TotalSharing) : nombre de lignes de répartition attendues.")
        Integer nombreActionnaires,
        @Schema(description = "« Nombre de parts (actions) » (SharingNumber) : total attendu de la répartition.")
        Integer nombreParts,
        @Schema(description = "Date de liquidation.") LocalDateTime dateLiquidation,
        @Schema(description = "« Part maison » (HomePart).") BigDecimal partMaison,
        @Schema(description = "Groupe d'actionnaires (CfgGroupShareholder), modèle de la répartition.")
        UUID groupeActionnairesId,
        String groupeActionnairesCode,
        String groupeActionnairesLibelle,
        @Schema(description = "Devise (StlCurrency).") UUID deviseId,
        String deviseCode,
        String deviseLibelle,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        @Schema(description = "« Courrier fait à » : ville imprimée avant la date des éditions.") String villeSignature,
        String telephone,
        String mobile,
        String fax,
        String email,
        String siteWeb,
        UUID paysId,
        String paysLibelle,
        @Schema(description = "Représentant légal (CfgTierId → CfgTier), un personnel.") UUID representantLegalId,
        String representantLegalLibelle,
        @Schema(description = "« Courrier signataire » (AdmSignatoryId → CfgTier).") UUID signataireId,
        String signataireLibelle,
        UUID responsableTechniqueId,
        String responsableTechniqueLibelle,
        UUID responsableCommercialId,
        String responsableCommercialLibelle,
        @Schema(description = "UserCreate : auteur de la création (OMRAN ou Arpent).") String creePar,
        LocalDateTime creeLe,
        @Schema(description = "UserUpdate : auteur de la dernière modification.") String modifiePar,
        LocalDateTime modifieLe,
        @Schema(description = "Répartition des parts, par code d'actionnaire ; null dans la liste.")
        List<LigneRepartitionDto> repartition
) {
}
