package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/prospects/{id}}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
 *
 * GAP-05: extended with the Omran "Général" tab identity fields (read side).
 */
public record ProspectDetailDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /**
         * Indicatif pays du fixe, sans le {@code +} ({@code "212"}). Sert à préremplir le
         * sélecteur en édition — sans lui, rouvrir une fiche et la réenregistrer obligerait à
         * rechoisir le pays à chaque fois.
         */
        String indicatifTelephone,
        String telephone1,
        String telephone2,
        String mobile,
        /** Indicatif pays du mobile, même usage que {@link #indicatifTelephone}. */
        String indicatifMobile,
        String fax,
        String email,
        String siteWeb,
        String adresse,
        String adresse2,
        String adresse3,
        String codePostal,
        String ville,
        LocalDateTime dateNaissance,
        String lieuNaissance,
        Integer nombreEnfants,
        String profession,
        String notes,
        String ice,
        Boolean estAncienClient,
        UUID typeId,
        String type,
        UUID kindId,
        String kind,
        UUID commercialId,
        String commercialNom,
        UUID titreId,
        String titre,
        UUID sexeId,
        String sexe,
        UUID paysResidenceId,
        String paysResidence,
        UUID paysId,
        String pays,
        UUID nationaliteId,
        String nationalite,
        UUID situationFamilialeId,
        String situationFamiliale,
        UUID typeResidenceId,
        String typeResidence,
        UUID regimeMatrimonialId,
        String regimeMatrimonial,
        UUID professionId,
        UUID salaireId,
        String salaire,
        String employeur,
        Integer anciennete,
        String filsDe,
        String etDe,
        PieceIdentiteDto pieceIdentite,

        // ------------------------------------------------------------------
        // Piste d'audit et tranches d'intérêt — voir
        // instructions/FEATURE-audit-tranches-prospect.md.
        // ------------------------------------------------------------------

        @Schema(description = "Auteur de la création de la fiche. Renseigné sur **100 %** des "
                + "13 664 prospects réels, avec 55 auteurs distincts : c'est une vraie piste "
                + "d'audit, pas un champ technique.")
        String creePar,

        @Schema(description = "Date de création de la fiche. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 99,9 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 99,9 %.")
        LocalDateTime modifieLe,

        @Schema(description = "Tranches auxquelles le prospect s'est intéressé, toutes ses "
                + "prospections confondues : la tranche **principale** de chacune et ses "
                + "**intérêts secondaires**, dédoublonnés.\n\n"
                + "**Toujours présent, vide plutôt qu'absent** quand le prospect n'a aucune "
                + "prospection. Court par nature — mesuré sur les 13 664 prospects réels : "
                + "13 246 ont une seule tranche, 254 en ont deux, 8 en ont trois.")
        List<TrancheInteretDto> tranchesInteret
) {
}
