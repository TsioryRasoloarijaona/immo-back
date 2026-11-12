package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.TotauxDecomptesDto;
import com.tsix.immocrm.services.projet.DecomptesProjetService;
import com.tsix.immocrm.services.projet.FiltresDecomptes;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.LignesExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet/decomptes")
public class DecomptesController {

    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.de("dateDocument", "Date", TypeExport.DATE), ColonneFixe.texte("notes", "Notes"),
            ColonneFixe.de("lignesTTC", "Ava. TTC", TypeExport.MONTANT), ColonneFixe.de("avaPct", "Ava. %", TypeExport.NOMBRE),
            ColonneFixe.de("depassementTTC", "Dépa. TTC", TypeExport.MONTANT),
            ColonneFixe.de("travauxSupplementaires", "TraSup. TTC", TypeExport.MONTANT),
            ColonneFixe.de("approvisionnementAPayer", "Appro. TTC", TypeExport.MONTANT),
            ColonneFixe.de("fluctuationsAPayer", "Fluc. TTC", TypeExport.MONTANT),
            ColonneFixe.de("totalTTC", "Tot. TTC", TypeExport.MONTANT), ColonneFixe.texte("lotCode", "Lot"),
            ColonneFixe.texte("projetCode", "Projet"), ColonneFixe.texte("fournisseurNom", "Fournisseur"),
            ColonneFixe.de("montantHT", "HT", TypeExport.MONTANT), ColonneFixe.de("montantTTC", "TTC", TypeExport.MONTANT),
            ColonneFixe.de("retenueGarantie", "RG", TypeExport.MONTANT), ColonneFixe.de("netAPayer", "Net", TypeExport.MONTANT),
            ColonneFixe.texte("factureCode", "Facture"), ColonneFixe.texte("factureStatutDescription", "Statut facture"),
            ColonneFixe.de("montantRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("resteARegler", "Reste", TypeExport.MONTANT));

    private final DecomptesProjetService service;
    private final ExportTableur exportTableur;

    public DecomptesController(DecomptesProjetService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des décomptes des projets visibles")
    public PageDto<DecompteListItemDto> decomptes(
            @ParameterObject @ModelAttribute FiltresDecomptes filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = {"docDate", "code"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(service.decomptes(filtres, pageable));
    }

    @GetMapping("/totaux")
    @Operation(summary = "Totaux de la liste des décomptes : mêmes filtres, toutes pages")
    @ApiResponse(responseCode = "200", description = "Totaux ; à zéro si l'appelant n'a aucun projet habilité.")
    @ApiResponse(responseCode = "400", description = "Filtre mal formé (UUID, date, booléen).", content = @Content)
    public TotauxDecomptesDto totaux(@ParameterObject @ModelAttribute FiltresDecomptes filtres) {
        return service.totaux(filtres);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de la liste des décomptes, mêmes filtres et mêmes lignes que la liste")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "docDate,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste.")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx (feuille DONNEES).",
            content = @Content(schema = @Schema(type = "string", format = "binary"),
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @ApiResponse(responseCode = "400", description = "Filtre mal formé ou format autre que xlsx.", content = @Content)
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresDecomptes filtres,
            @Parameter(hidden = true) Sort sort) {
        ExportTableur.exigerXlsx(format);
        List<DecompteListItemDto> lignes = LignesExport.toutes(sort, page -> service.decomptes(filtres, page));
        return exportTableur.reponse("decomptes", "Décomptes", COLONNES_EXPORT, lignes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un décompte")
    @ApiResponse(responseCode = "200", description = "Fiche du décompte.")
    @ApiResponse(responseCode = "404", description = "Décompte introuvable : id inconnu ou lot hors périmètre.",
            content = @Content)
    public DecompteDetailDto decompte(
            @Parameter(description = "Identifiant du décompte (PrjWorkingOut.PKey).") @PathVariable UUID id) {
        return service.decompte(id);
    }
}
