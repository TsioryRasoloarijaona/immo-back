package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filtres avancés de {@code GET /api/prospections}, liés depuis la chaîne de requête ; {@code null}
 * = pas de filtre. Les filtres « vue » coûtent une sous-requête, payée seulement si l'un est actif.
 * Voir {@code instructions/INVENTAIRE-colonnes-prospection.md} §3.
 */
public record FiltresAvancesProspection(

        // ---------------- Portés par l'entité — coût nul ----------------

        List<UUID> projetId,

        @Schema(description = "Id(s) de société / entité juridique (`CfgCompany`). Répétable. "
                + "99,8 %.")
        List<UUID> societeId,

        @Schema(description = "Id(s) de vocation (`StkVocation`). Répétable. 100 %.")
        List<UUID> vocationId,

        List<UUID> typeContactId,

        @Schema(description = "Id(s) de motif de clôture. Répétable. Renseigné sur 60,5 %.")
        List<UUID> raisonFermetureId,

        @Schema(description = "Id(s) de pays du prospect. Répétable. 27,3 %.")
        List<UUID> paysId,

        @Schema(description = "Id(s) d'action commerciale (campagne). Répétable. 40,2 %.")
        List<UUID> actionId,

        @Schema(description = "Id(s) de support d'origine. Répétable. 50,2 %.")
        List<UUID> origineSupportId,

        @Schema(description = "Filtre sur « le prospect est un ancien client ». 100 % renseigné.")
        Boolean ancienClient,

        @Schema(example = "ComPreProspection")
        List<String> typeProspection,

        // ---------------- Portés par la vue — sous-requête ----------------

        @Schema(example = "90")
        Integer inactiviteMin,

        @Schema(description = "Au moins N interactions journalisées. 97 %.", example = "3")
        Integer nbInteractionsMin,

        @Schema(example = "1")
        Integer nbVisitesMin,

        @Schema(description = "Dernier échange à partir de cette date (incluse), ISO-8601. 90,4 %.",
                example = "2026-01-01T00:00:00")
        LocalDateTime communicationDepuis,

        @Schema(description = "Dernier échange jusqu'à cette date (incluse), ISO-8601.",
                example = "2026-12-31T23:59:59")
        LocalDateTime communicationJusqua,

        Boolean avecAlerteActive
) {

    /** Vrai si au moins un filtre exige la sous-requête sur la vue. */
    public boolean exigeLaVue() {
        return inactiviteMin != null || nbInteractionsMin != null || nbVisitesMin != null
                || communicationDepuis != null || communicationJusqua != null
                || avecAlerteActive != null;
    }
}
