package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.services.projet.DecomptesProjetService;
import com.tsix.immocrm.services.projet.FiltresDecomptes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/decomptes")
public class DecomptesController {

    private final DecomptesProjetService service;

    public DecomptesController(DecomptesProjetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des décomptes des projets visibles")
    public PageDto<DecompteListItemDto> decomptes(
            @ParameterObject @ModelAttribute FiltresDecomptes filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = {"docDate", "code"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(service.decomptes(filtres, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un décompte")
    @ApiResponse(responseCode = "200", description = "Fiche du décompte.")
    @ApiResponse(responseCode = "404", description = "Décompte introuvable : id inconnu ou lot hors périmètre.",
            content = @Content)
    public DecompteDetailDto decompte(
            @Parameter(description = "Identifiant du décompte (PrjWorkingOut.PKey).") @PathVariable UUID id) {
        return service.decompte(id);
    }
}
