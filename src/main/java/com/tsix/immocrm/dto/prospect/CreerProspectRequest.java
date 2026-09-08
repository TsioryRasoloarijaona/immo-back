package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Body for {@code POST /api/prospects}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available. No fields are marked
 * required at the bean-validation level since none of the underlying CfgTier columns are
 * NOT NULL; the service only requires at least one of nom/raisonSociale to identify the tier.
 *
 * GAP-05: extended with the Omran "Général" tab identity fields that were missing
 * (titre/sexe/pays/nationalite/situationFamiliale/typeResidence/regimeMatrimonial/profession/
 * salaire/employeur/anciennete/filsDe/etDe/adresse2/adresse3/pieceIdentite), plus
 * lieuNaissance/nombreEnfants which were read-only on {@link ProspectDetailDto} but had no
 * write path.
 */
public record CreerProspectRequest(
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /**
         * Indicatif pays du fixe, sans le {@code +} ({@code "212"}). <b>Requis dès que
         * {@code telephone} est fourni</b> — voir {@code FEATURE-indicatif-telephone.md} §5.
         * Valeurs possibles : {@code GET /api/referentiels/pays}, champ
         * {@code indicatifTelephone}.
         */
        String indicatifTelephone,
        String mobile,
        /** Indicatif pays du mobile, mêmes règles que {@link #indicatifTelephone}. */
        String indicatifMobile,
        String email,
        String adresse,
        String ville,
        String codePostal,
        LocalDateTime dateNaissance,
        String lieuNaissance,
        Integer nombreEnfants,
        UUID typeId,
        UUID kindId,
        UUID commercialId,
        UUID titreId,
        UUID sexeId,
        UUID paysResidenceId,
        UUID paysId,
        UUID nationaliteId,
        UUID situationFamilialeId,
        UUID typeResidenceId,
        UUID regimeMatrimonialId,
        UUID professionId,
        UUID salaireId,
        String employeur,
        Integer anciennete,
        String filsDe,
        String etDe,
        String adresse2,
        String adresse3,
        PieceIdentiteRequest pieceIdentite
) {
}
