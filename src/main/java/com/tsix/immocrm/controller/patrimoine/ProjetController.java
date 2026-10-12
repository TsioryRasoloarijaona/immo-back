package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.services.patrimoine.ProjetService;
import com.tsix.immocrm.services.patrimoine.SuppressionProjetService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Programmes", description = "Programmes immobiliers (CfgProject) : lecture, création, modification, "
        + "suppression et zones.")
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private static final String ID_PROGRAMME = "Identifiant du programme (CfgProject.PKey).";
    private static final String REFUS_ROLE = "L'appelant n'a pas le rôle Administrateur.";
    private static final String PROGRAMME_INTROUVABLE = "Aucun programme avec cet id, ou programme d'une entité inactive.";

    private final ProjetService projetService;
    private final SuppressionProjetService suppressionProjetService;

    public ProjetController(ProjetService projetService, SuppressionProjetService suppressionProjetService) {
        this.projetService = projetService;
        this.suppressionProjetService = suppressionProjetService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des programmes des entités actives")
    public PageDto<ProjetListItemDto> findAll(
            @Parameter(description = "Recherche (casse ignorée) dans le code, le libellé, l'entité et la ville.")
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findAll(search, pageable));
    }

    @GetMapping("/defauts-entite/{entiteId}")
    @Operation(summary = "Valeurs par défaut d'un programme, reprises de l'entité juridique")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Corps de création pré-rempli (à corriger avant POST)."),
            @ApiResponse(responseCode = "404", description = "Aucune entité active avec cet id.")
    })
    public ProjetEcritureDto defautsEntite(
            @Parameter(description = "Entité juridique (CfgCompany.PKey).") @PathVariable UUID entiteId) {
        return projetService.defautsEntite(entiteId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Supprime un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Programme supprimé avec ses zones et habilitations."),
            @ApiResponse(responseCode = "403", description = REFUS_ROLE),
            @ApiResponse(responseCode = "404", description = PROGRAMME_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Une tranche, un article ou une autre donnée le référence.")
    })
    public ResponseEntity<Void> delete(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        suppressionProjetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche du programme."),
            @ApiResponse(responseCode = "404", description = PROGRAMME_INTROUVABLE)
    })
    public ProjetDetailDto findById(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id) {
        return projetService.findById(id);
    }

    @GetMapping("/{id}/tranches")
    @Operation(summary = "Tranches d'un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranches du programme (éventuellement aucune)."),
            @ApiResponse(responseCode = "404", description = PROGRAMME_INTROUVABLE)
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
            @ApiResponse(responseCode = "404", description = PROGRAMME_INTROUVABLE),
            @ApiResponse(responseCode = "409", description = "Code déjà utilisé par un autre programme.")
    })
    public ProjetDetailDto update(@Parameter(description = ID_PROGRAMME) @PathVariable UUID id,
                                  @RequestBody ProjetEcritureDto demande) {
        return projetService.update(id, demande);
    }
}
