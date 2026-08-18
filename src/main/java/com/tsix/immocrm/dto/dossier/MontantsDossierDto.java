package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shape for {@code GET /api/dossiers/{id}/montants} — always computed server-side from the
 * échéancier ({@code ComDocument}), never stored.
 *
 * NOTE: "ne pas compter FRAIS dans le total soumis à la règle prix TTC" is implemented via
 * {@code ComDocument.excludeFromPrice} (the schema's own boolean for exactly this purpose)
 * rather than string-matching a "FRAIS" category code, since category codes are
 * admin-configurable free text. FEATURE-plan-financement.md (referenced by the endpoint spec)
 * is not present in this repo to confirm the exact rule — adjust if it specifies otherwise.
 */
public record MontantsDossierDto(
        List<CategorieMontantDto> parCategorie,
        BigDecimal totalGeneral,
        BigDecimal totalSoumisReglePrixTTC
) {
}
