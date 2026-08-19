package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.prospection.AjouterAutreTrancheRequest;
import com.tsix.immocrm.dto.prospection.AjouterCategorieRequest;
import com.tsix.immocrm.dto.prospection.AjouterProfilRequest;
import com.tsix.immocrm.dto.prospection.AjouterTypeBienRequest;
import com.tsix.immocrm.dto.prospection.AjouterVilleRequest;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ProspectionAutreTrancheDto;
import com.tsix.immocrm.dto.prospection.ProspectionCategorieDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.ProspectionProfilDto;
import com.tsix.immocrm.dto.prospection.ProspectionTypeBienDto;
import com.tsix.immocrm.dto.prospection.ProspectionVilleDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.services.prospection.DevisService;
import com.tsix.immocrm.services.prospection.ProspectionQualificationService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/prospections")
public class ProspectionController {

    private final ProspectionService prospectionService;
    private final ProspectionQualificationService qualificationService;
    private final DevisService devisService;

    public ProspectionController(ProspectionService prospectionService,
                                  ProspectionQualificationService qualificationService,
                                  DevisService devisService) {
        this.prospectionService = prospectionService;
        this.qualificationService = qualificationService;
        this.devisService = devisService;
    }

    @GetMapping
    public PageDto<ProspectionListItemDto> findAll(@RequestParam(required = false) String etape,
            @RequestParam(required = false) String origine,
            @RequestParam(required = false) UUID commercial,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean ouvertes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectionService.findAll(etape, origine, commercial, search, ouvertes, pageable));
    }

    @GetMapping("/{id}")
    public ProspectionDetailDto findById(@PathVariable UUID id) {
        return prospectionService.findById(id);
    }

    @PatchMapping("/{id}")
    public ProspectionDetailDto modifierProspection(@PathVariable UUID id, @RequestBody ModifierProspectionRequest request) {
        return prospectionService.modifierProspection(id, request);
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
    public PageDto<InteractionDto> findInteractions(@PathVariable UUID id,
                                                      @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(prospectionService.findInteractions(id, pageable));
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<InteractionDto> addInteraction(@PathVariable UUID id, @RequestBody CreerInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.addInteraction(id, request));
    }

    // -- GAP-04: qualification sub-resources --

    @GetMapping("/{id}/villes")
    public List<ProspectionVilleDto> getVilles(@PathVariable UUID id) {
        return qualificationService.villes(id);
    }

    @PostMapping("/{id}/villes")
    public ProspectionVilleDto ajouterVille(@PathVariable UUID id, @RequestBody AjouterVilleRequest request) {
        return qualificationService.ajouterVille(id, request);
    }

    @DeleteMapping("/{id}/villes/{itemId}")
    public ResponseEntity<Void> retirerVille(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerVille(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/categories")
    public List<ProspectionCategorieDto> getCategories(@PathVariable UUID id) {
        return qualificationService.categories(id);
    }

    @PostMapping("/{id}/categories")
    public ProspectionCategorieDto ajouterCategorie(@PathVariable UUID id, @RequestBody AjouterCategorieRequest request) {
        return qualificationService.ajouterCategorie(id, request);
    }

    @DeleteMapping("/{id}/categories/{itemId}")
    public ResponseEntity<Void> retirerCategorie(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerCategorie(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/types-bien")
    public List<ProspectionTypeBienDto> getTypesBien(@PathVariable UUID id) {
        return qualificationService.typesBien(id);
    }

    @PostMapping("/{id}/types-bien")
    public ProspectionTypeBienDto ajouterTypeBien(@PathVariable UUID id, @RequestBody AjouterTypeBienRequest request) {
        return qualificationService.ajouterTypeBien(id, request);
    }

    @DeleteMapping("/{id}/types-bien/{itemId}")
    public ResponseEntity<Void> retirerTypeBien(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerTypeBien(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/profils")
    public List<ProspectionProfilDto> getProfils(@PathVariable UUID id) {
        return qualificationService.profils(id);
    }

    @PostMapping("/{id}/profils")
    public ProspectionProfilDto ajouterProfil(@PathVariable UUID id, @RequestBody AjouterProfilRequest request) {
        return qualificationService.ajouterProfil(id, request);
    }

    @DeleteMapping("/{id}/profils/{itemId}")
    public ResponseEntity<Void> retirerProfil(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerProfil(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/autres-tranches")
    public List<ProspectionAutreTrancheDto> getAutresTranches(@PathVariable UUID id) {
        return qualificationService.autresTranches(id);
    }

    @PostMapping("/{id}/autres-tranches")
    public ProspectionAutreTrancheDto ajouterAutreTranche(@PathVariable UUID id, @RequestBody AjouterAutreTrancheRequest request) {
        return qualificationService.ajouterAutreTranche(id, request);
    }

    @DeleteMapping("/{id}/autres-tranches/{itemId}")
    public ResponseEntity<Void> retirerAutreTranche(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerAutreTranche(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // -- GAP-07 (low priority): devis --

    @GetMapping("/{id}/devis")
    public PageDto<DevisDto> getDevis(@PathVariable UUID id,
                                       @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(devisService.findByProspection(id, pageable));
    }

    @PostMapping("/{id}/devis")
    public DevisDto creerDevis(@PathVariable UUID id, @RequestBody CreerDevisRequest request) {
        return devisService.creerDevis(id, request);
    }
}
