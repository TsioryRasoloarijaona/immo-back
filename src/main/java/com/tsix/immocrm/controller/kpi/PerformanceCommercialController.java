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
@Tag(name = "KPI commerciaux")
public class PerformanceCommercialController {

    private final PerformanceCommercialService service;

    public PerformanceCommercialController(PerformanceCommercialService service) {
        this.service = service;
    }

    @GetMapping("/moi")
    @Operation(summary = "Mes performances commerciales")
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
    @Operation(summary = "Performances de tous les commerciaux")
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
