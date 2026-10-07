package com.tsix.immocrm.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client = tout {@code CfgTier} référencé par {@code ComFolder.CfgCustomerId}, quels que soient son
 * type et sa nature ; {@code /api/prospects} filtre, lui, sur {@code IsProspect}.
 */
public record ClientListItemDto(
        UUID id,
        String code,
        String nomAffiche,
        String mobile,
        String telephone,
        String email,
        String ville,
        Integer nbDossiers,
        BigDecimal totalAPayer,
        BigDecimal totalRegle,
        BigDecimal totalReste
) {
}
