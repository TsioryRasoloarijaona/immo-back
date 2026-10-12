package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.TrancheService;
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

@Tag(name = "Tranches", description = "Tranches (CfgTranche) — phases d'un projet immobilier, "
        + "leurs conditions commerciales et leurs articles.")
@RestController
@RequestMapping("/api/tranches")
public class TrancheController {

    private static final String ID_TRANCHE = "Identifiant de la tranche (CfgTranche.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";

    private final TrancheService trancheService;
    private final ArticleService articleService;

    public TrancheController(TrancheService trancheService, ArticleService articleService) {
        this.trancheService = trancheService;
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des tranches, à plat")
    public PageDto<TrancheListItemDto> findAll(
            @Parameter(description = "Id du projet (Entité, CfgProject.id) pour ne renvoyer que "
                    + "ses tranches. Absent = toutes les tranches.")
            @RequestParam(required = false) UUID projetId,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(trancheService.findAll(projetId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'une tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche de la tranche."),
            @ApiResponse(responseCode = "404", description = "Aucune tranche avec cet id.")
    })
    public TrancheDetailDto findById(@Parameter(description = ID_TRANCHE) @PathVariable UUID id) {
        return trancheService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée une tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tranche créée ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une autre tranche, tous programmes confondus.")
    })
    public ResponseEntity<TrancheDetailDto> create(@RequestBody TrancheEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trancheService.create(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie une tranche (champs absents inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranche modifiée ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Aucune tranche avec cet id."),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par une autre tranche, tous programmes confondus.")
    })
    public TrancheDetailDto update(@Parameter(description = ID_TRANCHE) @PathVariable UUID id,
                                   @RequestBody TrancheEcritureDto demande) {
        return trancheService.update(id, demande);
    }

    @GetMapping("/{id}/sous-articles-libres")
    @Operation(summary = "Sous-articles libres de la tranche, à affecter à un article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sous-articles libres (éventuellement aucun)."),
            @ApiResponse(responseCode = "404", description = "Aucune tranche avec cet id.")
    })
    public PageDto<ArticleListItemDto> findSousArticlesLibres(
            @Parameter(description = ID_TRANCHE) @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable) {
        return PageDto.of(articleService.findSousArticlesLibres(id, pageable));
    }

    @GetMapping("/{id}/articles")
    @Operation(summary = "Articles et sous-articles d'une tranche")
    public PageDto<ArticleListItemDto> findArticles(
            @PathVariable UUID id,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Id de CfgCompany (entité juridique). Absent = toutes.")
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
        return PageDto.of(articleService.findByTranche(id, statut, categorieId, typeId,
                entiteId, natureId, vocationId, etageId, prixDe, prixA, surfaceDe, surfaceA, pageable));
    }
}
