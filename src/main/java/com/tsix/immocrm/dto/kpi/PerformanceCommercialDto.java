package com.tsix.immocrm.dto.kpi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PerformanceCommercialDto(

        UUID commercialId,

        UUID utilisateurId,

        @Schema(description = "Nom affiché : `Description` du tier, à défaut « prénom nom », à défaut son code.",
                example = "Faycal SAOUD")
        String commercialNom,

        @Schema(description = "Premier jour de la période prise en compte, inclus.", example = "2026-08-09")
        LocalDate du,

        @Schema(description = "Dernier jour de la période, inclus jusqu'à 23:59:59.", example = "2026-09-08")
        LocalDate au,

        // ------------------------------------------------------------------ activité réalisée

        @Schema(description = "Appels sortants saisis par ce commercial (`ComActionMessage` de type "
                + "« Téléphone Sortant »).", example = "312")
        long appelsPasses,

        @Schema(description = "Appels entrants qu'il a consignés.", example = "9")
        long appelsRecus,

        @Schema(description = "Visites effectivement consignées (type « Visite ») — à ne pas "
                + "confondre avec `visitesDecrochees`.", example = "24")
        long visitesRealisees,

        @Schema(example = "41")
        long visitesDecrochees,

        @Schema(description = "Appels dont le résultat est « Rendez vous pris ».", example = "18")
        long rdvObtenus,

        @Schema(example = "384")
        long interactionsTotal,

        List<ActionParTypeDto> actionsParType,

        @Schema(example = "97")
        long prospectionsTraitees,

        // ------------------------------------------------------------------ portefeuille confié

        @Schema(example = "140")
        long prospectionsAssignees,

        @Schema(description = "Parmi elles, celles clôturées (`IsClosed`), quel qu'en soit le motif — "
                + "vente conclue comme abandon.", example = "62")
        long prospectionsCloturees,

        @Schema(example = "104")
        long prospectionsPrisesEnCharge,

        @Schema(example = "0.743")
        BigDecimal tauxPriseEnCharge,

        @Schema(example = "6")
        Long delaiMedianPremiereReponseHeures,

        // ------------------------------------------------------------------ résultat

        @Schema(description = "Dossiers créés sur la période et assignés à ce commercial.", example = "3")
        long dossiersCrees,

        @Schema(description = "Somme des `TotalToPay` de ces dossiers, en dirhams.", example = "2450000.00")
        BigDecimal montantTotal,

        @Schema(description = "`montantTotal / dossiersCrees`. `null` si aucun dossier.", example = "816666.67")
        BigDecimal panierMoyen,

        @Schema(description = "`rdvObtenus / appelsPasses`, entre 0 et 1 — l'efficacité au téléphone. "
                + "`null` si aucun appel passé.", example = "0.058")
        BigDecimal tauxAppelVersRdv,

        @Schema(example = "0.047")
        BigDecimal tauxActionVersRdv,

        @Schema(example = "0.021")
        BigDecimal tauxProspectionVersDossier
) {
}
