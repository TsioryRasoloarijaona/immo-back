package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import com.tsix.immocrm.services.patrimoine.ActionArticleService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Actions d'un article")
@RestController
@RequestMapping("/api/articles/{id}/actions")
public class ActionArticleController {

    private static final String ID_ARTICLE = "Identifiant de l'article (StkItem.PKey).";

    private final ActionArticleService service;

    public ActionArticleController(ActionArticleService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Actions et alertes d'un article, plus récentes d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'actions (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun article avec cet id.")
    })
    public PageDto<SuiviActionDto> lister(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = {"docDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageDto.of(service.lister(id, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Ajoute une action ou une alerte à un article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Action créée ; renvoie sa ligne."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun article avec cet id.")
    })
    public ResponseEntity<SuiviActionDto> creer(@Parameter(description = ID_ARTICLE) @PathVariable UUID id,
                                                @RequestBody CreerInteractionRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(id, demande));
    }
}
