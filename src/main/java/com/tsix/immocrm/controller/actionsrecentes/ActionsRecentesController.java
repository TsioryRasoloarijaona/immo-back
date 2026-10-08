package com.tsix.immocrm.controller.actionsrecentes;

import com.tsix.immocrm.domain.PorteeActionsRecentes;
import com.tsix.immocrm.dto.actionsrecentes.ActionRecenteDto;
import com.tsix.immocrm.services.actionsrecentes.ActionsRecentesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Actions récentes")
@RestController
@RequestMapping("/api/actions-recentes")
public class ActionsRecentesController {

    private final ActionsRecentesService service;

    public ActionsRecentesController(ActionsRecentesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Dernières actions de l'utilisateur ou de son équipe, de la plus récente à la plus ancienne")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actions (liste vide possible)."),
            @ApiResponse(responseCode = "400", description = "`portee` inconnue, `limite` hors 1–100, `avant`/`avantId` mal formés, `avantId` sans `avant`."),
            @ApiResponse(responseCode = "401", description = "Non authentifié.")
    })
    public List<ActionRecenteDto> lister(
            @Parameter(description = "`MOI` (mes actions) ou `EQUIPE` (mon périmètre).", example = "MOI")
            @RequestParam(defaultValue = "MOI") PorteeActionsRecentes portee,
            @Parameter(description = "Nombre maximal d'actions, de 1 à 100.", example = "20")
            @RequestParam(defaultValue = "20") int limite,
            @Parameter(example = "2026-10-08T10:42:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime avant,
            @Parameter(example = "3f2a7c1e-9b4d-4e8a-a1c2-5d6e7f809a1b")
            @RequestParam(required = false) UUID avantId) {
        return service.lister(portee, limite, avant, avantId);
    }
}
