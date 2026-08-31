package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/dossiers}: creates a Dossier from an existing Prospection.
 *
 * NOTE (confirmed): {@code articleIds} is a list, never a single {@code articleId} — a
 * Dossier can link several {@code StkItem}s (e.g. a main lot + its annex).
 *
 * {@code modePaieId} ({@code ComPaymentMethod}) and {@code cin} (buyer's national ID number)
 * are required at creation, per business decision — even though the underlying DB columns
 * ({@code ComFolder.ComPaymentMethodId}, {@code CfgTier.IDNumber}) are nullable. {@code cin}
 * is written onto the linked prospect's {@code CfgTier.IDNumber} (visible afterwards via
 * {@code GET /api/prospects/{id}} → {@code pieceIdentite.numero}) — it does not create a
 * separate CIN field on the Dossier itself, since identity documents belong to the person,
 * not the sale.
 */
public record CreerDossierRequest(
        UUID prospectionId,
        List<UUID> articleIds,
        BigDecimal prix,
        String notes,
        UUID modePaieId,
        String cin
) {
}
