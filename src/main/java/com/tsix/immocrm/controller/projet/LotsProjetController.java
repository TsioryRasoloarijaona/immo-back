package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.LotListItemDto;
import com.tsix.immocrm.dto.projet.ReferentielsProjetDto;
import com.tsix.immocrm.services.projet.FiltresLots;
import com.tsix.immocrm.services.projet.LotsProjetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet")
public class LotsProjetController {

    private static final String ID_LOT = "Identifiant du lot (PrjMarket.PKey).";
    private static final String LOT_INTROUVABLE = "Lot introuvable : id inconnu, lot sans projet ou projet hors périmètre.";

    private final LotsProjetService service;

    public LotsProjetController(LotsProjetService service) {
        this.service = service;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres Lots et Décomptes")
    public ReferentielsProjetDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/lots")
    @Operation(summary = "Liste paginée des lots des projets visibles")
    public PageDto<LotListItemDto> lots(
            @ParameterObject @ModelAttribute FiltresLots filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(service.lots(filtres, pageable));
    }

    @GetMapping("/lots/{id}")
    @Operation(summary = "Fiche d'un lot")
    @ApiResponse(responseCode = "200", description = "Fiche du lot.")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public LotDetailDto lot(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.lot(id);
    }

    @GetMapping("/lots/{id}/bordereau")
    @Operation(summary = "Bordereau définitif d'un lot")
    @ApiResponse(responseCode = "200", description = "Lignes du bordereau et leurs totaux.")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public BordereauDto bordereau(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.bordereau(id);
    }

    @GetMapping("/lots/{id}/decomptes")
    @Operation(summary = "Décomptes d'un lot, par numéro croissant")
    @ApiResponse(responseCode = "200", description = "Décomptes du lot (éventuellement vide).")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public List<DecompteListItemDto> decomptesDuLot(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.decomptesDuLot(id);
    }
}
