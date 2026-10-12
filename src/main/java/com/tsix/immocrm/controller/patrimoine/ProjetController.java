package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.services.patrimoine.ProjetService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Programmes", description = "Programmes immobiliers (CfgProject) : lecture, création et modification.")
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private static final String ID_PROGRAMME = "Identifiant du programme (CfgProject.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des programmes des entités actives")
    public PageDto<ProjetListItemDto> findAll(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche du programme."),
            @ApiResponse(responseCode = "404", description = "Aucun programme avec cet id.")
    })
    public ProjetDetailDto findById(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return projetService.findById(id);
    }

    @GetMapping("/{id}/tranches")
    @Operation(summary = "Tranches d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranches du programme (éventuellement aucune)."),
            @ApiResponse(responseCode = "404", description = "Aucun programme avec cet id.")
    })
    public PageDto<TrancheListItemDto> findTranches(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findTranches(id, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Programme créé ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par un autre programme.")
    })
    public ResponseEntity<ProjetDetailDto> create(@RequestBody ProjetEcritureDto demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.create(demande));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie un programme (champs absents inchangés)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programme modifié ; renvoie sa fiche."),
            @ApiResponse(responseCode = "400", description = "Mêmes causes qu'à la création."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = "Aucun programme avec cet id."),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par un autre programme.")
    })
    public ProjetDetailDto update(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                  @RequestBody ProjetEcritureDto demande) {
        return projetService.update(id, demande);
    }
}
