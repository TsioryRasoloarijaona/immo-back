package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.projet.DepensesProjetDto;
import com.tsix.immocrm.dto.projet.FiltresReglementsProjet;
import com.tsix.immocrm.dto.projet.ReglementProjetDto;
import com.tsix.immocrm.services.achat.DepensesProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Projet — dépenses")
@RestController
@RequestMapping("/api/projet/depenses")
public class DepensesProjetController {

    private final DepensesProjetService service;

    public DepensesProjetController(DepensesProjetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Synthèse des dépenses (factures fournisseurs) par projet visible et par lot")
    @ApiResponse(responseCode = "200", description = "Un élément par projet visible ayant au moins une facture.")
    @ApiResponse(responseCode = "400", description = "projetIds mal formé.", content = @Content)
    public List<DepensesProjetDto> depenses(
            @Parameter(description = "Projets (PrjProject.PKey), paramètre répété ; un projet hors périmètre est ignoré.")
            @RequestParam(required = false) List<UUID> projetIds) {
        return service.lireDepenses(projetIds);
    }

    @GetMapping("/reglements")
    @Operation(summary = "Affectations de règlement (décaissements) des factures des projets visibles")
    @ApiResponse(responseCode = "200", description = "Page de lignes ; vide si rien n'est visible.")
    @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date).", content = @Content)
    public PageDto<ReglementProjetDto> reglements(@ParameterObject @ModelAttribute FiltresReglementsProjet filtres,
                                                  @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(service.lireReglements(filtres, pageable));
    }
}
