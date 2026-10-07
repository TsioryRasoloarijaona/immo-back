package com.tsix.immocrm.dto.prospect;

import java.util.UUID;

/** {@code CfgTier} dont {@code CfgTierKind.code} est COM, AGC, ATC, ACC ou RC (source validée par l'utilisateur). */
public record CommercialListItemDto(
        UUID id,
        String code,
        String prenom,
        String nom,
        String email,
        String telephone,
        String kind
) {
}
