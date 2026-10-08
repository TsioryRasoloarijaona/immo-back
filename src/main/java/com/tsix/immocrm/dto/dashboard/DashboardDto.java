package com.tsix.immocrm.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code relancesEnRetard} : dossiers dont une {@code ValidityDate} de Revival1/Revival2 est dépassée —
 * interprétation non confirmée par une spec.
 */
public record DashboardDto(
        BigDecimal valeurPipeline,
        BigDecimal panierMoyen,
        List<EtapeCountDto> dossiersParEtape,
        List<RelanceDto> relancesEnRetard,
        long prospectionsOuvertes,
        long prospectionsTotal
) {
}
