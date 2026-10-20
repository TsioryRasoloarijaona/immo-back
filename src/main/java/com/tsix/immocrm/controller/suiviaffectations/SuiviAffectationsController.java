package com.tsix.immocrm.controller.suiviaffectations;

import com.tsix.immocrm.domain.ColonneSuiviAffectations;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.referentiel.ColonneDto;
import com.tsix.immocrm.dto.suiviaffectations.LigneSuiviAffectationsDto;
import com.tsix.immocrm.dto.suiviaffectations.StatutSuiviAffectationsDto;
import com.tsix.immocrm.services.suiviaffectations.FiltresSuiviAffectations;
import com.tsix.immocrm.services.suiviaffectations.SuiviAffectationsService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Suivi des affectations")
@RestController
public class SuiviAffectationsController {

    private static final String BASE = "/api/suivi-affectations";
    private static final String TRI_DEFAUT = "dateMouvement";

    private final SuiviAffectationsService service;
    private final ExportTableur exportTableur;

    public SuiviAffectationsController(SuiviAffectationsService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping(BASE)
    @Operation(summary = "Liste paginée des affectations de règlements aux échéances de vente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de lignes, colonnes demandées seules."),
            @ApiResponse(responseCode = "400", description = "Colonne, tri, statut, état, sens ou période invalide.",
                    content = @Content)
    })
    public PageDto<LigneSuiviAffectationsDto> lister(
            @ParameterObject @ModelAttribute FiltresSuiviAffectations filtres,
            @Parameter(example = "categorieLibelle,trancheCode,articleCode,montantAffecte,dateMouvement,dossierCode")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "categorieLibelle")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(example = "AVANCE")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = TRI_DEFAUT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        ColonneSuiviAffectations groupe = Groupements.colonneDeDepliage(groupeColonne, groupeValeur,
                SuiviAffectationsController::groupable);
        SuiviAffectationsService.Depliage depliage = groupe == null ? null
                : new SuiviAffectationsService.Depliage(groupe, groupeValeur,
                        Groupements.granularite(groupe, granularite));
        selection(colonnes);
        return PageDto.of(service.lister(filtres, depliage, pageable));
    }

    @GetMapping(BASE + "/groupes")
    @Operation(summary = "Liste du suivi des affectations repliée en groupes, avec effectifs et agrégats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste."),
            @ApiResponse(responseCode = "400", description = "Colonne non groupable, agrégat ou filtre invalide.",
                    content = @Content)
    })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement (catalogue, groupable = true).",
                    example = "categorieLibelle", required = true)
            @RequestParam String groupBy,
            @ParameterObject @ModelAttribute FiltresSuiviAffectations filtres,
            @Parameter(description = "Vrai : groupe des lignes sans valeur inclus.", example = "true")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @Parameter(description = "Pas d'une colonne date.", example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (défaut) ou NOMBRE (effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(description = "Jetons d'agrégat par groupe.", example = "montantAffecte")
            @RequestParam(required = false) List<String> agregats,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneSuiviAffectations groupe = groupable(groupBy);
        return service.grouper(filtres, groupe, Groupements.granularite(groupe, granularite),
                Colonnes.resoudreAgregats(ColonneSuiviAffectations.class, agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping(BASE + "/totaux-colonnes")
    @Operation(summary = "Résumés (Σ) par colonne sur toutes les lignes du filtre, pas sur une page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé."),
            @ApiResponse(responseCode = "400", description = "Jeton d'agregats inconnu, fonction refusée ou filtre "
                    + "invalide.", content = @Content)
    })
    public TotauxListeDto totauxColonnes(
            @Parameter(example = "montantAffecte")
            @RequestParam(required = false) List<String> agregats,
            @ParameterObject @ModelAttribute FiltresSuiviAffectations filtres) {
        return service.totauxColonnes(filtres,
                Colonnes.resoudreAgregats(ColonneSuiviAffectations.class, agregats));
    }

    @GetMapping(BASE + "/export")
    @Operation(summary = "Export Excel du suivi des affectations, filtres et colonnes appliqués")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Format autre que xlsx, colonne, tri ou filtre invalide.",
                    content = @Content),
            @ApiResponse(responseCode = "200", description = "Classeur Excel, une feuille DONNEES.",
                    content = @Content(schema = @Schema(type = "string", format = "binary"),
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    })
    @Parameter(name = "sort", in = ParameterIn.QUERY, array = @ArraySchema(schema = @Schema(type = "string")),
            description = "Tri `colonne,asc|desc`, répétable, comme la liste. Défaut : dateMouvement,desc.",
            example = "dateMouvement,desc")
    public ResponseEntity<StreamingResponseBody> exporter(
            @ParameterObject @ModelAttribute FiltresSuiviAffectations filtres,
            @Parameter(example = "categorieLibelle,trancheCode,articleCode,montantAffecte,dateMouvement")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(description = "Seul xlsx est accepté.", example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @Parameter(hidden = true) @SortDefault(sort = TRI_DEFAUT, direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneSuiviAffectations> selection = selection(colonnes);
        List<LigneSuiviAffectationsDto> lignes = LignesExport.toutes(sort,
                page -> service.lister(filtres, null, page));
        return exportTableur.reponse("suivi-des-affectations", "Suivi des affectations",
                LignesExport.ordre(colonnes, selection), lignes);
    }

    @GetMapping(BASE + "/statuts")
    @Operation(summary = "Statuts de dossier de vente du filtre, tous, dans l'ordre du workflow")
    public List<StatutSuiviAffectationsDto> statuts() {
        return service.statuts();
    }

    @GetMapping("/api/referentiels/colonnes/suivi-affectations")
    @Operation(summary = "Catalogue des colonnes du suivi des affectations")
    public List<ColonneDto> colonnes() {
        return Arrays.stream(ColonneSuiviAffectations.values()).map(SuiviAffectationsController::versDto).toList();
    }

    private static ColonneDto versDto(ColonneSuiviAffectations c) {
        return ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(), c.parDefaut(), c.chemin());
    }

    /** @throws com.tsix.immocrm.domain.ColonneInconnueException nom hors catalogue (400) */
    private static Set<ColonneSuiviAffectations> selection(List<String> colonnes) {
        return Colonnes.resoudre(ColonneSuiviAffectations.class, colonnes, ColonneSuiviAffectations.ID);
    }

    private static ColonneSuiviAffectations groupable(String nom) {
        return Colonnes.resoudreGroupement(ColonneSuiviAffectations.class, nom);
    }
}
