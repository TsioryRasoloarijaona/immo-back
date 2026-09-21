package com.tsix.immocrm.dto.synchronisation;

import com.tsix.immocrm.config.SynchronisationDescendante.ResultatSync;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Résultat d'un déclenchement manuel de la synchronisation descendante
 * ({@code POST /api/synchronisation}). Chiffres du cycle qui vient de tourner.
 */
@Schema(description = "Bilan du cycle de synchronisation descendante qui vient de s'exécuter.")
public record SynchronisationResultatDto(

        @Schema(description = "Version Change Tracking de la source atteinte par ce cycle. "
                + "Sert de repère de diagnostic ; null si le Change Tracking n'est pas actif "
                + "sur la source (le cycle n'a alors rien pu faire).", example = "1287")
        Long version,

        @Schema(description = "Nombre de lignes insérées ou mises à jour dans la copie depuis la "
                + "source (créations et modifications faites hors CRM, typiquement par le "
                + "logiciel de bureau).", example = "12")
        int fusions,

        @Schema(description = "Nombre de lignes supprimées de la copie parce qu'elles ont "
                + "disparu de la source.", example = "1")
        int suppressions,

        @Schema(description = "Nombre de tables traitées sans erreur — leur filigrane a avancé.",
                example = "23")
        int tablesTraitees,

        @Schema(description = "Nombre de tables en échec sur ce cycle : leur filigrane est resté "
                + "inchangé et elles seront rejouées au cycle suivant. 0 en fonctionnement "
                + "nominal.", example = "0")
        int tablesEnEchec,

        @Schema(description = "Durée du cycle en millisecondes.", example = "2340")
        long dureeMs) {

    public static SynchronisationResultatDto de(ResultatSync r) {
        return new SynchronisationResultatDto(r.version(), r.fusions(), r.suppressions(),
                r.tablesTraitees(), r.tablesEnEchec(), r.dureeMs());
    }
}
