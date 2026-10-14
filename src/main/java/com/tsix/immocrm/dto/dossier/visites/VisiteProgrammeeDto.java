package com.tsix.immocrm.dto.dossier.visites;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Visite technique programmée, avec le sort du mail au responsable S.A.V.")
public record VisiteProgrammeeDto(
        VisiteTechniqueDto visite,
        @Schema(description = "Vrai si le mail est parti ; faux sans email du responsable ou si l'envoi est inactif.")
        boolean emailEnvoye
) {
}
