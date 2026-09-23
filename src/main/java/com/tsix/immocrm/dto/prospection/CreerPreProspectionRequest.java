package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Corps de {@code POST /api/pre-prospections} — voir
 * {@code instructions/FEATURE-creation-pre-prospection.md} §3.
 *
 * <p>Un lead brut : le contact n'est pas un {@code CfgTier} mais trois champs libres
 * ({@code TempDescription} / {@code TempTel} / {@code TempEMail}), exactement comme l'écran
 * « Pré-prospection » d'OMRAN. Seul {@code nom} est obligatoire — OMRAN enregistre une
 * pré-prospection avec le seul nom.
 */
@Schema(description = "Création d'une pré-prospection (lead brut, non qualifié). Seul `nom` est "
        + "obligatoire ; le prospect (`CfgTier`) et le commercial ne se posent pas ici mais par "
        + "`POST /api/pre-prospections/{id}/creer-prospect` puis `POST /api/prospections/{id}/transformer`.")
public record CreerPreProspectionRequest(
        @Schema(description = "Nom du contact tel que saisi (`TempDescription`, 255 caractères max). "
                + "**Obligatoire**, non blanc. Renseigné sur 99,9 % des pré-prospections réelles.",
                example = "Karim Bennani", requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,
        @Schema(description = "Téléphone, stocké tel quel (`TempTel`, 255 max). C'est ce champ que "
                + "la hotline retrouve par numéro, quel que soit le format.", example = "0612345678")
        String telephone,
        @Schema(description = "Email (`TempEMail`, 255 max).", example = "k.bennani@example.com")
        String email,
        @Schema(description = "Notes libres (`Notes`, 1 000 caractères max).")
        String notes,
        @Schema(description = "Tranche visée (CfgTranche → `StkHierarchyId`). Optionnel ; 400 si "
                + "inconnue, **403** si son projet est hors du périmètre commercial de l'appelant.")
        UUID trancheId,
        @Schema(description = "Article/lot visé (StkItem). Optionnel ; exige `trancheId` et doit lui "
                + "appartenir (400 sinon).")
        UUID articleId,
        @Schema(description = "Nature/étape (ComProspectionKind → `ComProspectionKindId`). Optionnel.")
        UUID etapeId,
        @Schema(description = "Source de contact (ComProspectionOrigin). Optionnel.")
        UUID origineId,
        @Schema(description = "Support d'origine (ComProspectionOriginSupport). Optionnel.")
        UUID origineSupportId,
        @Schema(description = "Type (ComActionMessageType → `ComActionMessageTypeId`). Optionnel.")
        UUID typeContactId,
        @Schema(description = "Action commerciale / campagne (ComAction). Optionnel.")
        UUID actionCommercialeId,
        @Schema(description = "Vocation (StkVocation). Absente : la vocation de code `VENTE` — celle "
                + "de 100 % des pré-prospections réelles qui en portent une.")
        UUID vocationId
) {
}
