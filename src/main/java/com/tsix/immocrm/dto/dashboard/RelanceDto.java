package com.tsix.immocrm.dto.dashboard;

import java.time.LocalDateTime;
import java.util.UUID;

/** {@code dateEcheance} : la plus ancienne {@code ValidityDate} dépassée, Revival1 et Revival2 pris séparément. */
public record RelanceDto(
        UUID dossierId,
        String clientNom,
        LocalDateTime dateEcheance,
        long joursRetard
) {
}
