package com.tsix.immocrm.controller.suivicommerciaux;

import com.tsix.immocrm.domain.ColonneSuiviCommerciaux;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.referentiel.ColonneDto;
import com.tsix.immocrm.dto.suivicommerciaux.LigneSuiviCommerciauxDto;
import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto;
import com.tsix.immocrm.dto.suivicommerciaux.ObjectifCommercialDto;
import com.tsix.immocrm.dto.suivicommerciaux.SyntheseSuiviCommerciauxDto;
import com.tsix.immocrm.services.suivicommerciaux.FiltresSuiviCommerciaux;
import com.tsix.immocrm.services.suivicommerciaux.ObjectifCommercialService;
import com.tsix.immocrm.services.suivicommerciaux.SuiviCommerciauxMensuelService;
import com.tsix.immocrm.services.suivicommerciaux.SuiviCommerciauxService;
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
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Suivi des commerciaux")
@RestController
public class SuiviCommerciauxController {

    private static final String BASE = "/api/suivi-commerciaux";
    private static final String TRI_DEFAUT = "dateSaisie";
    private static final String HORS_PORTEE = "Commercial demandé hors de la portée de l'appelant.";
    private static final String COMMERCIAL_IDS = "Clé(s) de commercial (employé, sinon compte), répétable.";

    private final SuiviCommerciauxService service;
    private final SuiviCommerciauxMensuelService mensuelService;
    private final ObjectifCommercialService objectifService;
    private final ExportTableur exportTableur;

    public SuiviCommerciauxController(SuiviCommerciauxService service, SuiviCommerciauxMensuelService mensuelService,
                                      ObjectifCommercialService objectifService, ExportTableur exportTableur) {
        this.service = service;
        this.mensuelService = mensuelService;
        this.objectifService = objectifService;
        this.exportTableur = exportTableur;
    }

    @GetMapping(BASE + "/synthese")
    @Operation(summary = "Grille commercial × jour : appels, contacts et personnes jointes, visites, objectif")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes, objectif et totaux de la période."),
            @ApiResponse(responseCode = "400", description = "Période inversée ou de plus de 92 jours.", content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public SyntheseSuiviCommerciauxDto synthese(
            @Parameter(description = "Premier jour inclus, ISO ; défaut : au.", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Dernier jour inclus, ISO ; défaut : aujourd'hui.", example = "2026-10-16")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = COMMERCIAL_IDS)
            @RequestParam(required = false) List<UUID> commercialIds,
            @Parameter(description = "Projet de l'interaction (prospection, sinon dossier).")
            @RequestParam(required = false) UUID projetId) {
        return service.synthese(du, au, commercialIds, projetId);
    }

    @GetMapping(BASE)
    @Operation(summary = "Liste paginée des interactions des commerciaux visibles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'interactions, colonnes demandées seules."),
            @ApiResponse(responseCode = "400", description = "Colonne ou tri inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public PageDto<LigneSuiviCommerciauxDto> lister(
            @ParameterObject @ModelAttribute FiltresSuiviCommerciaux filtres,
            @Parameter(example = "jour,commercialNom,type,resultat,joint,personneNom")
            @RequestParam(required = false) List<String> colonnes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = TRI_DEFAUT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        selection(colonnes);
        return PageDto.of(service.lister(filtres, pageable));
    }

    @GetMapping(BASE + "/groupes")
    @Operation(summary = "Liste des interactions repliée en groupes, avec effectifs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste."),
            @ApiResponse(responseCode = "400", description = "Colonne non groupable ou agrégat inconnu.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement (catalogue, groupable = true).", example = "commercialNom",
                    required = true)
            @RequestParam String groupBy,
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (défaut) ou NOMBRE (effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @ParameterObject @ModelAttribute FiltresSuiviCommerciaux filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneSuiviCommerciaux groupe = Colonnes.resoudreGroupement(ColonneSuiviCommerciaux.class, groupBy);
        return service.grouper(filtres, groupe, Groupements.granularite(groupe, granularite),
                Colonnes.resoudreAgregats(ColonneSuiviCommerciaux.class, agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping(BASE + "/totaux-colonnes")
    @Operation(summary = "Effectif de toutes les interactions du filtre, pas d'une page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Effectif du filtre et un résumé par jeton demandé."),
            @ApiResponse(responseCode = "400", description = "Jeton d'agregats inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public TotauxListeDto totauxColonnes(
            @RequestParam(required = false) List<String> agregats,
            @ParameterObject @ModelAttribute FiltresSuiviCommerciaux filtres) {
        return service.totauxColonnes(filtres, agregats);
    }

    @GetMapping(BASE + "/export")
    @Operation(summary = "Export Excel des interactions, filtres et colonnes appliqués")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "dateSaisie,desc",
            description = "Tri `colonne,asc|desc`, répétable, comme la liste. Défaut : dateSaisie,desc.",
            array = @ArraySchema(schema = @Schema(type = "string")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classeur .xlsx, feuille DONNEES.",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresSuiviCommerciaux filtres,
            @Parameter(example = "jour,commercialNom,type,resultat,joint,personneNom")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = TRI_DEFAUT, direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneSuiviCommerciaux> selection = selection(colonnes);
        List<LigneSuiviCommerciauxDto> lignes = LignesExport.toutes(sort, page -> service.lister(filtres, page));
        return exportTableur.reponse("suivi-commerciaux", "Suivi des commerciaux",
                LignesExport.ordre(colonnes, selection), lignes);
    }

    @GetMapping(BASE + "/mensuel")
    @Operation(summary = "Vue OMRAN mois / année : prospections, visites, ventes, versements et ratios par commercial")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Blocs mois et année, chacun avec Total et Moyenne."),
            @ApiResponse(responseCode = "400", description = "Mois hors 1..12 ou id mal formé.", content = @Content),
            @ApiResponse(responseCode = "403", description = HORS_PORTEE, content = @Content)
    })
    public MensuelSuiviCommerciauxDto mensuel(
            @Parameter(example = "2026") @RequestParam int annee,
            @Parameter(description = "1 à 12.", example = "10") @RequestParam int mois,
            @Parameter(description = "Tranche(s), répétable ; absent = tranches habilitées de l'appelant.")
            @RequestParam(required = false) List<UUID> trancheIds,
            @Parameter(description = COMMERCIAL_IDS)
            @RequestParam(required = false) List<UUID> commercialIds) {
        return mensuelService.mensuel(annee, mois, trancheIds, commercialIds);
    }

    @GetMapping(BASE + "/objectif")
    @Operation(summary = "Objectif de personnes jointes par jour (60 par défaut)")
    public ObjectifCommercialDto objectif() {
        return new ObjectifCommercialDto(objectifService.objectif());
    }

    @PutMapping(BASE + "/objectif")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(summary = "Régler l'objectif de personnes jointes par jour (Administrateur)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Objectif enregistré."),
            @ApiResponse(responseCode = "400", description = "Valeur absente ou hors 1..1000.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Appelant non Administrateur.", content = @Content)
    })
    public ObjectifCommercialDto modifierObjectif(@Valid @RequestBody ObjectifCommercialDto corps) {
        return new ObjectifCommercialDto(objectifService.modifierObjectif(corps.objectifPersonnesJointesJour()));
    }

    @GetMapping("/api/referentiels/colonnes/suivi-commerciaux")
    @Operation(summary = "Catalogue des colonnes de la liste du suivi des commerciaux")
    public List<ColonneDto> colonnes() {
        return Arrays.stream(ColonneSuiviCommerciaux.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), "INTERACTION", c.type().name(), c.parDefaut(), c.chemin()))
                .toList();
    }

    /** @throws com.tsix.immocrm.domain.ColonneInconnueException nom hors catalogue (400) */
    private static Set<ColonneSuiviCommerciaux> selection(List<String> colonnes) {
        return Colonnes.resoudre(ColonneSuiviCommerciaux.class, colonnes, ColonneSuiviCommerciaux.ID);
    }
}
