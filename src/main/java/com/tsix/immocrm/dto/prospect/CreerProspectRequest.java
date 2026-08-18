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
 */
public record CreerProspectRequest(
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
