package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.domain.DateRenseignee;
import com.tsix.immocrm.domain.PeriodeSur;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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

        // Repli date-heure : l'ancien format reste accepté, son heure est ignorée.
        @Schema(description = "Dernier échange à partir de ce jour (inclus), AAAA-MM-JJ. 90,4 %.",
                example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE, fallbackPatterns = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDate communicationDepuis,

        @Schema(description = "Dernier échange jusqu'à ce jour inclus (jour entier), AAAA-MM-JJ.",
                example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE, fallbackPatterns = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDate communicationJusqua,

        Boolean avecAlerteActive,

        // ---------------- Barre de filtres d'OMRAN ----------------

        Boolean aboutie,

        @Schema(description = "Date portant la période `dateDebut` / `dateFin`. Défaut : DATE.")
        PeriodeSur periodeSur,

        DateRenseignee dateRenseignee
) {

    /** Vrai si au moins un filtre exige la sous-requête sur la vue. */
    public boolean exigeLaVue() {
        return inactiviteMin != null || nbInteractionsMin != null || nbVisitesMin != null
                || communicationDepuis != null || communicationJusqua != null
                || avecAlerteActive != null;
    }
}
