package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création (POST) ou modification partielle (PATCH : champ absent ou null = inchangé) "
        + "d'une entité juridique.")
@Builder
public record EntiteJuridiqueEcritureDto(
        @Schema(description = "Code, unique sur toute la table. Obligatoire à la création.")
        String code,
        @Schema(description = "Libellé. Obligatoire à la création.")
        String description,
        @Schema(description = "« Active » ; défaut true à la création. false masque tout le patrimoine de l'entité.")
        Boolean actif,
        @Schema(description = "Forme juridique.")
        String formeJuridique,
        @Schema(description = "Matricule fiscal.")
        String matriculeFiscal,
        @Schema(description = "Registre du commerce.")
        String registreCommerce,
        @Schema(description = "N° de sécurité sociale (CNSS).")
        String cnss,
        @Schema(description = "Patente.")
        String patente,
        @Schema(description = "Identifiant commun de l'entreprise (ICE).")
        String ice,
        @Schema(description = "Date de création de la société.")
        LocalDateTime dateCreation,
        @Schema(description = "Capital social, positif ou nul.")
        BigDecimal capital,
        @Schema(description = "Devise (StlCurrency.PKey). Obligatoire à la création, non vidable.")
        UUID deviseId,
        @Schema(description = "Groupe d'actionnaires (sans recalcul : voir POST …/repartition/depuis-groupe).")
        UUID groupeId,
        @Schema(description = "« Nombre actionnaires » : doit égaler le nombre de lignes de la répartition.")
        Integer nombreActionnaires,
        @Schema(description = "« Nombre de parts (actions) » : doit égaler le total des parts réparties.")
        Integer nombreParts,
        @Schema(description = "« Part maison » en fraction, entre 0 et 1.", example = "0.25")
        BigDecimal partMaison,
        @Schema(description = "Date de liquidation.")
        LocalDateTime dateLiquidation,
        @Schema(description = "Adresse 1.")
        String adresse,
        @Schema(description = "Adresse 2.")
        String adresse2,
        @Schema(description = "Adresse 3.")
        String adresse3,
        @Schema(description = "Code postal.")
        String codePostal,
        @Schema(description = "Ville.")
        String ville,
        @Schema(description = "Pays (AdmCountry.PKey).")
        UUID paysId,
        @Schema(description = "Téléphone.")
        String telephone,
        @Schema(description = "Mobile.")
        String mobile,
        @Schema(description = "Fax.")
        String fax,
        @Schema(description = "E-mail.")
        String email,
        @Schema(description = "Site web.")
        String siteWeb,
        @Schema(description = "« Courrier fait à ».")
        String villeSignature,
        @Schema(description = "Représentant légal : un personnel (GET /api/referentiels-patrimoine/personnels).")
        UUID representantLegalId,
        @Schema(description = "« Courrier signataire » : un personnel.")
        UUID signataireId,
        @Schema(description = "Responsable technique : un personnel.")
        UUID responsableTechniqueId,
        @Schema(description = "Responsable commercial : un personnel.")
        UUID responsableCommercialId,
        @Schema(description = "Description détaillée (1000 caractères au plus).")
        String descriptionLongue,
        @Schema(description = "Répartition ; présente = remplace toutes les lignes, absente au PATCH = inchangée.")
        List<LigneRepartitionEcritureDto> repartition,
        @Schema(description = "PATCH : noms de champs à vider (texte → \"\", date et référence → null, nombre → 0).")
        List<String> champsAVider
) {
}
