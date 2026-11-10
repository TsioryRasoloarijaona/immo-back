package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.FiltresSoldesFournisseurs;
import com.tsix.immocrm.dto.achat.SoldesFournisseursDto;
import com.tsix.immocrm.services.achat.SoldesFournisseursService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Tag(name = "Achat")
@RestController
@RequestMapping("/api/achat/fournisseurs/soldes")
public class SoldesFournisseursController {

    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(
            ColonneFixe.texte("fournisseurNom", "Fournisseur"), ColonneFixe.texte("societeCode", "Société"),
            ColonneFixe.de("nombrePieces", "Pièces", TypeExport.NOMBRE),
            ColonneFixe.de("montantAPayer", "À payer", TypeExport.MONTANT),
            ColonneFixe.de("montantRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT),
            ColonneFixe.de("nombreValideesARegler", "Validées à régler", TypeExport.NOMBRE),
            ColonneFixe.de("resteValideARegler", "Reste validé", TypeExport.MONTANT),
            ColonneFixe.de("montantADeduire", "À déduire", TypeExport.MONTANT));

    private final SoldesFournisseursService service;
    private final ExportTableur exportTableur;

    public SoldesFournisseursController(SoldesFournisseursService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Solde par fournisseur et société, sur les pièces visibles (non paginé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes et total ; vides si l'appelant n'a aucun projet."),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID).", content = @Content)
    })
    public SoldesFournisseursDto soldes(@ParameterObject @ModelAttribute FiltresSoldesFournisseurs filtres) {
        return service.soldes(filtres);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel du solde fournisseurs, mêmes filtres et mêmes lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Filtre mal formé ou format autre que xlsx.",
                    content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresSoldesFournisseurs filtres) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponse("soldes-fournisseurs", "Solde fournisseurs", COLONNES_EXPORT,
                service.soldes(filtres).lignes());
    }
}
