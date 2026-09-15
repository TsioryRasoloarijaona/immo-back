package com.tsix.immocrm.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un lead arrivé dans la journée, servi par {@code GET /api/dashboard/nouveaux-pre-prospects}.
 *
 * <p>Une pré-prospection se définit par {@code CfgProspectId IS NULL AND CfgCommercialId IS NULL}
 * — le même discriminant que {@code GET /api/pre-prospections}. Elle n'a donc <b>jamais</b> de
 * commercial : c'est structurel, pas une donnée manquante.
 */
@Schema(description = "Un lead non encore qualifié, créé aujourd'hui. Les arrivées se font par "
        + "rafales (injection automatique depuis le serveur legacy) et **le week-end n'est pas "
        + "creux** — 34 leads un dimanche en base réelle. Une journée à zéro est le cas courant.")
public record NouveauPreProspectDto(

        @Schema(description = "Pré-prospection créée. La route de détail est celle des "
                + "prospections : `GET /api/prospections/{id}`.")
        UUID prospectionId,

        @Schema(description = "Code légataire de la pré-prospection, s'il a été attribué.")
        String code,

        @Schema(description = "Nom ou raison sociale saisis au premier contact "
                + "(`TempDescription`). `null` tant que le lead est anonyme.")
        String nom,

        @Schema(description = "Numéro saisi au premier contact (`TempTel`) — sur une "
                + "pré-prospection c'est la seule source possible, aucune fiche tiers n'existe "
                + "encore.")
        String telephone,

        @Schema(description = "E-mail saisi au premier contact (`TempEMail`).")
        String email,

        @Schema(description = "Horodatage de création, qui sert aussi de critère du jour.")
        LocalDateTime dateCreation
) {
}
