package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

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
 * complete.
 *
 * <p><b>Champs obligatoires (alignés sur l'écran légataire).</b> OMRAN refuse d'enregistrer une
 * prospection sans ces cinq valeurs : {@code notes} (Notes), {@code origineId} (Source de
 * contact), {@code typeContactId} (Type), {@code etapeId} (Nature) et {@code trancheId} (Tranche).
 * Absents ⟶ 400 avec un message nommant le champ. Les autres champs restent optionnels et validés
 * pour existence uniquement quand fournis. {@code trancheId}/{@code articleId} suivent la même
 * validation croisée que {@code PATCH /api/prospections/{id}/tranche-principale} (l'article doit
 * appartenir à la tranche).
 *
 * <p>Deliberately NOT included: closing fields ({@code fermee}/{@code raisonFermetureId}/
 * {@code raisonFermetureTexte}) — creating an already-closed prospection isn't part of the
 * "Nouvelle prospection" flow this endpoint serves; call {@code PATCH .../archiver} right after
 * if that's genuinely needed.
 */
public record CreerProspectionRequest(
        @Schema(description = "Prospect (CfgTier) déjà existant à rattacher. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID prospectId,
        @Schema(description = "Commercial (CfgTier) affecté. Optionnel : fourni ici pour une "
                + "prospection immédiatement complète, sinon posé plus tard par "
                + "`POST /api/prospections/{id}/transformer`.")
        UUID commercialId,
        @Schema(description = "Tranche/projet (CfgTranche → `StkHierarchyId`), champ « Tranche ». "
                + "**Obligatoire** (aligné sur l'écran légataire).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID trancheId,
        @Schema(description = "Article/lot principal (StkItem). Optionnel ; s'il est fourni il DOIT "
                + "appartenir à `trancheId` (400 sinon).")
        UUID articleId,
        @Schema(description = "Source de contact (ComProspectionOrigin → `ComProspectionOriginId`), "
                + "champ « Source de contact ». **Obligatoire**.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID origineId,
        @Schema(description = "Support d'origine (ComProspectionOriginSupport). Optionnel.")
        UUID origineSupportId,
        @Schema(description = "Vocation (StkVocation), p.ex. VENTE. Optionnel — à ne pas confondre "
                + "avec le champ « Type » qui est `typeContactId`.")
        UUID vocationId,
        @Schema(description = "Type (ComActionMessageType → `ComActionMessageTypeId`), champ "
                + "« Type ». **Obligatoire**. À distinguer de la vocation (`vocationId`).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeContactId,
        @Schema(description = "Action commerciale (ComAction). Optionnel.")
        UUID actionCommercialeId,
        @Schema(description = "Nature/étape (ComProspectionKind → `ComProspectionKindId`), champ "
                + "« Nature ». **Obligatoire**.",
                requiredMode = Schema.RequiredMode.REQUIRED)
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
        @Schema(description = "Notes libres, champ « Notes ». **Obligatoire** (non vide) — aligné "
                + "sur l'écran légataire.", requiredMode = Schema.RequiredMode.REQUIRED)
        String notes
) {
}
