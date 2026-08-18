package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tranches")
public class TrancheController {

    private final TrancheService trancheService;
    private final ArticleService articleService;

    public TrancheController(TrancheService trancheService, ArticleService articleService) {
        this.trancheService = trancheService;
        this.articleService = articleService;
    }

    @GetMapping("/{id}")
    public TrancheDetailDto findById(@PathVariable UUID id) {
        return trancheService.findById(id);
    }

    @GetMapping("/{id}/articles")
    public List<ArticleListItemDto> findArticles(@PathVariable UUID id,
                                                  @RequestParam(required = false) StatutArticle statut) {
        return articleService.findByTranche(id, statut);
    }
}
