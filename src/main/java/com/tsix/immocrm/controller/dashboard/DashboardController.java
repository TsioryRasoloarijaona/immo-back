package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.dto.dashboard.DashboardDto;
import com.tsix.immocrm.services.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tableau de bord", description = "Agrégats de la page d'accueil : valeur du pipeline, "
        + "panier moyen, répartition des dossiers par étape, relances en retard et compteurs de "
        + "prospections.\n\n"
        + "**Entités juridiques en exploitation.** Tous les agrégats ne portent que sur les "
        + "entités juridiques actives (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. "
        + "L'assiette du tableau de bord est donc la même que celle de GET /api/dossiers et de "
        + "GET /api/prospections ; les totaux ne sont pas comparables à ceux d'avant cette "
        + "restriction.")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(
            summary = "Agrégats du tableau de bord",
            description = "Tous les chiffres sont calculés côté serveur à la volée, jamais "
                    + "stockés. `valeurPipeline` et `nombre de dossiers actifs` (qui donne le "
                    + "`panierMoyen`) excluent les dossiers en statut de sortie (annulation, "
                    + "désistement...). `relancesEnRetard` liste les dossiers dont une date de "
                    + "validité de relance est dépassée. `prospectionsOuvertes`/"
                    + "`prospectionsTotal` excluent les pré-prospections.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), *tous* les agrégats "
                    + "ci-dessus ne portent que sur ses projets affectés — un chiffre d'affaires "
                    + "global serait précisément la donnée que le périmètre masque. Les dossiers "
                    + "et prospections dont le projet ne se résout pas restent comptés pour tout "
                    + "le monde."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agrégats calculés. Des listes vides "
                    + "et des montants à zéro signifient qu'aucun dossier n'entre dans le "
                    + "périmètre de l'appelant, pas une erreur.")
    })
    public DashboardDto get() {
        return dashboardService.get();
    }
}
