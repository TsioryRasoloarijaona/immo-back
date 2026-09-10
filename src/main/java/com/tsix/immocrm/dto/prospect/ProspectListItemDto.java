package com.tsix.immocrm.dto.prospect;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneProspect;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary shape for {@code GET /api/prospects}.
 *
 * <p><strong>Toutes les clés ne sont pas toujours présentes.</strong> Le paramètre
 * {@code ?colonnes=} sélectionne celles qui sont sérialisées ; les autres sont absentes de la
 * réponse — voir {@link ColonneProspect}, qui fait aussi office de liste blanche et alimente
 * {@code GET /api/referentiels/colonnes/prospects}.
 *
 * <p>Sans ce paramètre, la réponse est <em>exactement</em> celle d'avant son introduction : les
 * dix colonnes historiques, et rien d'autre.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
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

        // ------------------------------------------------------------------
        // Colonnes ajoutées avec le sélecteur. Toutes servies par la fiche depuis longtemps ;
        // la liste n'avait simplement aucun moyen de les demander.
        // Hors sélection par défaut, sans exception.
        // ------------------------------------------------------------------

        @Schema(description = "CIN du prospect (`CfgTier.IDNumber`).")
        String cin,

        @Schema(description = "Identifiant Commun de l'Entreprise (`CfgTier.ICE`). Ne pas "
                + "confondre avec `FiscalCode`, qui est une autre colonne et n'est pas servie.")
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

        @Schema(description = "Profession, alimentée par `CfgTier.Quality` — **mesurée à 0 ligne "
                + "sur 13 664**, donc toujours nulle. Servie pour rester cohérente avec la fiche, "
                + "à ne pas proposer comme critère.")
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
