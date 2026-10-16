package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.sousarticles.AjouterSousArticleRequest;
import com.tsix.immocrm.dto.dossier.sousarticles.SousArticleDisponibleDto;
import com.tsix.immocrm.dto.dossier.sousarticles.SousArticlesDto;
import com.tsix.immocrm.services.dossier.sousarticles.SousArticlesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dossiers — sous-articles")
@RestController
@RequestMapping("/api/dossiers/{id}/sous-articles")
public class DossierSousArticlesController {

    private static final String DOSSIER = "Identifiant du dossier (ComFolder.PKey).";
    private static final String DESCRIPTION_403 = "Réservé au rôle Administrateur.";
    private static final String DESCRIPTION_404 = "Dossier inexistant ou hors périmètre.";

    private final SousArticlesService service;

    public DossierSousArticlesController(SousArticlesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Sous-articles du dossier et prix qui en dépendent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste (peut être vide)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public SousArticlesDto lister(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.lister(id);
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Sous-articles libres ajoutables (même tranche, même vocation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste triée par code (peut être vide)."),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404)
    })
    public List<SousArticleDisponibleDto> disponibles(@Parameter(description = DOSSIER) @PathVariable UUID id) {
        return service.disponibles(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Ajoute un sous-article et recalcule le taux de remise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sous-articles et prix à jour."),
            @ApiResponse(responseCode = "400", description = "`articleId` absent ou introuvable."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = DESCRIPTION_404),
            @ApiResponse(responseCode = "409", description = "Article non libre ou hors tranche/vocation, ou dossier sorti.")
    })
    public SousArticlesDto ajouter(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                   @RequestBody AjouterSousArticleRequest request) {
        return service.ajouter(id, request.articleId());
    }

    @DeleteMapping("/{ligneId}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Retire un sous-article et recalcule le taux de remise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sous-articles et prix à jour ; l'article est libéré."),
            @ApiResponse(responseCode = "403", description = DESCRIPTION_403),
            @ApiResponse(responseCode = "404", description = "Dossier ou sous-article introuvable."),
            @ApiResponse(responseCode = "409", description = "Sous-article affecté par défaut, ou dossier sorti.")
    })
    public SousArticlesDto retirer(@Parameter(description = DOSSIER) @PathVariable UUID id,
                                   @Parameter(description = "Ligne du dossier (ComFolderItem.PKey).")
                                   @PathVariable UUID ligneId) {
        return service.retirer(id, ligneId);
    }
}
