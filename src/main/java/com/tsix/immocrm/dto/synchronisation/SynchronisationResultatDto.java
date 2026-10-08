package com.tsix.immocrm.dto.synchronisation;

import com.tsix.immocrm.config.SynchronisationDescendante.ResultatSync;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Bilan du cycle de synchronisation descendante qui vient de s'exécuter.")
public record SynchronisationResultatDto(

        @Schema(example = "1287")
        Long version,

        @Schema(example = "12")
        int fusions,

        @Schema(description = "Nombre de lignes supprimées de la copie parce qu'elles ont "
                + "disparu de la source.", example = "1")
        int suppressions,

        @Schema(description = "Nombre de tables traitées sans erreur — leur filigrane a avancé.",
                example = "23")
        int tablesTraitees,

        @Schema(example = "0")
        int tablesEnEchec,

        @Schema(description = "Durée du cycle en millisecondes.", example = "2340")
        long dureeMs) {

    public static SynchronisationResultatDto de(ResultatSync r) {
        return new SynchronisationResultatDto(r.version(), r.fusions(), r.suppressions(),
                r.tablesTraitees(), r.tablesEnEchec(), r.dureeMs());
    }
}
