package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/{id}")
    public ArticleDetailDto findById(@PathVariable UUID id) {
        return articleService.findById(id);
    }
}
