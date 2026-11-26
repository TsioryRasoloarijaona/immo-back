package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.FactureGenereeDto;
import com.tsix.immocrm.services.projet.FacturationDecompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/decomptes/{id}")
@ApiResponse(responseCode = "403", description = "Droit OMRAN (AdmRight) manquant pour cette action.", content = @Content)
@ApiResponse(responseCode = "404", description = "Décompte introuvable : id inconnu ou projet non habilité.", content = @Content)
public class FacturesDecompteController {

    private static final String ID_DECOMPTE = "Identifiant du décompte (PrjWorkingOut.PKey).";

    private final FacturationDecompteService service;

    public FacturesDecompteController(FacturationDecompteService service) {
        this.service = service;
    }

    @PostMapping("/facture")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Générer la facture fournisseur d'un décompte (lignes, extras et retenues du décompte)")
    @ApiResponse(responseCode = "201", description = "Facture créée et liée au décompte.")
    @ApiResponse(responseCode = "400", description = "Lot sans ventilation par catégorie budgétaire.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Une facture est déjà générée sur ce décompte.", content = @Content)
    public FactureGenereeDto facturer(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id) {
        return service.facturer(id);
    }

    @PostMapping("/avoir")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Générer un avoir fournisseur qui reprend la facture du décompte (plusieurs avoirs possibles)")
    @ApiResponse(responseCode = "201", description = "Avoir créé ; net à payer de la facture réduit du TTC de l'avoir si non financier.")
    @ApiResponse(responseCode = "409", description = "Décompte sans facture : aucun avoir possible.", content = @Content)
    public FactureGenereeDto avoir(@Parameter(description = ID_DECOMPTE) @PathVariable UUID id) {
        return service.avoir(id);
    }
}
