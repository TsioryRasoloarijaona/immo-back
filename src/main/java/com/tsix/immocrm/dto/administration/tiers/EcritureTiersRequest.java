package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Champs saisis d'une fiche ; un champ de la famille omis d'un PUT est vidé. Les {@code @Size} suivent les colonnes de {@code CfgTier}. */
@Schema(description = "Fiche de tiers à écrire ; type et auteur ne se saisissent pas.")
public record EcritureTiersRequest(
        @Schema(description = "Vrai = morale, faux = physique. Requis, faux pour personnels ; banques et organismes : ignoré.")
        Boolean personneMorale,
        @Schema(description = "Requise pour une personne morale ; ignorée pour une personne physique.")
        @Size(max = 255) String raisonSociale,
        @Schema(description = "Requis pour une personne physique ; ignoré pour une personne morale.")
        @Size(max = 255) String prenom,
        @Schema(description = "Requis pour une personne physique ; ignoré pour une personne morale.")
        @Size(max = 255) String nom,
        @Schema(description = "Titre (AdmTierTitle), requis pour une personne physique.") UUID titreId,
        @Schema(description = "Sexe (AdmSexType), requis pour une personne physique.") UUID sexeId,
        @Schema(description = "Nature (CfgTierKind) du type de la famille.") UUID natureId,
        @Schema(description = "Forme sociale (AdmSocialForm). Requise ; personnels : facultative ; banques, organismes : ignorée.")
        UUID formeSocialeId,
        @Schema(description = "Devise (StlCurrency). Requise ; personnels : facultative ; banques, organismes : ignorée.") UUID deviseId,
        @Schema(description = "Assujettissement (AdmLiability).") UUID assujettissementId,
        @Schema(description = "Pays (AdmCountry). Requis pour banques et organismes.") UUID paysId,
        @Schema(description = "Pays de résidence (AdmCountry). Requis ; banques et organismes : ignoré, égal à paysId.") UUID paysResidenceId,
        @Schema(description = "Ville du référentiel (AdmCity).") UUID villeId,
        @Schema(description = "Ville en clair.") @Size(max = 255) String ville,
        @Schema(description = "Code postal.") @Size(max = 255) String codePostal,
        @Schema(description = "Adresse, ligne 1.") @Size(max = 255) String adresse,
        @Schema(description = "Adresse, ligne 2.") @Size(max = 255) String adresse2,
        @Schema(description = "Adresse, ligne 3.") @Size(max = 255) String adresse3,
        @Schema(description = "Indicatif du mobile.") @Size(max = 10) String mobileIndicatif,
        @Schema(description = "Mobile ; mobile ou email requis, sauf banques et organismes.") @Size(max = 30) String mobile,
        @Schema(description = "Indicatif du téléphone.") @Size(max = 10) String telephoneIndicatif,
        @Schema(description = "Téléphone.") @Size(max = 30) String telephone,
        @Schema(description = "Indicatif du téléphone 1.") @Size(max = 10) String telephone1Indicatif,
        @Schema(description = "Téléphone 1.") @Size(max = 30) String telephone1,
        @Schema(description = "Indicatif du téléphone 2.") @Size(max = 10) String telephone2Indicatif,
        @Schema(description = "Téléphone 2.") @Size(max = 30) String telephone2,
        @Schema(description = "Indicatif du fax.") @Size(max = 10) String faxIndicatif,
        @Schema(description = "Fax.") @Size(max = 30) String fax,
        @Schema(description = "E-mail ; mobile ou email requis, sauf banques et organismes.") @Size(max = 100) String email,
        @Schema(description = "Site web.") @Size(max = 100) String siteWeb,
        @Schema(description = "Notes.") @Size(max = 1000) String notes,
        @Schema(description = "Identifiant commun de l'entreprise.") @Size(max = 255) String ice,
        @Schema(description = "Matricule fiscal.") @Size(max = 255) String matriculeFiscal,
        @Schema(description = "Registre de commerce.") @Size(max = 255) String registreCommerce,
        @Schema(description = "Douane.") @Size(max = 255) String douane,
        @Schema(description = "Code comptable.") @Size(max = 255) String codeComptable,
        @Schema(description = "Code comptable collectif.") @Size(max = 255) String codeComptableCollectif,
        @Schema(description = "Banque : tiers de type BAN.") UUID banqueId,
        @Schema(description = "Agence bancaire.") @Size(max = 255) String agence,
        @Schema(description = "RIB.") @Size(max = 255) String rib,
        @Schema(description = "IBAN.") @Size(max = 255) String iban,
        @Schema(description = "Taux de retenue à la source, tel que saisi.") @Digits(integer = 20, fraction = 6) BigDecimal tauxRetenue,
        @Schema(description = "Taux de TVA, tel que saisi.") @Digits(integer = 20, fraction = 6) BigDecimal tauxTva,
        @Schema(description = "Délai de paiement.") Integer delaiPaiement,
        @Schema(description = "Est un prestataire (IsExtern) ; absent = faux.") Boolean estPrestataire,
        @Schema(description = "Note (AdmRank).") UUID noteId,
        @Schema(description = "Date de naissance, de 1900 à 2078.") LocalDate dateNaissance,
        @Schema(description = "Lieu de naissance.") @Size(max = 255) String lieuNaissance,
        @Schema(description = "Situation familiale (AdmFamilySituation).") UUID situationFamilialeId,
        @Schema(description = "Nationalité (AdmNationality).") UUID nationaliteId,
        @Schema(description = "Profession (AdmProfession).") UUID professionId,
        @Schema(description = "Type de pièce d'identité (AdmIdentityDocumentType).") UUID typePieceId,
        @Schema(description = "Numéro de la pièce d'identité.") @Size(max = 255) String numeroPiece,
        @Schema(description = "Banques, organismes : saisi, requis, unique dans le type. Ailleurs : compteur, ignoré en écriture.")
        @Size(max = 255) String code,
        @Schema(description = "Banques, organismes : requise, mise en majuscules. Ailleurs : suit la raison sociale, ignorée en écriture.")
        @Size(max = 255) String description,
        @Schema(description = "Compte comptable (ActAccount) ; banques et organismes seulement.") UUID compteComptableId,
        @Schema(description = "Journal (ActJournal) ; banques et organismes seulement.") UUID journalId) {
}
