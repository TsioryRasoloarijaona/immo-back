package com.tsix.immocrm.dto.prospect;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneProspect;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seules les clés choisies par {@code ?colonnes=} sont sérialisées (filtre Jackson) ;
 * {@link ColonneProspect} est la liste blanche.
 */
@JsonFilter(ColonneProspect.FILTRE)
public record ProspectListItemDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        String mobile,
        String email,
        String ville,
        String type,

        // Hors sélection par défaut.

        @Schema(description = "CIN du prospect (`CfgTier.IDNumber`).")
        String cin,

        String ice,

        @Schema(description = "Date de naissance.")
        LocalDateTime dateNaissance,

        @Schema(description = "Lieu de naissance.")
        String lieuNaissance,

        @Schema(description = "Nombre d'enfants déclarés.")
        Integer nombreEnfants,

        @Schema(description = "Le prospect est un ancien client.")
        Boolean estAncienClient,

        @Schema(description = "Nom du père (`FatherName`).")
        String filsDe,

        @Schema(description = "Nom de la mère (`MotherName`).")
        String etDe,

        @Schema(description = "Indicatif pays du fixe, sans le `+`.", example = "212")
        String indicatifTelephone,

        @Schema(description = "Second téléphone.")
        String telephone1,

        @Schema(description = "Troisième téléphone.")
        String telephone2,

        @Schema(description = "Indicatif pays du mobile, sans le `+`.", example = "212")
        String indicatifMobile,

        @Schema(description = "Fax. **Quasi mort en base réelle.**")
        String fax,

        @Schema(description = "Site web.")
        String siteWeb,

        @Schema(description = "Adresse.")
        String adresse,

        @Schema(description = "Complément d'adresse. Absent de la vue légataire — cette donnée "
                + "n'existe que dans notre API et dans la table.")
        String adresse2,

        @Schema(description = "Second complément d'adresse. Même remarque que `adresse2`.")
        String adresse3,

        @Schema(description = "Code postal. Même remarque que `adresse2`.")
        String codePostal,

        @Schema(description = "Employeur.")
        String employeur,

        @Schema(description = "Ancienneté professionnelle, en années.")
        Integer anciennete,

        @Schema(description = "Genre du tiers (`CfgTierKind`). **Quasi mort** : 14 339 des 14 516 "
                + "tiers réels n'en portent aucun.")
        String kind,

        @Schema(description = "Titre (M., Mme…).")
        String titre,

        @Schema(description = "Sexe.")
        String sexe,

        @Schema(description = "Nationalité.")
        String nationalite,

        @Schema(description = "Situation familiale.")
        String situationFamiliale,

        @Schema(description = "Régime matrimonial.")
        String regimeMatrimonial,

        @Schema(description = "Pays.")
        String pays,

        @Schema(description = "Pays de résidence.")
        String paysResidence,

        @Schema(description = "Type de résidence.")
        String typeResidence,

        String profession,

        @Schema(description = "Tranche de salaire (`AdmSalary`). Rare : 0,2 % des prospects.")
        String salaire,

        @Schema(description = "Identifiant du commercial affecté.")
        UUID commercialId,

        @Schema(description = "Nom du commercial affecté.")
        String commercialNom,

        @Schema(description = "Auteur de la création. ~100 % en base réelle, 55 auteurs distincts.")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 99,9 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 99,9 %.")
        LocalDateTime modifieLe
) {
}
