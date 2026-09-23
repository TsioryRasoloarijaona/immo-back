package com.tsix.immocrm.controller.kpi;

import com.tsix.immocrm.dto.kpi.PerformanceCommercialDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.kpi.PerformanceCommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/kpi/commerciaux")
@Tag(name = "KPI commerciaux",
        description = """
                Indicateurs de performance des commerciaux sur une période — activité réalisée \
                (appels, visites, interactions), portefeuille confié (prospections, réactivité) et \
                résultat (dossiers, montants, taux de transformation).

                Deux attributions cohabitent et il faut le savoir pour lire les chiffres : \
                l'**activité** est comptée pour celui qui l'a saisie, le **portefeuille** pour le \
                commercial assigné. Un chef qui appelle sur la prospection d'un commercial voit \
                l'appel chez lui, pas chez l'autre.

                Tous les compteurs respectent la visibilité par entité juridique et le périmètre \
                commercial de l'appelant. Un commercial restreint verra donc des chiffres \
                inférieurs à son activité réelle — c'est volontaire.""")
public class PerformanceCommercialController {

    private final PerformanceCommercialService service;

    public PerformanceCommercialController(PerformanceCommercialService service) {
        this.service = service;
    }

    @GetMapping("/moi")
    @Operation(
            summary = "Mes performances commerciales",
            description = "Les indicateurs du commercial rattaché au compte connecté, sur la "
                    + "période demandée — ou ceux du **compte lui-même** s'il n'est rattaché à aucun "
                    + "employé (`commercialId` nul, `utilisateurId` renseigné, pas de portefeuille).\n\n"
                    + "**L'activité compte toutes les actions**, détaillées dans `actionsParType`, "
                    + "actions « Système » exclues ; l'auteur suit la règle du suivi des actions.\n\n"
                    + "**Renvoie 200 avec tous les compteurs à zéro** quand rien n'a été fait sur "
                    + "la période — pas une erreur. Le 404 est réservé à un appel sans compte "
                    + "applicatif identifiable.\n\n"
                    + "Les taux valent `null`, jamais `0`, quand leur dénominateur est nul : "
                    + "« aucun appel passé » et « des appels, aucun rendez-vous » sont deux "
                    + "situations opposées. Afficher « — » plutôt que « 0 % »."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Performances du commercial connecté."),
            @ApiResponse(responseCode = "400", description = "`du` postérieure à `au`, ou date non parsable."),
            @ApiResponse(responseCode = "404", description = "Aucun compte applicatif identifiable dans le jeton.")
    })
    public PerformanceCommercialDto moi(
            @Parameter(description = "Premier jour pris en compte, inclus. Défaut : il y a 30 jours.",
                    example = "2026-08-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Dernier jour pris en compte, **inclus jusqu'à 23:59:59**. Défaut : aujourd'hui.",
                    example = "2026-09-08")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return service.findMoi(principal, du, au);
    }

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Performances de tous les commerciaux",
            description = "Classement des commerciaux sur la période, les plus performants "
                    + "d'abord (dossiers créés décroissant, puis appels passés).\n\n"
                    + "**Seuls les commerciaux ayant au moins une ligne sur la période "
                    + "apparaissent** — une interaction, une prospection assignée ou un dossier. "
                    + "Ne pas supposer que cette liste couvre tous les commerciaux de "
                    + "`GET /api/commerciaux` : sur la base réelle, 60 des 75 comptes sont "
                    + "désactivés et une vingtaine de personnes seulement travaillent sur une "
                    + "période donnée.\n\n"
                    + "**Lire chaque taux avec son effectif.** Sur une période courte le "
                    + "dénominateur des taux de transformation est faible et le pourcentage très "
                    + "bruyant ; `dossiersCrees` et `prospectionsAssignees` sont là pour ça."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classement, éventuellement vide si personne n'a rien fait."),
            @ApiResponse(responseCode = "400", description = "`du` postérieure à `au`, ou date non parsable."),
            @ApiResponse(responseCode = "403", description = "Rôle `Administrateur` requis.")
    })
    public List<PerformanceCommercialDto> tous(
            @Parameter(description = "Premier jour pris en compte, inclus. Défaut : il y a 30 jours.",
                    example = "2026-08-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Dernier jour pris en compte, **inclus jusqu'à 23:59:59**. Défaut : aujourd'hui.",
                    example = "2026-09-08")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return service.findAll(du, au);
    }
}
