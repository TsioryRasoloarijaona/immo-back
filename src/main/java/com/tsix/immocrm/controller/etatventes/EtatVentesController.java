package com.tsix.immocrm.controller.etatventes;

import com.tsix.immocrm.domain.ColonneEtatVentes;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.etatventes.LigneEtatVentesDto;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto;
import com.tsix.immocrm.dto.referentiel.ColonneDto;
import com.tsix.immocrm.services.etatventes.EtatVentesService;
import com.tsix.immocrm.services.etatventes.FiltresEtatVentes;
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
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "État des ventes")
@RestController
public class EtatVentesController {

    private static final String BASE = "/api/etat-des-ventes";
    private static final String TRI_DEFAUT = "articleCode";

    private final EtatVentesService service;
    private final ExportTableur exportTableur;

    public EtatVentesController(EtatVentesService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping(BASE + "/synthese")
    @Operation(summary = "Tableau tranche × catégorie × statut (nombre d'articles racines)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comptes du filtre."),
            @ApiResponse(responseCode = "400", description = "Id mal formé.", content = @Content)
    })
    public SyntheseEtatVentesDto synthese(
            @Parameter(description = "Id(s) d'entité (société du projet, CfgCompany.id), répétable.")
            @RequestParam(required = false) List<UUID> entiteId,
            @Parameter(description = "Id(s) de tranche (CfgTranche.id), répétable.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id du projet (CfgProject.id) de la tranche.")
            @RequestParam(required = false) UUID projetId,
            @Parameter(description = "Id(s) de catégorie d'article (StkItemCategory.id), répétable.")
            @RequestParam(required = false) List<UUID> categorieId) {
        return service.synthese(FiltresEtatVentes.synthese(entiteId, trancheId, projetId, categorieId));
    }

    @GetMapping(BASE)
    @Operation(summary = "Liste paginée des articles racines en vente et de leur statut")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'articles, colonnes demandées seules."),
            @ApiResponse(responseCode = "400", description = "Colonne, tri, statut ou nature inconnu.", content = @Content)
    })
    public PageDto<LigneEtatVentesDto> lister(
            @ParameterObject @ModelAttribute FiltresEtatVentes filtres,
            @Parameter(example = "articleCode,categorieCode,statutLibelle,clientNom,prixVenteTTC")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "statutCode")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(example = "RESERVATION")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = TRI_DEFAUT) Pageable pageable) {
        ColonneEtatVentes groupe = Groupements.colonneDeDepliage(groupeColonne, groupeValeur,
                EtatVentesController::groupable);
        EtatVentesService.Depliage depliage = groupe == null ? null
                : new EtatVentesService.Depliage(groupe, groupeValeur, Groupements.granularite(groupe, granularite));
        return PageDto.of(service.lister(filtres, depliage, selection(colonnes), pageable));
    }

    @GetMapping(BASE + "/groupes")
    @Operation(summary = "Liste de l'état des ventes repliée en groupes, avec effectifs et agrégats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste."),
            @ApiResponse(responseCode = "400", description = "Colonne non groupable ou agrégat inconnu.",
                    content = @Content)
    })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement (catalogue, groupable = true).", example = "categorieCode",
                    required = true)
            @RequestParam String groupBy,
            @Parameter(example = "surface,prixVenteTTC")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (défaut) ou NOMBRE (effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @ParameterObject @ModelAttribute FiltresEtatVentes filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneEtatVentes groupe = groupable(groupBy);
        return service.grouper(filtres, groupe, Groupements.granularite(groupe, granularite),
                Colonnes.resoudreAgregats(ColonneEtatVentes.class, agregats), tri != null ? tri : TriGroupe.DEFAUT,
                inclureSansValeur, pageable).versPage();
    }

    @GetMapping(BASE + "/totaux-colonnes")
    @Operation(summary = "Résumés (Σ) par colonne sur tous les articles du filtre, pas sur une page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé."),
            @ApiResponse(responseCode = "400", description = "Jeton d'agregats inconnu ou fonction refusée.",
                    content = @Content)
    })
    public TotauxListeDto totauxColonnes(
            @Parameter(example = "surface,prixVenteTTC,montantRegle,resteARegler")
            @RequestParam(required = false) List<String> agregats,
            @ParameterObject @ModelAttribute FiltresEtatVentes filtres) {
        return service.totauxColonnes(filtres, agregats);
    }

    @GetMapping(BASE + "/export")
    @Operation(summary = "Export Excel de l'état des ventes, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "articleCode,asc",
            description = "Tri `colonne,asc|desc`, répétable, comme la liste. Défaut : articleCode,asc.",
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
            @ParameterObject @ModelAttribute FiltresEtatVentes filtres,
            @Parameter(example = "articleCode,categorieCode,statutLibelle,prixVenteTTC")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = TRI_DEFAUT) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneEtatVentes> selection = selection(colonnes);
        List<LigneEtatVentesDto> lignes = LignesExport.toutes(sort,
                page -> service.lister(filtres, null, selection, page));
        return exportTableur.reponse("etat-des-ventes", "État des ventes", LignesExport.ordre(colonnes, selection),
                lignes);
    }

    /** @throws com.tsix.immocrm.domain.ColonneInconnueException nom hors catalogue (400) */
    private static Set<ColonneEtatVentes> selection(List<String> colonnes) {
        return Colonnes.resoudre(ColonneEtatVentes.class, colonnes, ColonneEtatVentes.ARTICLE_ID);
    }

    private static ColonneEtatVentes groupable(String nom) {
        return Colonnes.resoudreGroupement(ColonneEtatVentes.class, nom);
    }

    @GetMapping("/api/referentiels/colonnes/etat-des-ventes")
    @Operation(summary = "Catalogue des colonnes de l'état des ventes")
    public List<ColonneDto> colonnes() {
        return Arrays.stream(ColonneEtatVentes.values())
                .map(c -> EtatVentesService.sommeCalculee(c)
                        ? ColonneDto.sommeCalculee(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                                c.parDefaut())
                        : ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(), c.parDefaut(),
                                c.chemin()))
                .toList();
    }
}
