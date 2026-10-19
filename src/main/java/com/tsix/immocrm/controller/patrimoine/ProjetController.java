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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Programmes", description = "Programmes immobiliers (CfgProject), en lecture et en écriture (Administrateur).")
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

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
    @Operation(summary = "Détail d'un programme")
    @ApiResponse(responseCode = "404", description = "Programme introuvable.")
    public ProjetDetailDto findById(@Parameter(description = "Id du programme (CfgProject.PKey).") @PathVariable UUID id) {
        return projetService.findById(id);
    }

    @GetMapping("/{id}/tranches")
    @Operation(summary = "Tranches d'un programme")
    @ApiResponse(responseCode = "404", description = "Programme introuvable.")
    public PageDto<TrancheListItemDto> findTranches(
            @Parameter(description = "Id du programme (CfgProject.PKey).") @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(projetService.findTranches(id, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Crée un programme")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Programme créé."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire manquant, entité ou pays inconnu, dates incohérentes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre programme.")
    })
    public ProjetDetailDto creer(@RequestBody ProjetEcritureDto saisie) {
        return projetService.creerProjet(saisie);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Modifie un programme (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Programme modifié."),
            @ApiResponse(responseCode = "400", description = "Champ vidé obligatoire, entité ou pays inconnu, dates incohérentes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Programme introuvable."),
            @ApiResponse(responseCode = "409", description = "Code déjà pris par un autre programme.")
    })
    public ProjetDetailDto modifier(@Parameter(description = "Id du programme (CfgProject.PKey).") @PathVariable UUID id,
                                    @RequestBody ProjetEcritureDto saisie) {
        return projetService.modifierProjet(id, saisie);
    }
}
