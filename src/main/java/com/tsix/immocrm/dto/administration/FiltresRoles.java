package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.Parameter;

public record FiltresRoles(
        @Parameter(description = "Texte cherché dans le code et la description (casse ignorée).") String texte) {
}
