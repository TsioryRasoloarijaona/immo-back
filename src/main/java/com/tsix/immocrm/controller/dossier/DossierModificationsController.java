package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.modifications.ModificationRequest;
import com.tsix.immocrm.dto.dossier.modifications.ModificationsDto;
import com.tsix.immocrm.services.dossier.modifications.ModificationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — modifications")
@RestController
@RequestMapping("/api/dossiers/{id}/modifications")
public class DossierModificationsController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String MODIFICATION = "Identifiant de la demande (ComModification.PKey).";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";
    private static final String DESCRIPTION_404_LIGNE = "Dossier ou demande introuvable (ou d'un autre dossier).";
    private static final String DESCRIPTION_200 = "Demandes et montants du dossier à jour.";

    private final ModificationsService service;

    public DossierModificationsController(ModificationsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Demandes de modification du dossier et montants qui en découlent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public ModificationsDto lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.lister(id);
    }

    @PostMapping
    @Operation(summary = "Ajoute une demande de modification")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public ResponseEntity<ModificationsDto> ajouter(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                                    @RequestBody ModificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(id, request));
    }

    @PutMapping("/{modificationId}")
    @Operation(summary = "Modifie une demande de modification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404_LIGNE)
    })
    public ModificationsDto modifier(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                     @Parameter(description = MODIFICATION) @PathVariable UUID modificationId,
                                     @RequestBody ModificationRequest request) {
        return service.modifier(id, modificationId, request);
    }

    @DeleteMapping("/{modificationId}")
    @Operation(summary = "Supprime une demande de modification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = DESCRIPTION_200),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404_LIGNE)
    })
    public ModificationsDto supprimer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                      @Parameter(description = MODIFICATION) @PathVariable UUID modificationId) {
        return service.supprimer(id, modificationId);
    }
}
