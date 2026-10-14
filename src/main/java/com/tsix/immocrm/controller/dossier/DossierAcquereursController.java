package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.acquereurs.AcquereursDto;
import com.tsix.immocrm.dto.dossier.acquereurs.ArchiverProspectionsRequest;
import com.tsix.immocrm.dto.dossier.acquereurs.RepartitionAcquereursRequest;
import com.tsix.immocrm.services.dossier.acquereurs.AcquereursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — acquéreurs")
@RestController
@RequestMapping("/api/dossiers/{id}/acquereurs")
public class DossierAcquereursController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";

    private final AcquereursService service;

    public DossierAcquereursController(AcquereursService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Client principal et co-acquéreurs, avec leur répartition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acquéreurs, principal en tête."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public AcquereursDto lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.lister(id);
    }

    @PutMapping
    @Operation(summary = "Enregistre la répartition des acquéreurs (remplace les co-acquéreurs)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acquéreurs à jour et prospections à proposer à l'archivage."),
            @ApiResponse(responseCode = "400", description = "Part du principal ≤ 0, Σ ≠ 100 %, tiers principal, en double ou introuvable."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public AcquereursDto enregistrer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                     @RequestBody RepartitionAcquereursRequest request) {
        return service.enregistrer(id, request);
    }

    @PostMapping("/archiver-prospections")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive les prospections ouvertes cochées des acquéreurs")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prospections fermées (« Création dossier de vente »)."),
            @ApiResponse(responseCode = "400", description = "Une prospection n'est pas une prospection ouverte d'un acquéreur du dossier."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public void archiver(@Parameter(description = DOSSIER) @PathVariable UUID id,
                         @RequestBody ArchiverProspectionsRequest request) {
        service.archiverProspections(id, request.prospectionIds());
    }
}
