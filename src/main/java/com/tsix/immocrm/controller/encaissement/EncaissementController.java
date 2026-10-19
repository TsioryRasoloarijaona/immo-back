package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDetailDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.dto.encaissement.TotauxEncaissementsDto;
import com.tsix.immocrm.services.encaissement.EncaissementService;
import com.tsix.immocrm.services.encaissement.EncaissementService.FiltresEncaissements;
import com.tsix.immocrm.web.Groupements;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Encaissements")
@RestController
@RequestMapping("/api/encaissements")
public class EncaissementController {

    private final EncaissementService encaissementService;
    private final ExportTableur exportTableur;

    public EncaissementController(EncaissementService encaissementService, ExportTableur exportTableur) {
        this.encaissementService = encaissementService;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des encaissements (règlements de Dossiers de vente)")
    public PageDto<EncaissementDto> findAll(
            @ParameterObject @ModelAttribute FiltresEncaissements filtres,
            @Parameter(example = "code,description,montantBrut,typeLibelle,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "typeLibelle")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(example = "CHEQUE")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneEncaissement groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneEncaissement::resoudreGroupement);
        return PageDto.of(encaissementService.findAll(filtres, groupe, groupeValeur, Groupements.granularite(groupe, granularite), pageable,
                ColonneEncaissement.resoudre(colonnes)));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des encaissements repliée en groupes, avec effectifs et totaux",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau de "
                    + "groupement est supporté.", example = "typeLibelle", required = true)
            @RequestParam String groupBy,
            @Parameter(example = "montantBrutRestant,montantNet,montantBrut:AVG")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "Ordre des groupes : LIBELLE (croissant, défaut) ou NOMBRE "
                    + "(effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @ParameterObject @ModelAttribute FiltresEncaissements filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneEncaissement groupe = ColonneEncaissement.resoudreGroupement(groupBy);
        return encaissementService.grouper(filtres, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneEncaissement.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping("/totaux")
    @Operation(summary = "Totaux de la liste plate sur tout le filtre (bande « Total : »), mêmes filtres que la liste")
    public TotauxEncaissementsDto totaux(@ParameterObject @ModelAttribute FiltresEncaissements filtres) {
        return encaissementService.totaux(filtres);
    }

    @GetMapping("/totaux-colonnes")
    @Operation(summary = "Résumés (Σ) par colonne sur tous les encaissements du filtre, pas sur une page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé"),
            @ApiResponse(responseCode = "400", description = "Jeton d'`agregats` inconnu, colonne non "
                    + "agrégeable ou fonction refusée pour son type.", content = @Content)
    })
    public TotauxListeDto totauxColonnes(
            @Parameter(example = "montantBrut:AVG")
            @RequestParam(required = false) List<String> agregats,
            @ParameterObject @ModelAttribute FiltresEncaissements filtres) {
        return encaissementService.totauxColonnes(filtres, ColonneEncaissement.resoudreAgregats(agregats));
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de la liste des encaissements, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "docDate,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste. Défaut : docDate,desc.",
            array = @ArraySchema(schema = @Schema(type = "string")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresEncaissements filtres,
            @Parameter(example = "reference,dateDocument,tierNom,montantBrut")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneEncaissement> selection = ColonneEncaissement.resoudre(colonnes);
        List<EncaissementDto> lignes = LignesExport.toutes(sort,
                page -> encaissementService.findAll(filtres, null, null, null, page, selection));
        return exportTableur.reponse("encaissements", "Encaissements", LignesExport.ordre(colonnes, selection),
                lignes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un encaissement (règlement) et ses affectations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encaissement trouvé."),
            @ApiResponse(responseCode = "404")
    })
    public EncaissementDetailDto findById(
            @Parameter(description = "Id du règlement (StlSettlement.id).")
            @PathVariable UUID id) {
        return encaissementService.findById(id);
    }
}
