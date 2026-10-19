package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.dossier.actions.ActionDossierDto;
import com.tsix.immocrm.dto.dossier.actions.CreerActionDossierRequest;
import com.tsix.immocrm.services.patrimoine.ActionsArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Actions d'article", description = "Actions et alertes (ComActionMessage) d'un article ; programme et tranche "
        + "n'en ont pas (aucune colonne).")
@RestController
@RequestMapping("/api/articles/{id}/actions")
public class ActionsArticleController {

    private static final String ARTICLE = "Id de l'article (StkItem.PKey).";
    private static final String INTROUVABLE = "Article introuvable.";

    private final ActionsArticleService actionsArticleService;

    public ActionsArticleController(ActionsArticleService actionsArticleService) {
        this.actionsArticleService = actionsArticleService;
    }

    @GetMapping
    @Operation(summary = "Actions portant sur l'article (dossiers compris), plus récentes d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste, éventuellement vide."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<ActionDossierDto> lister(@Parameter(description = ARTICLE) @PathVariable UUID id) {
        return actionsArticleService.lister(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Ajoute une action ou une alerte (date d'alerte) sur l'article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Action créée."),
            @ApiResponse(responseCode = "400", description = "typeId absent ou inconnu, note vide, type « Instruction »."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ActionDossierDto creer(@Parameter(description = ARTICLE) @PathVariable UUID id,
                                  @RequestBody CreerActionDossierRequest request) {
        return actionsArticleService.creer(id, request);
    }
}
