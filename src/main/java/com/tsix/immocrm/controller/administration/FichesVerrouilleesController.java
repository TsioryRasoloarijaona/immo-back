package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.dto.administration.LigneFicheVerrouilleeDto;
import com.tsix.immocrm.dto.administration.ResultatDeverrouillageDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.administration.FichesVerrouilleesService;
import com.tsix.immocrm.services.administration.FiltresFichesVerrouillees;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Fiches verrouillées (administration)")
@RestController
@RequestMapping("/api/administration/fiches-verrouillees")
@PreAuthorize("hasRole('Administrateur')")
public class FichesVerrouilleesController {

    private static final String PAS_ADMIN = "L'appelant n'a pas le rôle Administrateur.";

    private final FichesVerrouilleesService service;

    public FichesVerrouilleesController(FichesVerrouilleesService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Fiches verrouillées par OMRAN, page par page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, date de verrouillage décroissante puis PKey."),
            @ApiResponse(responseCode = "400", description = "État ou date illisible, dateDebut après dateFin, page ou "
                    + "size hors bornes.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public PageDto<LigneFicheVerrouilleeDto> lister(
            @ParameterObject FiltresFichesVerrouillees filtres,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000)
            @Parameter(description = "Page demandée, de 0 à 1 000 000.") int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200)
            @Parameter(description = "Taille de la page, de 1 à 200.") int size) {
        return service.lister(filtres, page, size);
    }

    @PostMapping("/{id}/deverrouiller")
    @Operation(summary = "Déverrouiller une fiche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche déverrouillée, ligne à jour."),
            @ApiResponse(responseCode = "400", description = "Identifiant illisible.", content = @Content),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content),
            @ApiResponse(responseCode = "404", description = "Aucun verrou avec cet id.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Fiche déjà déverrouillée : rien n'est écrit.", content = @Content)
    })
    public LigneFicheVerrouilleeDto deverrouiller(
            @Parameter(description = "Identifiant du verrou (AdmWebLockEntities.PKey).") @PathVariable UUID id) {
        return service.deverrouiller(id);
    }

    @PostMapping("/deverrouiller-tout")
    @Operation(summary = "Déverrouiller toutes les fiches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de fiches déverrouillées, 0 si aucun verrou n'était actif."),
            @ApiResponse(responseCode = "403", description = PAS_ADMIN, content = @Content)
    })
    public ResultatDeverrouillageDto deverrouillerTout() {
        return service.deverrouillerTout();
    }
}
