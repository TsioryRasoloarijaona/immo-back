package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.dossier.EcheancierService;
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
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierService dossierService;
    private final EcheancierService echeancierService;

    public DossierController(DossierService dossierService, EcheancierService echeancierService) {
        this.dossierService = dossierService;
        this.echeancierService = echeancierService;
    }

    @GetMapping
    public List<DossierListItemDto> findAll(@RequestParam(required = false) String statut,
                                             @RequestParam(required = false) UUID commercial,
                                             @RequestParam(required = false) UUID projet,
                                             @RequestParam(required = false) String search) {
        return dossierService.findAll(statut, commercial, projet, search);
    }

    @GetMapping("/{id}")
    public DossierDetailDto findById(@PathVariable UUID id) {
        return dossierService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DossierDetailDto> create(@RequestBody CreerDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.create(request));
    }

    @PatchMapping("/{id}/statut")
    public DossierDetailDto changerStatut(@PathVariable UUID id, @RequestBody ChangerStatutRequest request) {
        return dossierService.changerStatut(id, request);
    }

    @GetMapping("/{id}/echeancier")
    public List<EcheanceDto> findEcheances(@PathVariable UUID id) {
        return echeancierService.findEcheances(id);
    }

    @PostMapping("/{id}/echeancier")
    public ResponseEntity<EcheanceDto> addEcheance(@PathVariable UUID id, @RequestBody CreerEcheanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addEcheance(id, request));
    }

    @PatchMapping("/{id}/echeancier/{echeanceId}")
    public EcheanceDto updateEcheance(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                       @RequestBody ModifierEcheanceRequest request) {
        return echeancierService.updateEcheance(id, echeanceId, request);
    }

    @PostMapping("/{id}/echeancier/{echeanceId}/reglements")
    public ResponseEntity<ReglementDto> addReglement(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                                       @RequestBody CreerReglementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addReglement(id, echeanceId, request));
    }

    @GetMapping("/{id}/montants")
    public MontantsDossierDto montants(@PathVariable UUID id) {
        return dossierService.montants(id);
    }
}
