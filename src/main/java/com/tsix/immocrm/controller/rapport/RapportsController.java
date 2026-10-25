package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.dto.rapport.DescripteurRapportDto;
import com.tsix.immocrm.dto.rapport.GroupeRapportsDto;
import com.tsix.immocrm.dto.rapport.ResultatRapportDto;
import com.tsix.immocrm.services.rapport.ServiceRapports;
import com.tsix.immocrm.web.export.ExportPdf;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FormatExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Rapports")
@RestController
@RequestMapping("/api/rapports/{module}")
public class RapportsController {

    private static final String FORMAT = "format";

    private final ServiceRapports service;
    private final ExportTableur exportTableur;
    private final ExportPdf exportPdf;

    public RapportsController(ServiceRapports service, ExportTableur exportTableur, ExportPdf exportPdf) {
        this.service = service;
        this.exportTableur = exportTableur;
        this.exportPdf = exportPdf;
    }

    @GetMapping
    @Operation(summary = "Rapports du module visibles par l'appelant, groupés")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes triés ; [] si aucun rapport n'est visible."),
            @ApiResponse(responseCode = "404", description = "Module inconnu.", content = @Content)
    })
    public List<GroupeRapportsDto> lister(
            @Parameter(description = "Module : vente ou patrimoine.", example = "vente") @PathVariable String module) {
        return service.lister(module);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Descripteur d'un rapport : paramètres, défauts, options")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descripteur."),
            @ApiResponse(responseCode = "403", description = "Rapport existant non visible par l'appelant.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Module ou rapport inconnu.", content = @Content)
    })
    public DescripteurRapportDto descripteur(
            @Parameter(description = "Module : vente ou patrimoine.", example = "vente") @PathVariable String module,
            @Parameter(description = "Code du rapport.", example = "ComTdbSalesMonitoringGlobal")
            @PathVariable String code) {
        return service.descripteur(module, code);
    }

    @GetMapping("/{code}/resultat")
    @Operation(summary = "Exécute un rapport avec les paramètres de son descripteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sections calculées, totaux compris."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rapport existant non visible par l'appelant.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Module ou rapport inconnu.", content = @Content)
    })
    public ResultatRapportDto resultat(
            @Parameter(description = "Module : vente ou patrimoine.", example = "vente") @PathVariable String module,
            @Parameter(description = "Code du rapport.", example = "ComTdbSalesMonitoringGlobal")
            @PathVariable String code,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        return service.resultat(module, code, parametres);
    }

    @GetMapping("/{code}/export")
    @Operation(summary = "Export Excel ou PDF d'un rapport : une feuille ou un tableau par section")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx ou document PDF selon format.",
                    content = {
                            @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))
                    }),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rapport existant non visible par l'appelant.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Module ou rapport inconnu.", content = @Content)
    })
    public ResponseEntity<byte[]> exporter(
            @Parameter(description = "Module : vente ou patrimoine.", example = "vente") @PathVariable String module,
            @Parameter(description = "Code du rapport.", example = "ComTdbSalesMonitoringGlobal")
            @PathVariable String code,
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        MultiValueMap<String, String> sansFormat = new LinkedMultiValueMap<>(parametres);
        sansFormat.remove(FORMAT);
        ServiceRapports.Export export = service.exporter(module, code, format, sansFormat);
        return FormatExport.lire(format) == FormatExport.PDF
                ? exportPdf.reponse(export.code(), export.libelle(), export.filtres(), export.feuilles())
                : exportTableur.reponseEnMemoire(export.code(), export.libelle(), export.feuilles());
    }
}
