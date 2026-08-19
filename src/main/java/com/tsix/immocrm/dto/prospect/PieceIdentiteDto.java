package com.tsix.immocrm.dto.prospect;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Grouped identity-document fields — {@code CfgTier.AdmIdentityDocumentTypeId} +
 * {@code IDNumber}/{@code IDDate}/{@code IDPlace}/{@code IDValidateDate}/{@code IDValidatePlace}.
 */
public record PieceIdentiteDto(
        UUID typeId,
        String type,
        String numero,
        LocalDateTime delivreeLe,
        String lieuDelivrance,
        LocalDateTime valideJusquau,
        String lieuValidite
) {
}
