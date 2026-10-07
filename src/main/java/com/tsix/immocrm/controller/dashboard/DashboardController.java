package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.domain.EcheanceRelance;
import com.tsix.immocrm.domain.PorteeRelance;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dashboard.DashboardDto;
import com.tsix.immocrm.dto.dashboard.NouveauPreProspectDto;
import com.tsix.immocrm.dto.dashboard.RelanceProspectionDto;
import com.tsix.immocrm.dto.dashboard.SyntheseAnnuelleDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dashboard.DashboardService;
import com.tsix.immocrm.services.dashboard.RelanceDashboardService;
import com.tsix.immocrm.services.dashboard.SyntheseAnnuelleService;
import com.tsix.immocrm.services.tranche.FiltreTrancheAdministrateur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;


@Tag(name = "Tableau de bord")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final RelanceDashboardService relanceDashboardService;
    private final SyntheseAnnuelleService syntheseAnnuelleService;

    public DashboardController(DashboardService dashboardService,
                               RelanceDashboardService relanceDashboardService,
                               SyntheseAnnuelleService syntheseAnnuelleService) {
        this.dashboardService = dashboardService;
        this.relanceDashboardService = relanceDashboardService;
        this.syntheseAnnuelleService = syntheseAnnuelleService;
    }

    @GetMapping
    @Operation(summary = "Agrégats du tableau de bord")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "`trancheId` n'est pas un UUID."),
            @ApiResponse(responseCode = "403", description = "`trancheId` envoyé par un appelant "
                    + "qui n'a pas le rôle Administrateur.")
    })
    public DashboardDto get(
            @Parameter(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID trancheId) {
        FiltreTrancheAdministrateur.verifier(trancheId);
        return dashboardService.get(trancheId);
    }

    @GetMapping("/relances")
    @Operation(summary = "Les relances commerciales à traiter")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "`echeance` ou `portee` hors des "
                    + "valeurs admises, ou `trancheId` qui n'est pas un UUID."),
            @ApiResponse(responseCode = "403", description = "`trancheId` envoyé par un appelant "
                    + "qui n'a pas le rôle Administrateur.")
    })
    public PageDto<RelanceProspectionDto> relances(
            @Parameter(example = "AUJOURDHUI")
            @RequestParam(defaultValue = "AUJOURDHUI") EcheanceRelance echeance,

            @Parameter(example = "MOI")
            @RequestParam(defaultValue = "MOI") PorteeRelance portee,

            @Parameter(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID trancheId,

            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        FiltreTrancheAdministrateur.verifier(trancheId);
        return PageDto.of(relanceDashboardService.relances(echeance, portee, principal, trancheId, pageable));
    }

    @GetMapping("/nouveaux-pre-prospects")
    @Operation(summary = "Les leads arrivés dans la journée")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de leads du jour. Vide le plus "
                    + "souvent — ce n'est pas une erreur."),
            @ApiResponse(responseCode = "400", description = "`jour` non parsable.")
    })
    public PageDto<NouveauPreProspectDto> nouveauxPreProspects(
            @Parameter(description = "Jour observé. Défaut : aujourd'hui. Sert surtout à rattraper "
                    + "la veille après un week-end chargé.", example = "2026-09-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate jour,

            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(relanceDashboardService.nouveauxPreProspects(jour, pageable));
    }

    @GetMapping("/annuel")
    @Operation(summary = "Synthese annuelle facon logiciel de bureau")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "`annee` non numérique, ou "
                    + "`trancheId` qui n'est pas un UUID."),
            @ApiResponse(responseCode = "403", description = "`trancheId` envoyé par un appelant "
                    + "qui n'a pas le rôle Administrateur.")
    })
    public SyntheseAnnuelleDto annuel(
            @Parameter(description = "Annee observee. Defaut : l'annee courante.", example = "2026")
            @RequestParam(required = false) Integer annee,

            @Parameter(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID trancheId) {
        FiltreTrancheAdministrateur.verifier(trancheId);
        return syntheseAnnuelleService.get(annee, trancheId);
    }
}
