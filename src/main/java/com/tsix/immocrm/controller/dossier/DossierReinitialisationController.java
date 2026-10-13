package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.reinitialisation.ReinitialisationDto;
import com.tsix.immocrm.services.dossier.fiche.ReinitialisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — réinitialisation")
@RestController
@RequestMapping("/api/dossiers/{id}/reinitialiser")
@PreAuthorize("hasRole('Administrateur')")
public class DossierReinitialisationController {

    private static final String DESCRIPTION_403 = "Réservé au rôle Administrateur.";
    private static final String DESCRIPTION_404 = "Dossier inexistant, hors périmètre ou d'une entité inactive.";
    private static final String DESCRIPTION_409 = "Dossier annulé ou désisté, ou sans article principal.";

    private final ReinitialisationService reinitialisationService;

    public DossierReinitialisationController(ReinitialisationService reinitialisationService) {
        this.reinitialisationService = reinitialisationService;
    }

    @GetMapping("/apercu")
    @Operation(summary = "Aperçu de la réinitialisation depuis le stock (avant / après), sans rien écrire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "État actuel et état après réinitialisation."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = DESCRIPTION_409)
    })
    public ReinitialisationDto apercu(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return reinitialisationService.apercu(id);
    }

    @PostMapping
    @Operation(summary = "Réinitialise superficie, TVA et prix HT depuis le stock, puis recalcule la remise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réinitialisation appliquée ; le dossier est signé."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = DESCRIPTION_409)
    })
    public ReinitialisationDto reinitialiser(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return reinitialisationService.appliquerReinitialisation(id);
    }
}
