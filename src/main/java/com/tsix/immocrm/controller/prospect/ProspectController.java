package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectDoublonDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            @RequestParam(required = false) List<String> type,
            @Parameter(example = "code,nom,prenom,cin,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectService.findAll(search, type, pageable, ColonneProspect.resoudre(colonnes)));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @GetMapping("/doublons")
    @Operation(summary = "Fiches prospect/client existantes correspondant à une identité")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiches correspondantes, éventuellement aucune."),
            @ApiResponse(responseCode = "400", description = "Aucun critère utilisable : tous les "
                    + "paramètres sont absents, ou ne portent que des valeurs bidon.")
    })
    public List<ProspectDoublonDto> doublons(
            @Parameter(description = "Nom de famille (ou nom complet si `prenom` est vide).", example = "Lahlou")
            @RequestParam(required = false) String nom,
            @Parameter(description = "Prénom.", example = "El Mehdi")
            @RequestParam(required = false) String prenom,
            @Parameter(description = "Raison sociale, pour une personne morale.")
            @RequestParam(required = false) String raisonSociale,
            @Parameter(description = "Téléphone fixe, n'importe quel format.", example = "0674146033")
            @RequestParam(required = false) String telephone,
            @Parameter(description = "Mobile, n'importe quel format.", example = "+212 6 74 14 60 33")
            @RequestParam(required = false) String mobile,
            @Parameter(description = "Email.", example = "elmehdi.lahlou76@gmail.com")
            @RequestParam(required = false) String email) {
        if (!ProspectService.aUnCritereDeDoublon(prenom, nom, raisonSociale, telephone, mobile, email)) {
            throw new InvalidRequestException(
                    "Au moins un critère est requis : nom/prenom, raisonSociale, telephone, mobile ou email");
        }
        return prospectService.rechercherDoublons(prenom, nom, raisonSociale, telephone, mobile, email);
    }

    @PostMapping
    @Operation(summary = "Crée un prospect")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospect créé."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<ProspectDetailDto> create(@RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectService.create(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifie un prospect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospect mis à jour."),
            @ApiResponse(responseCode = "400", description = "Mêmes causes qu'à la création."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public ProspectDetailDto update(@PathVariable UUID id, @RequestBody ModifierProspectRequest request) {
        return prospectService.update(id, request);
    }

    // Mêmes DTO que /api/prospections, /api/dossiers, /api/encaissements, donc filtrés par le
    // périmètre commercial comme eux ; le prospect lui-même ne l'est pas (FEATURE-perimetre-commercial.md §9bis.2).

    @GetMapping("/{id}/prospections")
    @Operation(summary = "Prospections d'un prospect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospections du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<ProspectionListItemDto> getProspections(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.prospections(id, pageable));
    }

    @GetMapping("/{id}/dossiers")
    @Operation(summary = "Dossiers de vente d'un prospect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<DossierListItemDto> getDossiers(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.dossiers(id, pageable));
    }

    @GetMapping("/{id}/reglements")
    @Operation(summary = "Règlements d'un prospect, tous dossiers confondus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Règlements du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<ReglementProspectDto> getReglements(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.reglements(id, pageable));
    }

    @GetMapping("/{id}/historique")
    public PageDto<HistoriqueDto> getHistorique(@PathVariable UUID id,
                                                 @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.historique(id, pageable));
    }

    @GetMapping("/{id}/contacts")
    @Operation(summary = "Contacts du prospect")
    public List<ContactProspectDto> getContacts(@PathVariable UUID id) {
        return subResourcesService.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    @Operation(summary = "Ajoute un contact au prospect")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact créé/lié et rattaché."),
            @ApiResponse(responseCode = "400", description = "`relationTypeId` manquant ; ni "
                    + "`contactProspectId` ni `nouveauContact` (ou les deux) ; ou référentiel "
                    + "introuvable."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public ContactProspectDto ajouterContact(@PathVariable UUID id, @RequestBody CreerContactProspectRequest request) {
        return subResourcesService.ajouterContact(id, request);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<Void> retirerContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        subResourcesService.retirerContact(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
