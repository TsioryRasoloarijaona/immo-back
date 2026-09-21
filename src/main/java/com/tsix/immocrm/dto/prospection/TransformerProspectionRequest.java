package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Body for {@code POST /api/prospections/{id}/transformer}: achève une pré-prospection en
 * prospection en lui affectant à la fois un commercial et un projet (tranche). Le prospect
 * lui-même est créé séparément, en amont, via {@code POST /api/pre-prospections/{id}/creer-prospect}.
 *
 * <p><b>Règle métier — la tranche est obligatoire.</b> Une prospection transformée doit être
 * rattachée à une tranche (projet) : c'est ce rattachement qui la place dans le périmètre
 * commercial et sous une entité juridique. La règle est plus stricte que le légataire (qui
 * n'impose rien au niveau base — seule {@code PKey} est NOT NULL sur {@code ComProspection}), mais
 * justifiée par les données réelles : 14 700 des 14 725 prospections qualifiées portent déjà une
 * tranche (99,83 %). Le champ {@code trancheId} peut être fourni ici pour poser/remplacer la
 * tranche au moment de la transformation ; s'il est omis, la prospection doit déjà en porter une,
 * sinon la transformation est refusée (400).
 *
 * <p>NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo). Les champs
 * {@code trancheId}/{@code articleId} suivent la convention de
 * {@link ModifierTranchePrincipaleRequest}.
 */
public record TransformerProspectionRequest(
        @Schema(description = "Commercial (CfgTier) affecté à la prospection. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "5ca4a41f-35d1-4e89-a521-4e9c363ac99b")
        UUID commercialId,

        @Schema(description = "Tranche (projet) à rattacher, `ComProspection.StkHierarchyId`. "
                + "Fourni ici pour poser ou remplacer la tranche au moment de la transformation. "
                + "S'il est omis, la prospection doit **déjà** porter une tranche — sinon 400, la "
                + "tranche étant obligatoire pour transformer. Contrôle de périmètre commercial : "
                + "rattacher à un projet non affecté renvoie 403.",
                example = "5eee12f0-03ea-4008-9808-4109b6e242bf")
        UUID trancheId,

        @Schema(description = "Article/lot principal (StkItem) optionnel. S'il est fourni, il DOIT "
                + "appartenir à la tranche effective (celle de `trancheId`, ou celle déjà posée si "
                + "`trancheId` est omis) — 400 sinon. Même validation croisée que "
                + "`PATCH /api/prospections/{id}/tranche-principale`.",
                example = "null")
        UUID articleId
) {
}
