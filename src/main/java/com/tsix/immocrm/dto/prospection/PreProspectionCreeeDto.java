package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Réponse de {@code POST /api/pre-prospections} : la pré-prospection créée, et les doublons
 * éventuels. Un doublon n'empêche pas la création — décision métier, voir
 * {@code FEATURE-creation-pre-prospection.md} §3.
 */
@Schema(description = "Pré-prospection créée, accompagnée des doublons détectés. Les doublons "
        + "sont **signalés, jamais refusés** : OMRAN enregistre lui aussi une ligne par soumission.")
public record PreProspectionCreeeDto(
        @Schema(description = "La pré-prospection créée, sous la même forme que "
                + "`GET /api/prospections/{id}`.")
        ProspectionDetailDto preProspection,
        @Schema(description = "Prospections existantes au même contact : même nom (sans casse ni "
                + "espaces de bord), même téléphone (9 derniers chiffres) et même email (sans "
                + "casse) — un champ absent des deux côtés compte comme égal. Les plus récentes "
                + "d'abord, 50 au plus, et **seulement ceux que l'appelant a le droit de voir** "
                + "(périmètre commercial). **Vide = aucun doublon visible.**")
        List<DoublonPreProspectionDto> doublons
) {
}
