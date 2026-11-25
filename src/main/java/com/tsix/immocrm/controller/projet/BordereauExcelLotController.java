package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.domain.projet.TypeBordereau;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.BordereauEstimatifDto;
import com.tsix.immocrm.services.projet.ExportBordereauLot;
import com.tsix.immocrm.services.projet.ImportBordereauLot;
import com.tsix.immocrm.web.export.ExportTableur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/lots/{id}/bordereau")
public class BordereauExcelLotController {

    private static final String ID_LOT = "Identifiant du lot (PrjMarket.PKey).";
    private static final String TYPE = "Bordereau visé : DEFINITIF ou ESTIMATIF.";
    private static final String LOT_INTROUVABLE = "Lot introuvable : id inconnu, lot sans projet, projet hors périmètre ou statut invisible.";

    private final ImportBordereauLot importation;
    private final ExportBordereauLot export;

    public BordereauExcelLotController(ImportBordereauLot importation, ExportBordereauLot export) {
        this.importation = importation;
        this.export = export;
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel du bordereau définitif ou estimatif du lot")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx aux colonnes OMRAN : groupes puis lignes.",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Format autre que xlsx ou type inconnu.", content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public ResponseEntity<byte[]> exporter(
            @Parameter(description = ID_LOT) @PathVariable UUID id,
            @Parameter(description = TYPE, example = "DEFINITIF") @RequestParam TypeBordereau type,
            @Parameter(example = ExportTableur.FORMAT_XLSX) @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format) {
        ExportTableur.exigerXlsx(format);
        return export.exporter(id, type);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fusionne un classeur au format de l'export dans le bordereau du lot")
    @ApiResponse(responseCode = "200", description = "Bordereau relu, même corps que le GET du type demandé.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(oneOf = {BordereauDto.class, BordereauEstimatifDto.class})))
    @ApiResponse(responseCode = "400", content = @Content)
    @ApiResponse(responseCode = "403", description = "Droits du PUT du bordereau correspondant absents.", content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Mêmes refus que le PUT : estimatif cadenassé, ligne décomptée…",
            content = @Content)
    public Object importer(@Parameter(description = ID_LOT) @PathVariable UUID id,
                           @Parameter(description = TYPE, example = "DEFINITIF") @RequestParam TypeBordereau type,
                           @RequestPart("fichier") MultipartFile fichier) {
        return importation.importer(id, type, fichier);
    }
}
