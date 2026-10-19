package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.LigneContenanceDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.ReferencesFicheArticle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Fiche article", description = "Onglet Contenances de l'article et types de présentation de sa tranche.")
@RestController
public class ArticleFicheController {

    private static final String CONTENANCES = "/api/articles/{id}/contenances";
    private static final String ID_ARTICLE = "Id de l'article (StkItem.PKey).";
    private static final String INTROUVABLE = "Article introuvable ou de son entité inactive.";

    private final ArticleService articles;
    private final ReferencesFicheArticle references;

    public ArticleFicheController(ArticleService articles, ReferencesFicheArticle references) {
        this.articles = articles;
        this.references = references;
    }

    @GetMapping(CONTENANCES)
    @Operation(summary = "Contenances de l'article (StkItemContenance), par ordre de la contenance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lignes de l'onglet."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneContenanceDto> contenances(@Parameter(description = ID_ARTICLE) @PathVariable UUID id) {
        return articles.contenances(id);
    }

    @PutMapping(CONTENANCES)
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Enregistre la liste complète des contenances de l'article")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lignes enregistrées, relues."),
            @ApiResponse(responseCode = "400", description = "Ligne invalide : le détail nomme lignes[i].<champ> ; "
                    + "rien n'est écrit."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)})
    public LignesTrancheDto<LigneContenanceDto> enregistrerContenances(
            @Parameter(description = ID_ARTICLE) @PathVariable UUID id,
            @RequestBody LignesTrancheDto<LigneContenanceDto> corps) {
        return articles.enregistrerContenances(id, corps);
    }

    @GetMapping("/api/tranches/{id}/presentations")
    @Operation(summary = "Types de présentation (StkPresentationType) de la tranche, par code")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Types de la tranche (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Tranche introuvable ou de son entité inactive.")})
    public List<ReferentielItemDto> presentations(
            @Parameter(description = "Id de la tranche (CfgTranche.PKey).") @PathVariable UUID id) {
        return references.presentations(id);
    }
}
