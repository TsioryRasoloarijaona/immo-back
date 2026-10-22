package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Valeur proposée d'un paramètre de rapport.")
public record OptionRapportDto(
        @Schema(description = "Valeur à envoyer (id de tranche ou de commercial, ou valeur du choix).") String valeur,
        @Schema(description = "Libellé à afficher.") String libelle) {
}
