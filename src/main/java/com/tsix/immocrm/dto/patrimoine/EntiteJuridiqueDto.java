package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Entité juridique (CfgCompany) : ligne de la liste et fiche (la fiche ajoute la répartition).")
@Builder
public record EntiteJuridiqueDto(
        @Schema(description = "Identifiant (CfgCompany.PKey).")
        UUID id,
        @Schema(description = "Code, unique sur toute la table.")
        String code,
        @Schema(description = "Libellé (raison sociale).")
        String description,
        @Schema(description = "« Active » : une entité inactive masque ses programmes, tranches, articles, dossiers.")
        boolean actif,
        @Schema(description = "Forme juridique (SARL, SA…).")
        String formeJuridique,
        @Schema(description = "Matricule fiscal (CfgCompany.FiscalNumber).")
        String matriculeFiscal,
        @Schema(description = "Registre du commerce (CfgCompany.CommercialRegister).")
        String registreCommerce,
        @Schema(description = "N° de sécurité sociale (CNSS).")
        String cnss,
        @Schema(description = "Patente.")
        String patente,
        @Schema(description = "Identifiant commun de l'entreprise (ICE).")
        String ice,
        @Schema(description = "Date de création de la société.")
        LocalDateTime dateCreation,
        @Schema(description = "Capital social.")
        BigDecimal capital,
        @Schema(description = "Devise (StlCurrency.PKey).")
        UUID deviseId,
        @Schema(description = "Code de la devise (MAD…).")
        String deviseCode,
        @Schema(description = "Groupe d'actionnaires (CfgGroupShareholder.PKey), null si aucun.")
        UUID groupeId,
        @Schema(description = "Code du groupe d'actionnaires.")
        String groupeCode,
        @Schema(description = "Libellé du groupe d'actionnaires.")
        String groupeLibelle,
        @Schema(description = "« Nombre actionnaires » saisi (TotalSharing) : égal au nombre de lignes de répartition.")
        int nombreActionnaires,
        @Schema(description = "« Nombre de parts (actions) » (SharingNumber) : égal au total des parts réparties.")
        int nombreParts,
        @Schema(description = "« Part maison » en fraction : 0.25 = 25 %.", example = "0.25")
        BigDecimal partMaison,
        @Schema(description = "Date de liquidation, null si aucune.")
        LocalDateTime dateLiquidation,
        @Schema(description = "Adresse 1.")
        String adresse,
        @Schema(description = "Adresse 2.")
        String adresse2,
        @Schema(description = "Adresse 3.")
        String adresse3,
        @Schema(description = "Code postal.")
        String codePostal,
        @Schema(description = "Ville (texte libre).")
        String ville,
        @Schema(description = "Pays (AdmCountry.PKey).")
        UUID paysId,
        @Schema(description = "Libellé du pays.")
        String paysLibelle,
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
        @Schema(description = "« Courrier fait à » (CfgCompany.SignatureCity) : lieu imprimé en pied des courriers.")
        String villeSignature,
        @Schema(description = "Représentant légal (personnel, CfgTier.PKey).")
        UUID representantLegalId,
        @Schema(description = "Nom du représentant légal.")
        String representantLegalLibelle,
        @Schema(description = "« Courrier signataire » (personnel, CfgTier.PKey).")
        UUID signataireId,
        @Schema(description = "Nom du signataire des courriers.")
        String signataireLibelle,
        @Schema(description = "Responsable technique (personnel, CfgTier.PKey).")
        UUID responsableTechniqueId,
        @Schema(description = "Nom du responsable technique.")
        String responsableTechniqueLibelle,
        @Schema(description = "Responsable commercial (personnel, CfgTier.PKey).")
        UUID responsableCommercialId,
        @Schema(description = "Nom du responsable commercial.")
        String responsableCommercialLibelle,
        @Schema(description = "Description détaillée (onglet « Autres », 1000 caractères au plus).")
        String descriptionLongue,
        @Schema(description = "Auteur de la création.")
        String creePar,
        @Schema(description = "Date de création de la fiche.")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification.")
        String modifiePar,
        @Schema(description = "Date de la dernière modification.")
        LocalDateTime modifieLe,
        @Schema(description = "Répartition par actionnaire (onglet « Répartition ») ; null dans la liste, renseignée en fiche.")
        List<LigneRepartitionDto> repartition
) {
}
