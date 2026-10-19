package com.tsix.immocrm.dto.entite;

import com.tsix.immocrm.dto.patrimoine.SaisieCoordonnees;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Création ou modification d'une entité juridique. En PATCH, champ absent ou null = inchangé, "
        + "\"\" = vidé ; noms identiques à la fiche.")
public record EntiteJuridiqueEcritureDto(
        @Schema(description = "Code, unique sur toutes les entités (409). Obligatoire.") String code,
        @Schema(description = "Libellé. Obligatoire.") String description,
        @Schema(description = "Description détaillée (1000 caractères au plus).") String descriptionLongue,
        @Schema(description = "Active ; true à la création si absent. La bascule masque ou rend tout le contenu de l'entité.")
        Boolean isActive,
        String formeJuridique,
        String identifiantFiscal,
        String registreCommerce,
        LocalDateTime dateCreation,
        @Schema(description = "Capital, ≥ 0.") BigDecimal capital,
        String cnss,
        String patente,
        String ice,
        @Schema(description = "Nombre de lignes de répartition attendues, ≥ 0.") Integer nombreActionnaires,
        @Schema(description = "Nombre total de parts, ≥ 0.") Integer nombreParts,
        LocalDateTime dateLiquidation,
        @Schema(description = "Part maison, ≥ 0.") BigDecimal partMaison,
        @Schema(description = "Groupe d'actionnaires ; inconnu → 400.") UUID groupeActionnairesId,
        @Schema(description = "Devise ; à la création, absente = devise MAD (400 si MAD n'existe pas) ; inconnue → 400.")
        UUID deviseId,
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
        @Schema(description = "Pays ; inconnu → 400.") UUID paysId,
        @Schema(description = "Représentant légal (CfgTier) ; inconnu → 400.") UUID representantLegalId,
        @Schema(description = "Courrier signataire (CfgTier) ; inconnu → 400.") UUID signataireId,
        @Schema(description = "Responsable technique (CfgTier) ; inconnu → 400.") UUID responsableTechniqueId,
        @Schema(description = "Responsable commercial (CfgTier) ; inconnu → 400.") UUID responsableCommercialId,
        @Schema(description = "Facultatif : présente = remplace la répartition, contrôlée avec les nombres du même corps.")
        List<LigneRepartitionDto> repartition,
        @Schema(description = "PATCH : noms de champs à vider (date, clé → NULL ; nombre → 0 ; texte → \"\").")
        List<String> champsAVider
) implements SaisieCoordonnees {
}
