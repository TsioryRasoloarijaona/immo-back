package com.tsix.immocrm.dto.kpi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = """
        Performance d'un commercial sur une période. Trois familles d'indicateurs : l'activité \
        réalisée (appels, visites, interactions), le portefeuille confié (prospections, dossiers, \
        réactivité) et le résultat (montants, taux de transformation).

        Deux attributions différentes cohabitent volontairement, et il faut le savoir pour lire les \
        chiffres. L'activité est comptée pour **celui qui l'a faite** : un chef qui appelle sur la \
        prospection d'un commercial voit l'appel chez lui. Le portefeuille est compté pour **le \
        commercial assigné**, qu'il ait ou non passé les appels lui-même. Les deux coïncident dans \
        86 % des cas réels ; l'écart est exactement ce qu'on veut voir.

        Tous les compteurs respectent la visibilité par entité juridique et le périmètre commercial \
        de l'appelant : un commercial restreint verra des chiffres inférieurs à son activité réelle.""")
public record PerformanceCommercialDto(

        @Schema(description = "Identifiant du commercial (CfgTier.PKey), et non celui de son compte "
                + "utilisateur — un même employé peut porter deux comptes, dont les chiffres sont "
                + "additionnés ici.")
        UUID commercialId,

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

        @Schema(description = "Visites **décrochées** : appels dont le résultat est « Le prospect "
                + "veut faire une visite du projet ». C'est un engagement obtenu, pas une visite "
                + "réalisée.", example = "41")
        long visitesDecrochees,

        @Schema(description = "Appels dont le résultat est « Rendez vous pris ».", example = "18")
        long rdvObtenus,

        @Schema(description = "Toutes interactions confondues sur la période, y compris notes et messages.",
                example = "384")
        long interactionsTotal,

        @Schema(description = "Prospections **distinctes** sur lesquelles il a saisi au moins une "
                + "interaction. Mesure le travail réellement effectué, indépendamment de qui la "
                + "prospection est assignée.", example = "97")
        long prospectionsTraitees,

        // ------------------------------------------------------------------ portefeuille confié

        @Schema(description = "Prospections créées sur la période et assignées à ce commercial "
                + "(`ComProspection.CfgCommercialId`). Les pré-prospections, qui n'ont par "
                + "construction aucun commercial, n'y figurent jamais.", example = "140")
        long prospectionsAssignees,

        @Schema(description = "Parmi elles, celles clôturées (`IsClosed`), quel qu'en soit le motif — "
                + "vente conclue comme abandon.", example = "62")
        long prospectionsCloturees,

        @Schema(description = "Parmi les prospections assignées, celles ayant reçu au moins une "
                + "interaction — de lui ou de quelqu'un d'autre. Le complément est le nombre de "
                + "leads confiés puis laissés sans contact : 259 sur 4 555 en données réelles "
                + "(5,7 %). À ne pas confondre avec les 32 % de leads jamais contactés de la base "
                + "entière, dont l'essentiel n'est assigné à personne et n'entre donc dans le "
                + "compteur d'aucun commercial.", example = "104")
        long prospectionsPrisesEnCharge,

        @Schema(description = "`prospectionsPrisesEnCharge / prospectionsAssignees`, entre 0 et 1. "
                + "**`null` si aucune prospection assignée** — jamais 0, qui se lirait comme un "
                + "échec là où il n'y a rien à mesurer.", example = "0.743")
        BigDecimal tauxPriseEnCharge,

        @Schema(description = "Médiane du délai entre la création d'une prospection assignée et sa "
                + "première interaction, en heures. Médiane et non moyenne : quelques leads repris "
                + "des mois plus tard suffisent à rendre une moyenne illisible (109 h de moyenne "
                + "sur la base). `null` si aucune prospection prise en charge.", example = "6")
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

        @Schema(description = "`dossiersCrees / prospectionsAssignees`, entre 0 et 1. `null` si aucune "
                + "prospection assignée. **À lire avec son effectif** : sur une période courte le "
                + "dénominateur est faible et le taux très bruyant.", example = "0.021")
        BigDecimal tauxProspectionVersDossier
) {
}
