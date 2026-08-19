package com.tsix.immocrm.controller.client;

import com.tsix.immocrm.dto.client.ClientListItemDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.services.client.ClientService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
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

    @GetMapping
    public PageDto<ClientListItemDto> findAll(@RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(clientService.findAll(search, pageable));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @GetMapping("/{id}/dossiers")
    public PageDto<DossierListItemDto> getDossiers(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.dossiers(id, pageable));
    }
}
