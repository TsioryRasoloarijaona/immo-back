package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainService;
import com.tsix.immocrm.web.export.DemandeExport;
import com.tsix.immocrm.web.export.ExportListe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Lotissements")
@RestController
@RequestMapping("/api/lotissements")
public class LotissementController {

    private final TerrainService terrains;
    private final ExportListe export;

    public LotissementController(TerrainService terrains, ExportListe export) {
        this.terrains = terrains;
        this.export = export;
    }

    @GetMapping("/export")
    @Operation(summary = "Export xlsx ou pdf de la liste des lots de terrain", description = ExportListe.DESCRIPTION)
    @ApiResponse(responseCode = "400", description = ExportListe.REFUS, content = @Content)
    public ResponseEntity<StreamingResponseBody> exporter(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @ParameterObject @ModelAttribute DemandeExport demande,
            @Parameter(hidden = true) @SortDefault(sort = "code", direction = Sort.Direction.ASC) Sort sort) {
        return TerrainController.exporter(export, terrains, filtres, demande, sort, true);
    }

    @GetMapping
    @Operation(summary = "Lots de terrain : terrains d'une tranche, vocation autre que PROJET")
    @ApiResponse(responseCode = "200", description = "Page de lots de terrain des tranches visibles.")
    public PageDto<TerrainListeDto> lister(
            @ParameterObject @ModelAttribute FiltresTerrains filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageDto.of(terrains.lister(filtres, true, pageable));
    }
}
