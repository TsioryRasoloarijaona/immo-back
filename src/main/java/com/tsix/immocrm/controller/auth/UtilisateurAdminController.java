package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.FiltresUtilisateurs;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseRequest;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.dto.auth.UtilisateurListItemDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.perimetre.AffecterProjetsRequest;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.services.auth.UtilisateurAdminService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Utilisateurs (admin)")
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurAdminController {

    private final UtilisateurAdminService utilisateurAdminService;
    private final PerimetreCommercialService perimetreService;

    public UtilisateurAdminController(UtilisateurAdminService utilisateurAdminService,
                                       PerimetreCommercialService perimetreService) {
        this.utilisateurAdminService = utilisateurAdminService;
        this.perimetreService = perimetreService;
    }

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Liste les comptes utilisateurs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste (éventuellement vide) des comptes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.")
    })
    public PageDto<UtilisateurListItemDto> findAll(
            @ParameterObject @ModelAttribute FiltresUtilisateurs filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "description", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PageDto.of(utilisateurAdminService.findAll(filtres, pageable));
    }

    @PostMapping("/{admUserId}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Réinitialise le mot de passe Arpent d'un compte Omran")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Mot de passe généré ; email déclenché si un destinataire existe et que le service d'envoi est actif."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(
            @Parameter(description = "Id de l'AdmUser à réinitialiser.") @PathVariable UUID admUserId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Facultatif. `email` : adresse d'envoi pour cette fois seulement.")
            @RequestBody(required = false) ReinitialiserMotDePasseRequest request) {
        return utilisateurAdminService.reinitialiserMotDePasse(admUserId,
                request == null ? null : request.email());
    }

    // -- Périmètre commercial : affectation de projets (voir instructions/FEATURE-perimetre-commercial.md) --

    @GetMapping("/{admUserId}/projets")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Projets affectés à un utilisateur (périmètre commercial)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projets affectés (éventuellement vide)."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public List<ProjetPerimetreDto> getProjets(
            @Parameter(description = "Id de l'AdmUser dont on consulte le périmètre.")
            @PathVariable UUID admUserId) {
        return perimetreService.projetsAffectes(admUserId);
    }

    @PutMapping("/{admUserId}/projets")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Remplace les projets affectés à un utilisateur (périmètre commercial)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Périmètre remplacé — renvoie le nouvel état."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public List<ProjetPerimetreDto> remplacerProjets(
            @Parameter(description = "Id de l'AdmUser dont on redéfinit le périmètre.")
            @PathVariable UUID admUserId,
            @RequestBody AffecterProjetsRequest request) {
        return perimetreService.remplacerProjets(admUserId, request.projetIds());
    }
}
