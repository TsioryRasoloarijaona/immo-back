package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
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
        PieceIdentiteDto pieceIdentite
) {
}
