package com.tsix.immocrm.controller.suiviprevisions;

import com.tsix.immocrm.dto.rapport.ParametreRapportDto;
import com.tsix.immocrm.dto.rapport.ResultatRapportDto;
import com.tsix.immocrm.services.rapport.ServiceRapports;
import com.tsix.immocrm.services.rapport.vente.SuiviPrevisionsVente;
import com.tsix.immocrm.web.export.ExportTableur;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Suivi des prévisions")
@RestController
@RequestMapping("/api/suivi-previsions")
public class SuiviPrevisionsController {

    private static final String FORMAT = "format";

    private final SuiviPrevisionsVente service;
    private final ExportTableur exportTableur;

    public SuiviPrevisionsController(SuiviPrevisionsVente service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping("/parametres")
    @Operation(summary = "Paramètres de l'écran : année, tranches visibles, frais")
    @ApiResponse(responseCode = "200", description = "Trois paramètres, défauts et options compris.")
    public List<ParametreRapportDto> parametres() {
        return service.parametres();
    }

    @GetMapping
    @Operation(summary = "Prévu et réel par mois : concrétisations nettes et encaissements")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deux sections de douze mois, totaux et graphiques compris."),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResultatRapportDto resultat(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        return service.resultat(parametres);
    }

    @GetMapping("/detail")
    @Operation(summary = "Détail d'un mois : dossiers, mouvements et échéances qui font les cellules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trois sections : dossiers, mouvements, echeances."),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResultatRapportDto detail(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        return service.detail(parametres);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de l'écran : une feuille par section")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResponseEntity<byte[]> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        ExportTableur.exigerXlsx(format);
        MultiValueMap<String, String> sansFormat = new LinkedMultiValueMap<>(parametres);
        sansFormat.remove(FORMAT);
        ServiceRapports.Export export = service.exporter(format, sansFormat);
        return exportTableur.reponseEnMemoire(export.code(), export.libelle(), export.feuilles());
    }
}
