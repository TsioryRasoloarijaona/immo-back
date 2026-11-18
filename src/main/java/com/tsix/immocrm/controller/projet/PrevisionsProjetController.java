package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.PrevisionsProjetDto;
import com.tsix.immocrm.dto.projet.ProjetPrevisionOptionDto;
import com.tsix.immocrm.services.projet.PrevisionsProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/previsions")
public class PrevisionsProjetController {

    private static final String PROJET = "Projet (PrjProject.PKey) habilité.";
    private static final String SANS_DROIT = "Droit Yes.Prj.Front.Tdb.ProjectPrevision absent.";
    private static final String INTROUVABLE = "Projet inconnu ou non habilité (même message).";
    private static final String MAL_FORME = "projetId absent ou mal formé.";

    private final PrevisionsProjetService service;

    public PrevisionsProjetController(PrevisionsProjetService service) {
        this.service = service;
    }

    @GetMapping("/projets")
    @Operation(summary = "Projets habilités de la liste de l'écran Prévisions, triés par description")
    @ApiResponse(responseCode = "200", description = "Projets de l'habilitation du compte ; vide sans habilitation.")
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    public List<ProjetPrevisionOptionDto> projetsPrevisions() {
        return service.projets();
    }

    @GetMapping
    @Operation(summary = "Prévisions des lots d'un projet (lecture seule)")
    @ApiResponse(responseCode = "200", description = "Totaux, graphe, lots, décaissements et avancements ; zéros sans lot.")
    @ApiResponse(responseCode = "400", description = MAL_FORME, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public PrevisionsProjetDto previsionsProjet(@Parameter(description = PROJET) @RequestParam UUID projetId) {
        return service.previsions(projetId);
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx des prévisions d'un projet (8 feuilles)")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx : mêmes chiffres que la lecture.",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = MAL_FORME, content = @Content)
    @ApiResponse(responseCode = "403", description = SANS_DROIT, content = @Content)
    @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> exporterPrevisionsProjet(@Parameter(description = PROJET) @RequestParam UUID projetId) {
        return service.exporter(projetId);
    }
}
