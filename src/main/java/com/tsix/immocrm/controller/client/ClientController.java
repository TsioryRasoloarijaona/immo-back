package com.tsix.immocrm.controller.client;

import com.tsix.immocrm.dto.client.ClientListItemDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.services.client.ClientService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.services.tranche.FiltreTrancheAdministrateur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

/**
 * Pas de table Client : la fiche et les dossiers d'un client réutilisent les services prospect ;
 * seule la liste a sa logique propre ({@link ClientService}).
 */
@Tag(name = "Clients")
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final ProspectService prospectService;
    private final ProspectSubResourcesService subResourcesService;

    public ClientController(ClientService clientService, ProspectService prospectService,
                             ProspectSubResourcesService subResourcesService) {
        this.clientService = clientService;
        this.prospectService = prospectService;
        this.subResourcesService = subResourcesService;
    }

    @Operation(summary = "Liste des clients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de clients (éventuellement "
                    + "vide). Une tranche inconnue ou sans dossier donne une page vide, pas un 404."),
            @ApiResponse(responseCode = "400", description = "`trancheId` n'est pas un UUID."),
            @ApiResponse(responseCode = "403", description = "`trancheId` envoyé par un appelant "
                    + "qui n'a pas le rôle Administrateur.")
    })
    @GetMapping
    public PageDto<ClientListItemDto> findAll(
            @Parameter(description = "Recherche insensible à la casse sur le code, le nom, le "
                    + "prénom, la raison sociale ou l'e-mail.", example = "benali")
            @RequestParam(required = false) String search,
            @Parameter(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID trancheId,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        FiltreTrancheAdministrateur.verifier(trancheId);
        return PageDto.of(clientService.findAll(search, trancheId, pageable));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @GetMapping("/{id}/dossiers")
    @Operation(summary = "Dossiers de vente d'un client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers du client (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun tier avec cet id.")
    })
    public PageDto<DossierListItemDto> getDossiers(
            @Parameter(description = "Identifiant du client (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.dossiers(id, pageable));
    }
}
