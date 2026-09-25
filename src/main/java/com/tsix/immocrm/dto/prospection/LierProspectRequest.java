package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Corps de {@code POST /api/pre-prospections/{id}/lier-prospect} : rattache un prospect
 * <b>existant</b> à une pré-prospection, sans rien créer — le pendant de
 * {@code POST .../creer-prospect} pour le cas « c'est bien cette personne » de
 * {@code instructions/FEATURE-doublons-prospect-projet.md} §2.
 */
@Schema(description = "Rattachement d'un prospect existant à une pré-prospection (réponse « c'est "
        + "l'une de ces personnes » à la liste de doublons).")
public record LierProspectRequest(
        @Schema(description = "Fiche existante (CfgTier) de type prospect ou client — typiquement "
                + "un `id` renvoyé par `GET /api/prospects/doublons` ou par le 409 de "
                + "`creer-prospect`. Obligatoire ; 400 si inconnue ou d'un autre type de tiers.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID prospectId
) {
}
