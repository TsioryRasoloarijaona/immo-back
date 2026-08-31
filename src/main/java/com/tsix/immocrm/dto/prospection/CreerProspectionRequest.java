package com.tsix.immocrm.dto.prospection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body for {@code POST /api/prospections} — instructions/from_front.md demande #9. Creates a
 * fully-qualified prospection in one call, for the front's "Nouvelle prospection" flow where the
 * prospect already exists ({@code POST /api/prospects}, already wired) — {@code prospectId} is
 * therefore required, unlike the separate pré-prospection flow
 * ({@code POST /api/pre-prospections/{id}/creer-prospect} + {@code POST .../transformer}), which
 * stays for raw/unidentified leads (walk-ins, inbound calls with no known prospect yet, via
 * {@code tempDescription}/{@code tempTel}/{@code tempEmail} — not created by this endpoint).
 *
 * <p>{@code code} is always server-generated (same {@code AdmSequence} mechanism as
 * {@code ComFolder}, row {@code Code = "Prospection"} — confirmed real format
 * {@code "{année sur 2 chiffres}/{numéro sur 5 chiffres}"}, e.g. "26/05932"), never
 * client-supplied. {@code commercialId} is optional here (unlike the separate
 * {@code POST .../transformer} step) — providing it at creation immediately produces a fully
 * assigned prospection instead of requiring a second call; omit it to create an intermediate
 * state (prospect linked, no commercial yet) that a later {@code POST .../transformer} can
 * complete. Every other field mirrors {@link ModifierProspectionRequest} + "Nature"
 * ({@code etapeId}, see {@code PATCH /api/prospections/{id}/etape}) — same optional, "null = not
 * set" semantics, all validated for existence only when provided. {@code trancheId}/
 * {@code articleId} follow the same cross-validation as
 * {@code PATCH /api/prospections/{id}/tranche-principale} (article must belong to tranche).
 *
 * <p>Deliberately NOT included: closing fields ({@code fermee}/{@code raisonFermetureId}/
 * {@code raisonFermetureTexte}) — creating an already-closed prospection isn't part of the
 * "Nouvelle prospection" flow this endpoint serves; call {@code PATCH .../archiver} right after
 * if that's genuinely needed.
 */
public record CreerProspectionRequest(
        UUID prospectId,
        UUID commercialId,
        UUID trancheId,
        UUID articleId,
        UUID origineId,
        UUID origineSupportId,
        UUID vocationId,
        UUID typeContactId,
        UUID actionCommercialeId,
        UUID etapeId,
        BigDecimal surface,
        BigDecimal surfaceJusqua,
        BigDecimal prix,
        BigDecimal prixJusqua,
        BigDecimal avance,
        BigDecimal financementFondsPropres,
        BigDecimal financementCredit,
        BigDecimal financementAutre,
        Boolean ancienClient,
        String notes
) {
}
