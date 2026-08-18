package com.tsix.immocrm.dto.dashboard;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One overdue follow-up in {@link DashboardDto#relancesEnRetard()} — a Dossier
 * ({@code ComFolder}) whose {@code Revival1}/{@code Revival2} follow-up validity window has
 * expired unaddressed. {@code dateEcheance} is the earliest overdue {@code ValidityDate} found
 * (Revival1 and Revival2 checked independently); {@code joursRetard} is the whole number of
 * days elapsed since then.
 */
public record RelanceDto(
        UUID dossierId,
        String clientNom,
        LocalDateTime dateEcheance,
        long joursRetard
) {
}
