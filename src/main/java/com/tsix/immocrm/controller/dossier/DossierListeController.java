package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.LignesListeDossiers;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

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

    private static final String FILTRES_GROUPES = "filtresGroupes";

    /** /groupes n'a pas de paramètre {@code lignes} : toute valeur vaut absente (DOSSIERS), jamais un 400. */
    @InitBinder(FILTRES_GROUPES)
    void sansLignes(WebDataBinder binder) {
        // setDisallowedFields est sans effet sur une liaison par constructeur (record).
        binder.registerCustomEditor(LignesListeDossiers.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String texte) {
                setValue(null);
            }
        });
    }

    @GetMapping
    @Operation(summary = "Liste paginée des dossiers de vente")
    @ApiResponse(responseCode = "400", content = @Content)
    @Parameter(name = "lignes", in = ParameterIn.QUERY, example = "DOSSIERS")
    public PageDto<DossierListItemDto> findAll(
            @ParameterObject @ModelAttribute FiltresListeDossiers filtres,
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
    @Parameter(name = "lignes", in = ParameterIn.QUERY, hidden = true)
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
            @ParameterObject @ModelAttribute(FILTRES_GROUPES) FiltresListeDossiers filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
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
    public TotauxDossiersDto totaux(@ParameterObject @ModelAttribute FiltresListeDossiers filtres) {
        return listeService.totaux(filtres);
    }

    @GetMapping("/totaux-colonnes")
    @Operation(summary = "Résumés (Σ) par colonne sur tous les dossiers du filtre, pas sur une page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé"),
            @ApiResponse(responseCode = "400", description = "Jeton d'`agregats` inconnu, colonne non "
                    + "agrégeable ou fonction refusée pour son type.", content = @Content)
    })
    public TotauxListeDto totauxColonnes(
            @Parameter(example = "prix:AVG")
            @RequestParam(required = false) List<String> agregats,
            @ParameterObject @ModelAttribute FiltresListeDossiers filtres) {
        return listeService.totauxColonnes(filtres, ColonneDossier.resoudreAgregats(agregats));
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
            @ParameterObject @ModelAttribute FiltresListeDossiers filtres,
            @RequestParam(defaultValue = "false") boolean ordreOmran,
            @Parameter(example = "code,statut,clientNom,prix")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
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
