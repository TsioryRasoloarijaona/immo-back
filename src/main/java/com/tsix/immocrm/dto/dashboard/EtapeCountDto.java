package com.tsix.immocrm.dto.dashboard;

/** One row in {@link DashboardDto#dossiersParEtape()}. */
public record EtapeCountDto(
        String etape,
        long nombre
) {
}
