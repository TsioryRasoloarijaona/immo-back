package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Tranches", description = "Tranches (CfgTranche) — phases d'un projet immobilier, "
        + "leurs conditions commerciales et leurs articles.")
@RestController
@RequestMapping("/api/tranches")
public class TrancheController {

    private final TrancheService trancheService;
    private final ArticleService articleService;

    public TrancheController(TrancheService trancheService, ArticleService articleService) {
        this.trancheService = trancheService;
        this.articleService = articleService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des tranches, à plat",
            description = "Toutes les tranches, tous projets confondus, ou filtrées sur un seul "
                    + "projet via `projetId`. Évite le contournement N+1 (GET /api/projets puis "
                    + "GET /api/projets/{id}/tranches pour chacun) pour peupler une checklist de "
                    + "filtre côté front — voir instructions/filtres-avances.md point 2."
    )
    public PageDto<TrancheListItemDto> findAll(
            @Parameter(description = "Id du projet (Entité, CfgProject.id) pour ne renvoyer que "
                    + "ses tranches. Absent = toutes les tranches.")
            @RequestParam(required = false) UUID projetId,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(trancheService.findAll(projetId, pageable));
    }

    @GetMapping("/{id}")
    public TrancheDetailDto findById(@PathVariable UUID id) {
        return trancheService.findById(id);
    }

    @GetMapping("/{id}/articles")
    public PageDto<ArticleListItemDto> findArticles(@PathVariable UUID id,
            @RequestParam(required = false) StatutArticle statut,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.findByTranche(id, statut, pageable));
    }
}
