package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.services.prospect.ProspectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private final ProspectService prospectService;

    public ProspectController(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    @GetMapping
    public List<ProspectListItemDto> findAll(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) String type) {
        return prospectService.findAll(search, type);
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
}
