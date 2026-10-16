package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.LignesListeDossiers;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.liste.ChampDateListeDto;
import com.tsix.immocrm.dto.dossier.liste.StatutListeDossierDto;
import com.tsix.immocrm.dto.dossier.liste.TotauxDossiersDto;
import com.tsix.immocrm.services.dossier.liste.FiltresListeDossiers;
import com.tsix.immocrm.services.dossier.liste.ListeDossiersService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Dossiers — liste")
@RestController
@RequestMapping("/api/dossiers")
public class DossierListeController {

    private final ListeDossiersService listeService;
    private final ExportTableur exportTableur;

    public DossierListeController(ListeDossiersService listeService, ExportTableur exportTableur) {
        this.listeService = listeService;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des dossiers de vente")
    @ApiResponse(responseCode = "400", content = @Content)
    public PageDto<DossierListItemDto> findAll(
            @Parameter(example = "CONCRETISATION")
            @RequestParam(required = false) List<String> statut,
            @Parameter(description = "Id du commercial (CfgTier.id, cfgCommercial).")
            @RequestParam(required = false) UUID commercial,
            @Parameter(description = "Id du projet (CfgProject.id), résolu par la tranche de l'article principal.")
            @RequestParam(required = false) UUID projet,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "CONCRETISATION")
            @RequestParam(required = false) ChampDateDossier champDate,
            @Parameter(description = "Borne inférieure (incluse) sur champDate, ISO-8601.", example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur champDate, ISO-8601.", example = "2026-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Boolean dateVide,
            @RequestParam(required = false) String search,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSortis,
            @Parameter(example = "DOSSIERS")
            @RequestParam(required = false) LignesListeDossiers lignes,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean ordreOmran,
            @Parameter(example = "code,statut,projetCode,trancheCode,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "statut")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(example = "CONCRETISATION")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        FiltresListeDossiers filtres = new FiltresListeDossiers(statut, commercial, projet, trancheId, champDate,
                dateDebut, dateFin, dateVide, search, inclureSortis, lignes);
        ColonneDossier groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneDossier::resoudreGroupement);
        ListeDossiersService.Depliage depliage = groupe == null ? null
                : new ListeDossiersService.Depliage(groupe, groupeValeur, Groupements.granularite(groupe, granularite));
        return PageDto.of(listeService.lister(filtres, depliage, ordreOmran, pageable,
                ColonneDossier.resoudre(colonnes)));
    }

    @GetMapping("/groupes")
    @Operation(summary = "Liste des dossiers repliée en groupes, avec effectifs et totaux")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau de "
                    + "groupement est supporté.", example = "statut", required = true)
            @RequestParam String groupBy,
            @Parameter(example = "prix,totalAPayer,prix:AVG")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "Ordre des groupes : LIBELLE (croissant, défaut) ou NOMBRE "
                    + "(effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<String> statut,
            @RequestParam(required = false) UUID commercial,
            @RequestParam(required = false) UUID projet,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) ChampDateDossier champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Boolean dateVide,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean inclureSortis,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        FiltresListeDossiers filtres = new FiltresListeDossiers(statut, commercial, projet, trancheId, champDate,
                dateDebut, dateFin, dateVide, search, inclureSortis, LignesListeDossiers.DOSSIERS);
        ColonneDossier groupe = ColonneDossier.resoudreGroupement(groupBy);
        return listeService.grouper(filtres, groupe, Groupements.granularite(groupe, granularite),
                ColonneDossier.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping("/totaux")
    @Operation(summary = "Σ des montants par nature sur tout le filtre de la liste (bande de totaux)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sommes du filtre, une clé par colonne additionnable."),
            @ApiResponse(responseCode = "400", content = @Content)
    })
    public TotauxDossiersDto totaux(
            @RequestParam(required = false) List<String> statut,
            @RequestParam(required = false) UUID commercial,
            @RequestParam(required = false) UUID projet,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) ChampDateDossier champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Boolean dateVide,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean inclureSortis,
            @RequestParam(required = false) LignesListeDossiers lignes) {
        return listeService.totaux(new FiltresListeDossiers(statut, commercial, projet, trancheId, champDate,
                dateDebut, dateFin, dateVide, search, inclureSortis, lignes));
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel de la liste des dossiers, filtres et colonnes appliqués")
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
            @RequestParam(required = false) List<String> statut,
            @RequestParam(required = false) UUID commercial,
            @RequestParam(required = false) UUID projet,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) ChampDateDossier champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) Boolean dateVide,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean inclureSortis,
            @RequestParam(required = false) LignesListeDossiers lignes,
            @RequestParam(defaultValue = "false") boolean ordreOmran,
            @Parameter(example = "code,statut,clientNom,prix")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        FiltresListeDossiers filtres = new FiltresListeDossiers(statut, commercial, projet, trancheId, champDate,
                dateDebut, dateFin, dateVide, search, inclureSortis, lignes);
        List<ColonneDossier> ordre = LignesExport.ordre(colonnes, ColonneDossier.resoudre(colonnes));
        List<DossierListItemDto> lignesExport = listeService.toutes(filtres, ordreOmran, sort,
                new LinkedHashSet<>(ordre));
        return exportTableur.reponse("dossiers", "Dossiers de vente", ordre, lignesExport);
    }

    @GetMapping("/statuts")
    @Operation(summary = "Statuts de dossier proposés au filtre de la liste")
    public List<StatutListeDossierDto> statuts() {
        return listeService.statuts();
    }

    @GetMapping("/champs-date")
    @Operation(summary = "Les 47 dates du filtre de période, dans l'ordre d'OMRAN")
    public List<ChampDateListeDto> champsDate() {
        return Arrays.stream(ChampDateDossier.values())
                .map(c -> new ChampDateListeDto(c.name(), c.getLibelle()))
                .toList();
    }
}
