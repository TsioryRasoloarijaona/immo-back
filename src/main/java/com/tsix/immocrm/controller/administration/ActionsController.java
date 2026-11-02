package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.LigneActionDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.ActionsService;
import com.tsix.immocrm.services.administration.FiltresActions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Actions (administration)")
@RestController
@RequestMapping("/api/administration/actions")
@PreAuthorize("hasRole('Administrateur')")
public class ActionsController {

    private final ActionsService service;

    public ActionsController(ActionsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Journal des actions, page par page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, date de création décroissante, sans date en dernier."),
            @ApiResponse(responseCode = "400", description = "Date illisible, début de période après sa fin, page ou "
                    + "size hors bornes.", content = @Content),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
    })
    public PageDto<LigneActionDto> lister(
            @ParameterObject FiltresActions filtres,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000)
            @Parameter(description = "Page demandée, de 0 à 1 000 000.") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200)
            @Parameter(description = "Taille de la page, de 1 à 200.") int size) {
        return service.lister(filtres, page, size);
    }
}
