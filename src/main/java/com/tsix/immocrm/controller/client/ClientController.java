package com.tsix.immocrm.controller.client;

import com.tsix.immocrm.dto.client.ClientListItemDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.services.client.ClientService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
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
 * A "client" is a {@code CfgTier} referenced by at least one {@code ComFolder.CfgCustomerId} —
 * same underlying table as {@code /api/prospects} (there's no separate Client entity/table in
 * the schema), so the fiche ({@code /{id}}) and its dossiers sub-resource
 * ({@code /{id}/dossiers}) reuse {@link ProspectService}/{@link ProspectSubResourcesService}
 * internally rather than duplicating logic. Only the list ({@code GET /api/clients}) has
 * client-specific shape/filtering — see {@link ClientService}.
 */
@Tag(name = "Clients", description = "Tiers ayant acheté — un `CfgTier` référencé par au moins un "
        + "dossier de vente. Il n'y a pas d'entité Client distincte dans le schéma : la fiche et "
        + "ses sous-ressources sont celles de /api/prospects.\n\n"
        + "**Entités juridiques en exploitation.** La qualité de client se juge sur les seuls "
        + "dossiers rattachés à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui "
        + "ADMOD et GHRIZA. Un tier dont tous les dossiers relèvent d'une entité hors exploitation "
        + "n'est donc plus un client au sens de cet endpoint et disparaît de la liste ; les "
        + "agrégats `nombreDossiers`, `totalAPayer` et `totalRegle` suivent la même assiette, pour "
        + "qu'ils concordent avec GET /api/clients/{id}/dossiers.")
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

    @Operation(
            summary = "Liste des clients",
            description = "Les tiers portant au moins un dossier de vente sur une entité juridique "
                    + "en exploitation, avec leurs agrégats calculés côté serveur (nombre de "
                    + "dossiers, total à payer, total réglé). Une page vide signifie qu'aucun "
                    + "client ne correspond, pas une erreur.")
    @GetMapping
    public PageDto<ClientListItemDto> findAll(
            @Parameter(description = "Recherche insensible à la casse sur le code, le nom, le "
                    + "prénom, la raison sociale ou l'e-mail.", example = "benali")
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(clientService.findAll(search, pageable));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @GetMapping("/{id}/dossiers")
    @Operation(
            summary = "Dossiers de vente d'un client",
            description = "Les dossiers dont ce tier est l'acquéreur — mêmes `DossierListItemDto` "
                    + "que GET /api/dossiers.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), seuls les dossiers de "
                    + "ses projets affectés sont renvoyés. Le **client**, lui, n'est pas filtré : "
                    + "sa fiche reste accessible, la liste peut simplement revenir vide ou "
                    + "partielle. `totalElements` reflète le nombre filtré, pas le total réel."
    )
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
