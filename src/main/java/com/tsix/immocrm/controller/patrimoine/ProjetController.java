package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ProjetDetailDto;
import com.tsix.immocrm.dto.patrimoine.ProjetListItemDto;
import com.tsix.immocrm.dto.patrimoine.TrancheListItemDto;
import com.tsix.immocrm.services.patrimoine.ProjetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @GetMapping
    public List<ProjetListItemDto> findAll() {
        return projetService.findAll();
    }

    @GetMapping("/{id}")
    public ProjetDetailDto findById(@PathVariable UUID id) {
        return projetService.findById(id);
    }

    @GetMapping("/{id}/tranches")
    public List<TrancheListItemDto> findTranches(@PathVariable UUID id) {
        return projetService.findTranches(id);
    }
}
