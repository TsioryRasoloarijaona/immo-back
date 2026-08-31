package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.prospection.ProspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Pré-prospections", description = "Leads bruts non encore qualifiés (ComProspection "
        + "sans CfgProspectId ni CfgCommercialId) — même forme que /api/prospections.")
@RestController
@RequestMapping("/api/pre-prospections")
public class PreProspectionController {

    private final ProspectionService prospectionService;

    public PreProspectionController(ProspectionService prospectionService) {
        this.prospectionService = prospectionService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des pré-prospections",
            description = "Mêmes filtres que GET /api/prospections (étape, origine, commercial, "
                    + "tranche(s), plage de dates, recherche libre, ouvertes) appliqués aux leads "
                    + "non encore qualifiés. Voir instructions/filtres-avances.md point 5."
    )
    public PageDto<ProspectionListItemDto> findAll(
            @Parameter(description = "Code(s) ComProspectionKind (\"Nature\"/étape) à filtrer. "
                    + "Répéter le paramètre pour plusieurs étapes.")
            @RequestParam(required = false) List<String> etape,
            @Parameter(description = "Code(s) ComProspectionOrigin à filtrer. Répéter le "
                    + "paramètre pour plusieurs origines.")
            @RequestParam(required = false) List<String> origine,
            @Parameter(description = "Id(s) du commercial (CfgTier.id, cfgCommercial). Répéter "
                    + "le paramètre pour plusieurs commerciaux.")
            @RequestParam(required = false) List<UUID> commercial,
            @Parameter(description = "Id(s) de tranche visée (CfgTranche.id, "
                    + "ComProspection.stkHierarchy). Répéter le paramètre pour plusieurs tranches.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Borne inférieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean ouvertes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectionService.findPreProspections(etape, origine, commercial, trancheId, dateDebut,
                dateFin, search, ouvertes, pageable));
    }

    @PostMapping("/{id}/creer-prospect")
    public ResponseEntity<ProspectionDetailDto> creerProspect(@PathVariable UUID id,
                                                                @RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerProspect(id, request));
    }
}
