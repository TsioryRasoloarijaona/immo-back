package com.tsix.immocrm.dto.dossier.echeancier;

import com.tsix.immocrm.domain.Periodicite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Découpage d'un montant en échéances périodiques (« Générer périodicité » d'OMRAN).")
public record GenererPeriodiqueRequest(
        @Schema(description = "Catégorie des lignes générées (`ComSettlementCategory.PKey`). Requise.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID categorieId,

        @Schema(description = "Montant à répartir, TTC ; au plus le montant initial de `GET …/echeancier/schema`.",
                example = "300000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal montantTotal,

        @Schema(description = "Nombre de périodes, au moins 1.", example = "12",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Integer nombre,

        @Schema(description = "Périodicité des échéances.", requiredMode = Schema.RequiredMode.REQUIRED)
        Periodicite periodicite,

        @Schema(description = "Date de la première échéance (enregistrée à minuit).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime premiereDate,

        @Schema(description = "Note recopiée sur chaque ligne.")
        String notes
) {
}
