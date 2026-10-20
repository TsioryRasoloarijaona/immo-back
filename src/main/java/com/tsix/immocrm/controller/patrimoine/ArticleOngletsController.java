package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleDossierDto;
import com.tsix.immocrm.dto.patrimoine.ArticleHistoriqueDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.patrimoine.ArticleOngletsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Articles — onglets", description = "Onglets de lecture de la fiche article : prospections, dossiers, historique.")
@RestController
@RequestMapping("/api/articles/{id}")
public class ArticleOngletsController {

    private static final String ID_ARTICLE = "Id de l'article (StkItem.PKey).";
    private static final String INTROUVABLE = "Article inconnu, ou de l'entité juridique inactive.";

    private final ArticleOngletsService service;

    public ArticleOngletsController(ArticleOngletsService service) {
        this.service = service;
    }

    @GetMapping("/prospections")
    @Operation(summary = "Prospections portant l'article (pré-prospections exclues)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste, la plus récente d'abord (peut être vide)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<ProspectionListItemDto> prospections(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        return service.prospections(id);
    }

    @GetMapping("/dossiers")
    @Operation(summary = "Dossiers de vente, de location et de syndic de l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventes, puis locations, puis syndics (peut être vide)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<ArticleDossierDto> dossiers(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        return service.dossiers(id);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique des statuts des dossiers de l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes, la plus récente d'abord (peut être vide)."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<ArticleHistoriqueDto> historique(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        return service.historique(id);
    }
}
