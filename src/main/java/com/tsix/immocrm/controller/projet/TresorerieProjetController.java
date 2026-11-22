package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.dto.projet.FiltresTresorerieProjet;
import com.tsix.immocrm.dto.projet.LigneCelluleTresorerieDto;
import com.tsix.immocrm.dto.projet.TresorerieProjetDto;
import com.tsix.immocrm.services.projet.TresorerieProjetService;
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

import java.util.List;

@Tag(name = "Projet — trésorerie")
@RestController
@RequestMapping("/api/projet/tresorerie")
@ApiResponse(responseCode = "400", description = "Veuillez sélectionner au moins un projet ! (aucun projet habilité demandé).",
        content = @Content)
@ApiResponse(responseCode = "403", description = "Droit Yes.Prj.Front.Tdb.ExpensesByYear absent.", content = @Content)
public class TresorerieProjetController {

    private final TresorerieProjetService service;
    private final ExportTableur exportTableur;

    public TresorerieProjetController(TresorerieProjetService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Arbre mensuel des prévisions des projets demandés (onglets Projet et Fournisseur)")
    @ApiResponse(responseCode = "200", description = "Arbre de l'année ; racines absentes si aucune prévision.")
    public TresorerieProjetDto tresorerie(@ParameterObject @ModelAttribute FiltresTresorerieProjet filtres) {
        return service.lire(filtres);
    }

    @GetMapping("/cellule")
    @Operation(summary = "Lignes sources d'une cellule (nœud × mois), triées par date ; leur somme = la cellule")
    @ApiResponse(responseCode = "200", description = "Tableau des lignes ; vide si la cellule vaut 0.")
    public List<LigneCelluleTresorerieDto> cellule(
            @ParameterObject @ModelAttribute FiltresTresorerieProjet filtres,
            @Parameter(description = "Mois de 1 (janvier) à 12.", example = "6") @RequestParam int mois,
            @Parameter(description = "Clé `cle` du nœud, lue dans l'arbre.") @RequestParam String noeud) {
        return service.cellule(filtres, mois, noeud);
    }

    @Operation(summary = "Export Excel de l'arbre de la Trésorerie projet, mêmes filtres que l'arbre")
    @GetMapping("/export")
    @ApiResponse(description = "Classeur .xlsx.", responseCode = "200", content = @Content(
            schema = @Schema(type = "string", format = "binary"),
            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    public ResponseEntity<byte[]> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresTresorerieProjet filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponseEnMemoire("tresorerie-projet", "Trésorerie projet", service.feuillesExport(filtres));
    }
}
