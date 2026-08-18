package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/prospects/{id}}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code CfgTier}. Adjust once a spec is available.
 */
public record ProspectDetailDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String raisonSociale,
        String telephone,
        String telephone1,
        String telephone2,
        String mobile,
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
        String commercialNom
) {
}
