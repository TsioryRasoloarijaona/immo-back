package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Body for {@code PATCH /api/prospects/{id}}. Every field is optional — only non-null
 * fields are applied (partial update).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
 *
 * GAP-05: same new fields as {@link CreerProspectRequest} — see its doc-comment.
 */
public record ModifierProspectRequest(
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        /**
         * Indicatif pays du fixe, sans le {@code +}. <b>Requis dès que {@code telephone} est
         * fourni</b>, sans reprise implicite de celui déjà en base : un numéro qui change peut
         * changer de pays. Une modification qui ne touche pas au téléphone n'a rien à fournir.
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
