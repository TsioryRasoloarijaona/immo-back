package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers}: creates a Dossier from an existing Prospection.
 *
 * NOTE (confirmed): {@code articleIds} is a list, never a single {@code articleId} — a
 * Dossier can link several {@code StkItem}s (e.g. a main lot + its annex).
 */
public record CreerDossierRequest(
        UUID prospectionId,
        List<UUID> articleIds,
        BigDecimal prix,
        String notes
) {
}
