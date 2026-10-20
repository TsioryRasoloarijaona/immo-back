package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.ColonneArticle;
import com.tsix.immocrm.dto.referentiel.ColonneDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@Tag(name = "Référentiels")
@RestController
public class ColonnesArticleController {

    @GetMapping("/api/referentiels/colonnes/articles")
    @Operation(summary = "Catalogue des colonnes des listes Articles et Sous-articles (POURCENTAGE = fraction)")
    public List<ColonneDto> getColonnesArticles() {
        return Stream.of(ColonneArticle.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(), c.parDefaut(), null))
                .toList();
    }
}
