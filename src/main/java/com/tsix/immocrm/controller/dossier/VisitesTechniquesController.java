package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.visites.CalendrierVisitesDto;
import com.tsix.immocrm.dto.dossier.visites.ProgrammerVisiteRequest;
import com.tsix.immocrm.dto.dossier.visites.VisiteProgrammeeDto;
import com.tsix.immocrm.services.dossier.fiche.VisitesTechniquesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Dossiers — visites techniques")
@RestController
@RequestMapping("/api/dossiers/{id}/visites-techniques")
public class VisitesTechniquesController {

    private final VisitesTechniquesService visitesService;

    public VisitesTechniquesController(VisitesTechniquesService visitesService) {
        this.visitesService = visitesService;
    }

    @GetMapping
    @Operation(summary = "Créneaux de visite technique déjà pris sur la tranche du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calendrier de la tranche et ses règles."),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant, hors périmètre ou d'une entité inactive."),
            @ApiResponse(responseCode = "409", description = "Dossier sans article rattaché à une tranche.")
    })
    public CalendrierVisitesDto calendrier(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Début de période (inclus) ; absent = aujourd'hui 00:00.", example = "2026-10-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime du,
            @Parameter(description = "Fin de période (exclue) ; absente = début + 60 jours.", example = "2026-12-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime au) {
        return visitesService.calendrier(id, du, au);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Programme une visite technique pour le dossier et prévient le S.A.V.")
    @ApiResponses({
            @ApiResponse(responseCode = "201"),
            @ApiResponse(responseCode = "400", description = "debut ou fin absent, ou fin non postérieure au début."),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant, hors périmètre ou d'une entité inactive."),
            @ApiResponse(responseCode = "409")
    })
    public VisiteProgrammeeDto programmer(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @RequestBody ProgrammerVisiteRequest request) {
        return visitesService.programmer(id, request);
    }
}
