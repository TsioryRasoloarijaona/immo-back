package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    public LotissementController(TerrainService terrains) {
        this.terrains = terrains;
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
