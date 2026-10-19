package com.tsix.immocrm.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Propriété d'une ligne d'audit : valeurs telles qu'OMRAN les écrit (dates jj/MM/aaaa HH:mm:ss).")
public record ChangementAuditDto(
        @Schema(description = "Nom de colonne, ou de la navigation pour une clé étrangère (ex. CfgProject).") String propriete,
        @Schema(description = "Valeur avant ; vide pour un ajout.") String ancienneValeur,
        @Schema(description = "Valeur après.") String nouvelleValeur
) {
}
