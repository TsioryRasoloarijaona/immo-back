package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.patrimoine.AffectationSousArticleDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.services.patrimoine.ArticlePieceJointeService;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Articles")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final String ADMINISTRATEUR = "hasRole('Administrateur')";
    private static final String REPONSE_403 = "L'appelant n'a pas le rôle Administrateur.";
    private static final String ARTICLE_INTROUVABLE = "Article introuvable.";
    private static final String ID_ARTICLE = "Id de l'article (StkItem.PKey).";
    private static final String ID_SOUS_ARTICLE = "Id du sous-article (StkItem.PKey).";

    private final ArticleService articleService;
    private final ArticlePieceJointeService pieceJointeService;

    public ArticleController(ArticleService articleService, ArticlePieceJointeService pieceJointeService) {
        this.articleService = articleService;
        this.pieceJointeService = pieceJointeService;
    }

    @GetMapping
    @Operation(summary = "Catalogue transverse des articles")
    public PageDto<ArticleListItemDto> findAll(
            @Parameter(description = "Sous-chaîne recherchée dans le code ou la description de l'article.", example = "A-204")
            @RequestParam(required = false) String search,
            @Parameter(description = "Id(s) de CfgTranche. Répétable, correspondance OR. Absent = toutes les tranches (des entités actives).")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE et OCCUPE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Id de CfgCompany (entité juridique). Absent = toutes (parmi les entités actives).")
            @RequestParam(required = false) UUID entiteId,
            @Parameter(description = "Id de StkKind (nature du bien). Absent = toutes.")
            @RequestParam(required = false) UUID natureId,
            @Parameter(description = "Id de StkVocation. Absent = toutes.")
            @RequestParam(required = false) UUID vocationId,
            @Parameter(description = "Id de StkFloor (étage). Absent = tous.")
            @RequestParam(required = false) UUID etageId,
            @Parameter(description = "Borne basse (incluse) sur le prix de vente (StkItem.ComSalePrice).")
            @RequestParam(required = false) BigDecimal prixDe,
            @Parameter(description = "Borne haute (incluse) sur le prix de vente (StkItem.ComSalePrice).")
            @RequestParam(required = false) BigDecimal prixA,
            @Parameter(description = "Borne basse (incluse) sur la superficie (StkItem.Area).")
            @RequestParam(required = false) BigDecimal surfaceDe,
            @Parameter(description = "Borne haute (incluse) sur la superficie (StkItem.Area).")
            @RequestParam(required = false) BigDecimal surfaceA,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.findAll(search, trancheId, statut, categorieId, typeId,
                entiteId, natureId, vocationId, etageId, prixDe, prixA, surfaceDe, surfaceA, pageable));
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
            @ApiResponse(responseCode = "400", description = "L'article est lui-même un sous-article."),
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

    // -- Pièces jointes (AdmAttachedFile, CategoryId = StkItem.PKey — voir ArticlePieceJointeService) --

    @GetMapping("/{id}/pieces-jointes")
    @Operation(summary = "Liste les pièces jointes de l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des pièces jointes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Article introuvable.")
    })
    public List<PieceJointeDto> getPiecesJointes(
            @Parameter(description = "Identifiant de l'article (StkItem.PKey).")
            @PathVariable UUID id) {
        return pieceJointeService.findByArticle(id);
    }

    @GetMapping("/{id}/pieces-jointes/{pieceId}/fichier")
    @Operation(summary = "Affiche ou télécharge une pièce jointe de l'article (ex. un plan)")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "404", description = "Article introuvable, pièce jointe introuvable "
                    + "pour cet article, ou aucun fichier stocké pour cette pièce.")
    })
    public ResponseEntity<Void> getFichier(
            @Parameter(description = "Identifiant de l'article (StkItem.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey).")
            @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(pieceJointeService.urlFichier(id, pieceId))
                .build();
    }
}
