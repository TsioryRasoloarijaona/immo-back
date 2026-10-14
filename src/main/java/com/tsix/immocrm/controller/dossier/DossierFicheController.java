package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.fiche.DossierFicheDto;
import com.tsix.immocrm.services.dossier.fiche.DossierFicheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — fiche")
@RestController
@RequestMapping("/api/dossiers/{id}/fiche")
public class DossierFicheController {

    private final DossierFicheService ficheService;

    public DossierFicheController(DossierFicheService ficheService) {
        this.ficheService = ficheService;
    }

    @GetMapping
    @Operation(summary = "Fiche complète du dossier en consultation, par onglet OMRAN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche du dossier."),
            @ApiResponse(responseCode = "404", description = "Dossier inexistant, hors périmètre ou d'une entité inactive.")
    })
    public DossierFicheDto fiche(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return ficheService.fiche(id);
    }
}
