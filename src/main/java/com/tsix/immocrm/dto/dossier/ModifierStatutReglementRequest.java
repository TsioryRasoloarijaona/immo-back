package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Body de {@code PATCH /api/dossiers/{id}/reglements/{reglementId}/statut}.
 *
 * <p>Un règlement naît toujours en {@code IN_WORK} ; c'est ce PATCH qui le fait évoluer. Deux
 * règles y sont appliquées côté serveur, pas seulement documentées :
 * <ul>
 *   <li>passer à {@code POINTED} exige <b>au moins une pièce jointe</b> sur le règlement (la
 *       preuve de paiement) — 409 sinon ;</li>
 *   <li>passer à {@code POINTED} exige {@code dateReglement} — 400 sinon. Statut et date
 *       d'encaissement sont deux encodages du même fait et n'ont jamais divergé sur les 3 244
 *       règlements réels ; l'API ne doit pas introduire la première incohérence.</li>
 * </ul>
 */
@Schema(description = "Changement de statut d'un règlement.")
public record ModifierStatutReglementRequest(

        @Schema(description = "Statut cible. Valeurs acceptées : `POINTED` (encaissement constaté) "
                + "ou `CANCELED` (règlement annulé). Depuis `IN_WORK` uniquement — les deux états "
                + "sont terminaux, donc toute transition partant de `POINTED` ou `CANCELED` "
                + "renvoie 409.",
                example = "POINTED",
                allowableValues = {"POINTED", "CANCELED"})
        String statut,

        @Schema(description = "Date d'encaissement effectif (`StlSettlement.SettlementDate`). "
                + "**Requise** pour `POINTED`, ignorée pour `CANCELED` (qui laisse la colonne "
                + "à `null`, conformément aux données réelles).",
                example = "2026-09-01T10:00:00")
        LocalDateTime dateReglement
) {
}
