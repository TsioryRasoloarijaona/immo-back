package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.dto.prospection.FiltresAvancesProspection;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.prospection.FiltresListeProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Exports Excel des listes de prospections et de pré-prospections : mêmes lectures que les listes. */
@Tag(name = "Prospections — export")
@RestController
public class ProspectionExportController {

    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ProspectionService prospectionService;
    private final ExportTableur exportTableur;

    public ProspectionExportController(ProspectionService prospectionService, ExportTableur exportTableur) {
        this.prospectionService = prospectionService;
        this.exportTableur = exportTableur;
    }

    @GetMapping("/api/prospections/export")
    @Operation(summary = "Export Excel de la liste des prospections, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "docDate,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste. Défaut : docDate,desc.",
            array = @ArraySchema(schema = @Schema(type = "string")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = MIME_XLSX, schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporterProspections(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false) String dateDebut,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @Parameter(example = "code,prospectNom,etape,commercialNom")
            @RequestParam(required = false) List<String> colonnes,
            @ModelAttribute FiltresAvancesProspection filtres,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneProspection> selection = ColonneProspection.resoudre(colonnes);
        FiltresListeProspection liste = new FiltresListeProspection(etape, origine, commercial, trancheId, false,
                dateDebut, dateFin, search, ouvertes);
        List<ProspectionListItemDto> lignes = LignesExport.toutes(sort, page ->
                prospectionService.findAll(liste, filtres, selection, null, null, null, page));
        return exportTableur.reponse("prospections", "Prospections", LignesExport.ordre(colonnes, selection), lignes);
    }

    @GetMapping("/api/pre-prospections/export")
    @Operation(summary = "Export Excel de la liste des pré-prospections, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "docDate,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste. Défaut : docDate,desc.",
            array = @ArraySchema(schema = @Schema(type = "string")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = MIME_XLSX, schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporterPreProspections(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansTranche,
            @RequestParam(required = false) List<UUID> projetId,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false) String dateDebut,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @Parameter(example = "code,prospectNom,telephone,origine")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneProspection> selection = ColonneProspection.resoudre(colonnes);
        FiltresListeProspection liste = new FiltresListeProspection(etape, origine, commercial, trancheId,
                inclureSansTranche, dateDebut, dateFin, search, ouvertes);
        List<ProspectionListItemDto> lignes = LignesExport.toutes(sort, page ->
                prospectionService.findPreProspections(liste, projetId, null, null, null, page));
        return exportTableur.reponse("pre-prospections", "Pré-prospections",
                LignesExport.ordre(colonnes, selection), lignes);
    }
}
