package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.FiltresListeArticles;
import com.tsix.immocrm.dto.patrimoine.LigneArticleDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.patrimoine.ArticleController.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.COLONNES;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.COLONNE_INCONNUE;
import static com.tsix.immocrm.controller.patrimoine.ArticleController.REPONSE_403;
import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Sous-articles")
@RestController
@RequestMapping("/api/sous-articles")
public class SousArticleController {

    private static final String INTROUVABLE = "Sous-article introuvable, entité inactive, ou article principal.";
    private static final String ID = "Id du sous-article (StkItem.PKey).";

    private final ArticleService articleService;
    private final ExportListe export;

    public SousArticleController(ArticleService articleService, ExportListe export) {
        this.articleService = articleService;
        this.export = export;
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des sous-articles, colonnes du catalogue", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<StreamingResponseBody> exporter(
            @ParameterObject @ModelAttribute FiltresListeArticles filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "code", direction = Sort.Direction.ASC) Sort sort) {
        return ArticleController.exporter(export, articleService, filtres.versFiltres(true), demande, sort,
                "sous-articles", "Sous-articles");
    }

    @GetMapping
    @Operation(summary = "Liste des sous-articles (libres et rattachés), à colonnes choisies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de sous-articles, colonnes demandées seules."),
            @ApiResponse(responseCode = "400", description = COLONNE_INCONNUE, content = @Content)
    })
    public PageDto<LigneArticleDto> findAll(
            @ParameterObject @ModelAttribute FiltresListeArticles filtres,
            @Parameter(description = COLONNES, example = "code,description,parentCode,etat,parentOccupe")
            @RequestParam(required = false) List<String> colonnes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageDto.of(articleService.lister(filtres.versFiltres(true), ColonneArticle.resoudre(colonnes), pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un sous-article libre, ou rattaché à un article de la même tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sous-article créé ; le prix du parent éventuel est recalculé."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant, parent d'une autre tranche ou "
                    + "vocation, « toujours affecté » sans parent."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans la tranche, ou article parent occupé.")
    })
    public ArticleDetailDto creer(@RequestBody ArticleEcritureDto saisie) {
        return articleService.creerSousArticleLibre(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Modifie un sous-article libre (champ absent ou null = inchangé), ou le rattache")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sous-article modifié."),
            @ApiResponse(responseCode = "400", description = "Parent d'une autre tranche ou vocation, « toujours "
                    + "affecté » sans parent, parentId dans champsAVider."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Sous-article rattaché (lecture seule ici), parent occupé, "
                    + "code déjà pris.")
    })
    public ArticleDetailDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                     @RequestBody ArticleEcritureDto saisie) {
        return articleService.modifierSousArticle(id, saisie);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime un sous-article ; le prix de son article parent est recalculé")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Supprimé avec vues, contenances, tracés, pièces et notes."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Affecté par défaut, dossier de vente ou de location, "
                    + "encore référencé.")
    })
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        articleService.supprimerSousArticle(id);
    }
}
