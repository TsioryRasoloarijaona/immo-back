package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.patrimoine.AffectationSousArticleDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.SousArticleEcritureDto;
import com.tsix.immocrm.services.patrimoine.ArticleEcritureService;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Articles")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final String ROLE_ADMINISTRATEUR = "hasRole('Administrateur')";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String ID_ARTICLE = "Identifiant de l'article (StkItem.PKey).";
    private static final String ID_SOUS_ARTICLE = "Identifiant du sous-article (StkItem.PKey).";
    private static final String AUCUN_ARTICLE = "Aucun article avec cet id.";

    private final ArticleService articleService;
    private final ArticleEcritureService ecritureService;
    private final ArticlePieceJointeService pieceJointeService;

    public ArticleController(ArticleService articleService, ArticleEcritureService ecritureService,
                             ArticlePieceJointeService pieceJointeService) {
        this.articleService = articleService;
        this.ecritureService = ecritureService;
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

    // -- Écritures (Administrateur) : FEATURE-patrimoine-ecriture.md §5 --

    @PostMapping
    @PreAuthorize(ROLE_ADMINISTRATEUR)
    @Operation(summary = "Crée un article principal")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Article créé ; renvoie sa fiche, prix calculés."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé dans la tranche.")
    })
    public ResponseEntity<ArticleDetailDto> create(@RequestBody ArticleEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ecritureService.create(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ROLE_ADMINISTRATEUR)
    @Operation(summary = "Modifie un article ou un sous-article (champs absents inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article modifié ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = AUCUN_ARTICLE),
            @ApiResponse(responseCode = "409")
    })
    public ArticleDetailDto update(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                   @RequestBody ArticleEcritureDto demande) {
        return ecritureService.update(id, demande);
    }

    @PostMapping("/{id}/sous-articles")
    @PreAuthorize(ROLE_ADMINISTRATEUR)
    @Operation(summary = "Crée un sous-article rattaché à l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sous-article créé ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = AUCUN_ARTICLE),
            @ApiResponse(responseCode = "409", description = "Code déjà pris dans la tranche, ou article affecté à un dossier.")
    })
    public ResponseEntity<ArticleDetailDto> createSousArticle(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                                              @RequestBody SousArticleEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ecritureService.createSousArticle(id, demande));
    }

    @PostMapping("/{id}/sous-articles/{sousArticleId}/affecter")
    @PreAuthorize(ROLE_ADMINISTRATEUR)
    @Operation(summary = "Rattache un sous-article libre de la tranche à l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rattaché ; renvoie la fiche de l'article, prix de vente recalculé."),
            @ApiResponse(responseCode = "400", description = "L'article cible est lui-même un sous-article."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Article ou sous-article introuvable."),
            @ApiResponse(responseCode = "409")
    })
    public ArticleDetailDto affecter(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                     @Parameter(description = ID_SOUS_ARTICLE) @PathVariable UUID sousArticleId,
                                     @RequestBody(required = false) AffectationSousArticleDto demande) {
        return ecritureService.affecter(id, sousArticleId, demande);
    }

    @PostMapping("/{id}/sous-articles/{sousArticleId}/desaffecter")
    @PreAuthorize(ROLE_ADMINISTRATEUR)
    @Operation(summary = "Détache un sous-article de l'article (il redevient libre)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détaché ; renvoie la fiche de l'article, prix de vente recalculé."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Article ou sous-article introuvable."),
            @ApiResponse(responseCode = "409")
    })
    public ArticleDetailDto desaffecter(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                        @Parameter(description = ID_SOUS_ARTICLE) @PathVariable UUID sousArticleId) {
        return ecritureService.desaffecter(id, sousArticleId);
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
