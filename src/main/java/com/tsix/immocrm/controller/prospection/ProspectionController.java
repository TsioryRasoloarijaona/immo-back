package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.services.prospection.ProspectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prospections")
public class ProspectionController {

    private final ProspectionService prospectionService;

    public ProspectionController(ProspectionService prospectionService) {
        this.prospectionService = prospectionService;
    }

    @GetMapping
    public List<ProspectionListItemDto> findAll(@RequestParam(required = false) String etape,
                                                 @RequestParam(required = false) String origine,
                                                 @RequestParam(required = false) UUID commercial,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(required = false) Boolean ouvertes) {
        return prospectionService.findAll(etape, origine, commercial, search, ouvertes);
    }

    @GetMapping("/{id}")
    public ProspectionDetailDto findById(@PathVariable UUID id) {
        return prospectionService.findById(id);
    }

    @PostMapping("/{id}/transformer")
    public ProspectionDetailDto transformer(@PathVariable UUID id, @RequestBody TransformerProspectionRequest request) {
        return prospectionService.transformer(id, request);
    }

    @PatchMapping("/{id}/etape")
    public ProspectionDetailDto changerEtape(@PathVariable UUID id, @RequestBody ChangerEtapeRequest request) {
        return prospectionService.changerEtape(id, request);
    }

    @PatchMapping("/{id}/archiver")
    public ProspectionDetailDto archiver(@PathVariable UUID id, @RequestBody ArchiverProspectionRequest request) {
        return prospectionService.archiver(id, request);
    }

    @GetMapping("/{id}/interactions")
    public List<InteractionDto> findInteractions(@PathVariable UUID id) {
        return prospectionService.findInteractions(id);
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<InteractionDto> addInteraction(@PathVariable UUID id, @RequestBody CreerInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.addInteraction(id, request));
    }
}
