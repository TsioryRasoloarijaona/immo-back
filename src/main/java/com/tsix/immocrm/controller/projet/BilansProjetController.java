package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.BilanProjetDto;
import com.tsix.immocrm.dto.projet.FiltresBilansProjet;
import com.tsix.immocrm.services.projet.BilansProjetService;
import com.tsix.immocrm.web.export.ExportTableur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Projet — bilans")
@RestController
@RequestMapping("/api/projet/bilans")
@ApiResponse(responseCode = "400", description = "Veuillez sélectionner au moins un projet ! (aucun projet habilité demandé).",
        content = @Content)
@ApiResponse(responseCode = "403", description = "Droit Yes.Prj.Front.Tdb.ProjectReview absent.", content = @Content)
public class BilansProjetController {

    private final BilansProjetService service;
    private final ExportTableur exportTableur;

    public BilansProjetController(BilansProjetService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Arbre des catégories de dépenses des projets demandés × 8 colonnes, avec écart")
    @ApiResponse(responseCode = "200", description = "Bilan ; `lignes` vide si aucune catégorie non nulle.")
    public BilanProjetDto bilans(@ParameterObject @ModelAttribute FiltresBilansProjet filtres) {
        return service.lire(filtres);
    }

    @ApiResponse(content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            schema = @Schema(format = "binary", type = "string")), description = "Classeur .xlsx.", responseCode = "200")
    @Operation(summary = "Export Excel du Bilan projet, mêmes filtres que l'arbre")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresBilansProjet filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponseEnMemoire("bilans-projet", "Bilans projet", service.feuillesExport(filtres));
    }
}
