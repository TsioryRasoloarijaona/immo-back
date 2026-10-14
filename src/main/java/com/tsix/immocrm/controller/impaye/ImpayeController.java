package com.tsix.immocrm.controller.impaye;

import com.tsix.immocrm.domain.ChampDateImpaye;
import com.tsix.immocrm.domain.ColonneImpaye;
import com.tsix.immocrm.domain.EchuA;
import com.tsix.immocrm.dto.impaye.ImpayeDto;
import com.tsix.immocrm.dto.impaye.ImpayesPageDto;
import com.tsix.immocrm.services.impaye.ImpayeService;
import com.tsix.immocrm.services.impaye.ImpayeService.FiltresImpayes;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Impayés")
@RestController
@RequestMapping("/api/impayes")
public class ImpayeController {

    private static final String TRI_DEFAUT = "dateEcheance";

    private final ImpayeService impayeService;
    private final ExportTableur exportTableur;

    public ImpayeController(ImpayeService impayeService, ExportTableur exportTableur) {
        this.impayeService = impayeService;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des impayés de vente, avec les totaux du filtre complet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'impayés et totaux."),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public ImpayesPageDto findAll(
            @Parameter(description = "Id(s) de projet (CfgProject). Répétable, OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> projetId,
            @Parameter(description = "Id(s) de tranche (CfgTranche). Répétable, OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Condition « Échu à ». Défaut : ECHEANCE.", example = "ECHEANCE")
            @RequestParam(required = false) EchuA echu,
            @Parameter(example = "ECHEANCE")
            @RequestParam(required = false) ChampDateImpaye champDate,
            @Parameter(description = "Borne inférieure incluse (ISO, aaaa-mm-jj).", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Borne supérieure incluse (ISO, aaaa-mm-jj).", example = "2026-10-14")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Id du commercial du dossier (CfgTier).")
            @RequestParam(required = false) UUID commercialId,
            @Parameter(description = "Id de la catégorie de règlement de l'échéance (ComSettlementCategory).")
            @RequestParam(required = false) UUID categorieId,
            @Parameter(description = "Sous-chaîne du code ou du nom du client, insensible à la casse.")
            @RequestParam(required = false) String client,
            @Parameter(description = "Code ou désignation exacts de l'article principal du dossier.")
            @RequestParam(required = false) String article,
            @Parameter(description = "Recherche libre : notes, catégorie, tranche, projet, société, code dossier.")
            @RequestParam(required = false) String q,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = TRI_DEFAUT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return impayeService.findAll(new FiltresImpayes(projetId, trancheId, echu, champDate, du, au,
                commercialId, categorieId, client, article, q), pageable);
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de la liste des impayés, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "dateEcheance,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste. Défaut : dateEcheance,desc.",
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
            @RequestParam(required = false) List<UUID> projetId,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) EchuA echu,
            @RequestParam(required = false) ChampDateImpaye champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) UUID commercialId,
            @RequestParam(required = false) UUID categorieId,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String article,
            @RequestParam(required = false) String q,
            @Parameter(example = "dateEcheance,dossierCode,clientNom,montantReste")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = TRI_DEFAUT, direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneImpaye> selection = ColonneImpaye.resoudre(colonnes);
        List<ImpayeDto> lignes = impayeService.lignes(new FiltresImpayes(projetId, trancheId, echu, champDate, du,
                au, commercialId, categorieId, client, article, q), sort);
        List<ImpayeDto> plafonnees = lignes.size() > LignesExport.LIGNES_EXPORT_MAX
                ? lignes.subList(0, LignesExport.LIGNES_EXPORT_MAX) : lignes;
        return exportTableur.reponse("impayes", "Impayés", LignesExport.ordre(colonnes, selection), plafonnees);
    }
}
