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
 */
public record ModifierProspectRequest(
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        String mobile,
        String email,
        String adresse,
        String ville,
        String codePostal,
        LocalDateTime dateNaissance,
        UUID typeId,
        UUID kindId,
        UUID commercialId
) {
}
