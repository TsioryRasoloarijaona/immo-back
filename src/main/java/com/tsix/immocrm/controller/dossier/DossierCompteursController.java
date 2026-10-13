package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.compteurs.CompteursDto;
import com.tsix.immocrm.services.dossier.fiche.CompteursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Dossiers — compteurs", description = "Compteurs eau, électricité et gaz de l'article du dossier (bouton « Compteurs »).")
@RestController
@RequestMapping("/api/dossiers/{id}/compteurs")
public class DossierCompteursController {

    private static final String DESCRIPTION_404 = "Dossier inexistant, hors périmètre ou d'une entité inactive.";
    private static final String DESCRIPTION_409 = "Dossier sans article principal.";

    private final CompteursService compteursService;

    public DossierCompteursController(CompteursService compteursService) {
        this.compteursService = compteursService;
    }

    @GetMapping
    @Operation(summary = "Compteurs de l'article principal du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compteurs (champs null si non renseignés)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = DESCRIPTION_409)
    })
    public CompteursDto lire(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return compteursService.lire(id);
    }

    @PatchMapping
    @Operation(summary = "Modifie les compteurs : champ null = inchangé, chaîne vide = effacé")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compteurs à jour ; l'article et le dossier sont signés."),
            @ApiResponse(responseCode = "400", description = "Corps absent ou champ de plus de 50 caractères."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = DESCRIPTION_409)
    })
    public CompteursDto modifier(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @RequestBody CompteursDto request) {
        return compteursService.modifier(id, request);
    }
}
