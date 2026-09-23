package com.tsix.immocrm.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shape for {@code GET /api/dashboard}.
 *
 * NOTE: field naming/scope is not backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derivations:
 * <ul>
 *   <li>{@code valeurPipeline}/{@code panierMoyen} are computed over "active" dossiers, i.e.
 *       excluding the 2 exit {@link com.tsix.immocrm.domain.StatutDossier} statuses
 *       (ANNULATION, DESISTEMENT) and dossiers with no status set.</li>
 *   <li>{@code relancesEnRetard} lists dossiers where {@code Revival1}/{@code Revival2} is
 *       set and its paired {@code ValidityDate} is in the past — assumed to mean "the
 *       follow-up's validity window has expired unaddressed". Not confirmed by a spec doc.</li>
 *   <li>{@code prospectionsOuvertes}/{@code prospectionsTotal} exclude pré-prospections
 *       ({@code StrEntity.Code = 'ComPreProspection'}), consistent with
 *       {@code GET /api/prospections} — "ouvertes" means {@code IsClosed} is false or null.</li>
 * </ul>
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
