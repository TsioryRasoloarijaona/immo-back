package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private final ProspectService prospectService;
    private final ProspectSubResourcesService subResourcesService;

    public ProspectController(ProspectService prospectService, ProspectSubResourcesService subResourcesService) {
        this.prospectService = prospectService;
        this.subResourcesService = subResourcesService;
    }

    @GetMapping
    public PageDto<ProspectListItemDto> findAll(@RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectService.findAll(search, type, pageable));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProspectDetailDto> create(@RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectService.create(request));
    }

    @PatchMapping("/{id}")
    public ProspectDetailDto update(@PathVariable UUID id, @RequestBody ModifierProspectRequest request) {
        return prospectService.update(id, request);
    }

    // -- GAP-06: prospect fiche sub-resources --

    @GetMapping("/{id}/prospections")
    public PageDto<ProspectionListItemDto> getProspections(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.prospections(id, pageable));
    }

    @GetMapping("/{id}/dossiers")
    public PageDto<DossierListItemDto> getDossiers(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.dossiers(id, pageable));
    }

    @GetMapping("/{id}/reglements")
    public PageDto<ReglementProspectDto> getReglements(@PathVariable UUID id,
                                                         @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.reglements(id, pageable));
    }

    @GetMapping("/{id}/historique")
    public PageDto<HistoriqueDto> getHistorique(@PathVariable UUID id,
                                                 @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.historique(id, pageable));
    }

    @GetMapping("/{id}/contacts")
    public List<ContactProspectDto> getContacts(@PathVariable UUID id) {
        return subResourcesService.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    public ContactProspectDto ajouterContact(@PathVariable UUID id, @RequestBody CreerContactProspectRequest request) {
        return subResourcesService.ajouterContact(id, request);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<Void> retirerContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        subResourcesService.retirerContact(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
