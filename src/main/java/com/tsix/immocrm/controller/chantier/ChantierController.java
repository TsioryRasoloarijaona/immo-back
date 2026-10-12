package com.tsix.immocrm.controller.chantier;

import com.tsix.immocrm.dto.chantier.BordereauDto;
import com.tsix.immocrm.dto.chantier.DecompteDetailDto;
import com.tsix.immocrm.dto.chantier.DecompteListItemDto;
import com.tsix.immocrm.dto.chantier.LotDetailDto;
import com.tsix.immocrm.dto.chantier.LotListItemDto;
import com.tsix.immocrm.dto.chantier.ProjetChantierDetailDto;
import com.tsix.immocrm.dto.chantier.ProjetChantierListItemDto;
import com.tsix.immocrm.dto.chantier.ReferentielsChantierDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.services.chantier.DecompteChantierService;
import com.tsix.immocrm.services.chantier.FiltresChantier;
import com.tsix.immocrm.services.chantier.LotChantierService;
import com.tsix.immocrm.services.chantier.ProjetChantierService;
import com.tsix.immocrm.services.chantier.ReferentielChantierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Chantier")
@RestController
@RequestMapping("/api/chantier")
public class ChantierController {

    private static final String OK = "200";
    private static final String INTROUVABLE = "404";
    private static final String MAL_FORME = "400";
    private static final String PROJET_404 = "Projet inconnu ou hors du périmètre de l'appelant.";
    private static final String LOT_404 = "Lot inconnu ou hors du périmètre de l'appelant.";
    private static final String ID_400 = "Identifiant mal formé (UUID attendu).";

    private final ReferentielChantierService referentielService;
    private final ProjetChantierService projetService;
    private final LotChantierService lotService;
    private final DecompteChantierService decompteService;

    public ChantierController(ReferentielChantierService referentielService, ProjetChantierService projetService,
                              LotChantierService lotService, DecompteChantierService decompteService) {
        this.referentielService = referentielService;
        this.projetService = projetService;
        this.lotService = lotService;
        this.decompteService = decompteService;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres du module Chantier")
    @ApiResponses(@ApiResponse(responseCode = OK, description = "Types, statuts de lot, statuts de facture, catégories."))
    public ReferentielsChantierDto referentiels() {
        return referentielService.referentiels();
    }

    @GetMapping("/projets")
    @Operation(summary = "Projets de travaux du périmètre, avec les totaux de leurs lots")
    @ApiResponses(@ApiResponse(responseCode = OK, description = "Projets triés par code ; liste vide si aucun projet habilité."))
    public List<ProjetChantierListItemDto> projets() {
        return projetService.liste();
    }

    @GetMapping("/projets/{id}")
    @Operation(summary = "Fiche d'un projet de travaux")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Fiche du projet."),
            @ApiResponse(responseCode = MAL_FORME, description = ID_400),
            @ApiResponse(responseCode = INTROUVABLE, description = PROJET_404)
    })
    public ProjetChantierDetailDto projet(@Parameter(description = "Id du projet (PrjProject).") @PathVariable UUID id) {
        return projetService.detail(id);
    }

    @GetMapping("/projets/{id}/lots")
    @Operation(summary = "Lots d'un projet, paginés et filtrables")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Page de lots."),
            @ApiResponse(responseCode = MAL_FORME, description = "Identifiant ou filtre mal formé, ou clé de tri inconnue."),
            @ApiResponse(responseCode = INTROUVABLE, description = PROJET_404)
    })
    public PageDto<LotListItemDto> lots(
            @Parameter(description = "Id du projet (PrjProject).") @PathVariable UUID id,
            @Parameter(description = "Statut du lot (StrStatus.PKey). Absent = tous.") @RequestParam(required = false) UUID statutId,
            @Parameter(description = "Type de lot (PrjMarketType.PKey). Absent = tous.") @RequestParam(required = false) UUID typeId,
            @Parameter(description = "Fournisseur titulaire (CfgTier.PKey). Absent = tous.")
            @RequestParam(required = false) UUID fournisseurId,
            @Parameter(description = "Recherche partielle sur le code, la désignation ou le fournisseur du lot.")
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(lotService.lotsDuProjet(
                new FiltresChantier.FiltreLots(id, statutId, typeId, fournisseurId, search), pageable));
    }

    @GetMapping("/lots/{id}")
    @Operation(summary = "Fiche d'un lot")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Fiche du lot."),
            @ApiResponse(responseCode = MAL_FORME, description = ID_400),
            @ApiResponse(responseCode = INTROUVABLE, description = LOT_404)
    })
    public LotDetailDto lot(@Parameter(description = "Id du lot (PrjMarket).") @PathVariable UUID id) {
        return lotService.detail(id);
    }

    @GetMapping("/lots/{id}/bordereau")
    @Operation(summary = "Bordereau définitif d'un lot")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Lignes et totaux ; lignes vides = lot au forfait."),
            @ApiResponse(responseCode = MAL_FORME, description = ID_400),
            @ApiResponse(responseCode = INTROUVABLE, description = LOT_404)
    })
    public BordereauDto bordereau(@Parameter(description = "Id du lot (PrjMarket).") @PathVariable UUID id) {
        return lotService.bordereau(id);
    }

    @GetMapping("/lots/{id}/decomptes")
    @Operation(summary = "Décomptes d'un lot, par numéro croissant")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Décomptes du lot (liste vide possible)."),
            @ApiResponse(responseCode = MAL_FORME, description = ID_400),
            @ApiResponse(responseCode = INTROUVABLE, description = LOT_404)
    })
    public List<DecompteListItemDto> decomptesDuLot(
            @Parameter(description = "Id du lot (PrjMarket).") @PathVariable UUID id) {
        return lotService.decomptes(id);
    }

    @GetMapping("/decomptes")
    @Operation(summary = "Tous les décomptes du périmètre, paginés et filtrables")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Page de décomptes ; vide si aucun projet habilité."),
            @ApiResponse(responseCode = MAL_FORME, description = "Filtre mal formé (UUID, date AAAA-MM-JJ) ou clé de tri inconnue.")
    })
    public PageDto<DecompteListItemDto> decomptes(
            @Parameter(description = "Projet (PrjProject.PKey). Absent = tous.") @RequestParam(required = false) UUID projetId,
            @Parameter(description = "Lot (PrjMarket.PKey). Absent = tous.") @RequestParam(required = false) UUID lotId,
            @Parameter(description = "Fournisseur du décompte (CfgTier.PKey). Absent = tous.")
            @RequestParam(required = false) UUID fournisseurId,
            @Parameter(description = "Date du décompte ≥ (incluse).", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date du décompte ≤ (incluse).", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Statut de la facture (BuyDocumentStatus.PKey). Absent = tous.")
            @RequestParam(required = false) UUID statutFactureId,
            @Parameter(description = "true = seulement les décomptes non facturés ; false ou absent = pas de filtre.")
            @RequestParam(required = false) Boolean sansFacture,
            @Parameter(description = "Recherche partielle : code du décompte, code ou désignation du lot, fournisseur.")
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = {"dateDocument", "code"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageDto.of(decompteService.liste(new FiltresChantier.FiltreDecomptes(projetId, lotId, fournisseurId,
                du, au, statutFactureId, sansFacture, search), pageable));
    }

    @GetMapping("/decomptes/{id}")
    @Operation(summary = "Fiche d'un décompte")
    @ApiResponses({
            @ApiResponse(responseCode = OK, description = "Fiche du décompte."),
            @ApiResponse(responseCode = MAL_FORME, description = ID_400),
            @ApiResponse(responseCode = INTROUVABLE, description = "Décompte inconnu ou hors du périmètre de l'appelant.")
    })
    public DecompteDetailDto decompte(@Parameter(description = "Id du décompte (PrjWorkingOut).") @PathVariable UUID id) {
        return decompteService.detail(id);
    }
}
