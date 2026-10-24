package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.AffectationSousArticleDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.FiltresListeArticles;
import com.tsix.immocrm.dto.patrimoine.LigneArticleDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.FiltresArticle;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf;
import com.tsix.immocrm.services.patrimoine.fiche.FichesPatrimoine;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.util.Set;
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

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Articles")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    static final String ADMINISTRATEUR = "hasRole('Administrateur')";
    static final String REPONSE_403 = "L'appelant n'a pas le rôle Administrateur.";
    static final String COLONNES = "Colonnes du catalogue /api/referentiels/colonnes/articles ; absent = colonnes par défaut.";
    static final String COLONNE_INCONNUE = "Colonne inconnue : le corps liste les noms valides.";
    private static final String ARTICLE_INTROUVABLE = "Article introuvable.";
    private static final String ID_ARTICLE = "Id de l'article (StkItem.PKey).";
    private static final String ID_SOUS_ARTICLE = "Id du sous-article (StkItem.PKey).";

    private final ArticleService articleService;
    private final TrancheService trancheService;
    private final ExportListe export;
    private final FichePdf fichePdf;

    public ArticleController(ArticleService articleService, TrancheService trancheService, ExportListe export,
                             FichePdf fichePdf) {
        this.articleService = articleService;
        this.trancheService = trancheService;
        this.export = export;
        this.fichePdf = fichePdf;
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf du catalogue des articles, colonnes du catalogue", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<StreamingResponseBody> exporter(
            @ParameterObject @ModelAttribute FiltresListeArticles filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        return exporter(export, articleService, filtres.versFiltres(false), demande, sort, "articles", "Articles");
    }

    /** Export partagé avec {@code /api/sous-articles/export} : même lecture que la liste, filtres déjà traduits. */
    static ResponseEntity<StreamingResponseBody> exporter(ExportListe export, ArticleService articles, FiltresArticle filtres,
                                                          DemandeExport demande, Sort sort, String liste, String titre) {
        Set<ColonneArticle> selection = ColonneArticle.resoudre(demande.colonnes());
        return export.reponse(demande, liste, titre, LignesExport.ordre(demande.colonnes(), selection),
                () -> LignesExport.toutes(sort, page -> articles.lister(filtres, selection, page)));
    }

    @GetMapping("/{id}/fiche.pdf")
    @Operation(summary = "Fiche PDF d'un article ou sous-article (inline), visibilité du détail")
    @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> fichePdf(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        ArticleDetailDto article = articleService.findById(id);
        return fichePdf.reponse(FichesPatrimoine.article(article, trancheService.findById(article.trancheId()).entiteId()));
    }

    @GetMapping
    @Operation(summary = "Catalogue transverse des articles, à colonnes choisies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'articles, colonnes demandées seules."),
            @ApiResponse(responseCode = "400", description = COLONNE_INCONNUE, content = @Content)
    })
    public PageDto<LigneArticleDto> findAll(
            @ParameterObject @ModelAttribute FiltresListeArticles filtres,
            @Parameter(description = COLONNES, example = "code,description,parentCode,etat")
            @RequestParam(required = false) List<String> colonnes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.lister(filtres.versFiltres(false), ColonneArticle.resoudre(colonnes), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un article ou sous-article")
    public ArticleDetailDto findById(@PathVariable UUID id) {
        return articleService.findById(id);
    }

    @GetMapping("/{id}/sous-articles")
    @Operation(summary = "Sous-articles disponibles (ou occupés) d'un article")
    public PageDto<ArticleListItemDto> findSousArticles(
            @PathVariable UUID id,
            @Parameter(description = "Filtre sur la disponibilité du sous-article. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.findSousArticles(id, statut, categorieId, typeId, pageable));
    }

    // -- Écriture (Administrateur, FEATURE-patrimoine-ecriture.md §5) --

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un article principal (prix calculés comme OMRAN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Article créé."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant, référence inconnue ou hors filtre, valeur invalide."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans la tranche.")
    })
    public ArticleDetailDto creer(@RequestBody ArticleEcritureDto saisie) {
        return articleService.creerArticle(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Modifie un article ou un sous-article (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article modifié."),
            @ApiResponse(responseCode = "400", description = "Changement de tranche, valeur invalide."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, ou champ protégé d'un article occupé.")
    })
    public ArticleDetailDto modifier(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                     @RequestBody ArticleEcritureDto saisie) {
        return articleService.modifierArticle(id, saisie);
    }

    @PostMapping("/{id}/sous-articles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Crée un sous-article rattaché à l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sous-article créé ; le prix de vente de l'article est recalculé."),
            @ApiResponse(responseCode = "400", description = "Article non principal, tranche dans le corps, valeur invalide."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Article occupé, ou code déjà pris dans la tranche.")
    })
    public ArticleDetailDto creerSousArticle(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                             @RequestBody ArticleEcritureDto saisie) {
        return articleService.creerSousArticle(id, saisie);
    }

    @PostMapping("/{id}/sous-articles/{sousArticleId}/affecter")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Rattache un sous-article libre de la tranche à l'article ; renvoie l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rattaché ; prix de vente de l'article recalculé."),
            @ApiResponse(responseCode = "400", description = "L'article est un sous-article, ou d'une autre vocation."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Sous-article non libre, ou article occupé.")
    })
    public ArticleDetailDto affecter(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                     @Parameter(description = ID_SOUS_ARTICLE) @PathVariable UUID sousArticleId,
                                     @RequestBody(required = false) AffectationSousArticleDto corps) {
        return articleService.affecterSousArticle(id, sousArticleId,
                corps != null && Boolean.TRUE.equals(corps.toujoursAffecte()));
    }

    @PostMapping("/{id}/sous-articles/{sousArticleId}/desaffecter")
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Détache un sous-article de l'article ; renvoie l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détaché ; prix de vente de l'article recalculé."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Sous-article non rattaché ou occupé, ou article occupé.")
    })
    public ArticleDetailDto desaffecter(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                        @Parameter(description = ID_SOUS_ARTICLE) @PathVariable UUID sousArticleId) {
        return articleService.desaffecterSousArticle(id, sousArticleId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMINISTRATEUR)
    @Operation(summary = "Supprime un article, ses sous-articles rattachés et leurs lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Supprimé avec vues, contenances, tracés, pièces et notes."),
            @ApiResponse(responseCode = "403", description = REPONSE_403),
            @ApiResponse(responseCode = "404", description = ARTICLE_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Article ou sous-article occupé, encore référencé ou porteur de dépenses.")
    })
    public void supprimer(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        articleService.supprimer(id);
    }
}
