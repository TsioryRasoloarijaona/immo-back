package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.PrevisionsChantierDto;
import com.tsix.immocrm.dto.projet.PrevisionsChantierRequest;
import com.tsix.immocrm.services.projet.PrevisionsChantierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Projet — configuration, prévisions chantier")
@RestController
@RequestMapping("/api/projet/config/projets/{id}/previsions-chantier")
@PreAuthorize("hasRole('Administrateur')")
public class PrevisionsChantierController {

    private static final String ID_PROJET = "Id du projet (PrjProject.PKey).";
    private static final String REFUS = "Rôle Administrateur ou droit OMRAN ProjectList.BuildingPrevision absent.";
    private static final String INTROUVABLE = "Projet introuvable.";

    private final PrevisionsChantierService service;

    public PrevisionsChantierController(PrevisionsChantierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Grille des prévisions chantier du projet et types de travaux")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = REFUS),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public PrevisionsChantierDto lire(@Parameter(description = ID_PROJET) @PathVariable UUID id) {
        return service.lire(id);
    }

    @PutMapping
    @Operation(summary = "Remplace la grille des prévisions chantier ; réponse = grille relue")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Type absent, inconnu ou en double ; valeur négative ou trop grande."),
            @ApiResponse(responseCode = "403", description = REFUS),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Le projet n'a pas de nivellement.")
    })
    public PrevisionsChantierDto modifier(@Parameter(description = ID_PROJET) @PathVariable UUID id,
                                          @RequestBody PrevisionsChantierRequest saisie) {
        return service.modifier(id, saisie);
    }
}
