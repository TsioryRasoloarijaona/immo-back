package com.tsix.immocrm.controller.suivi;

import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import com.tsix.immocrm.services.suivi.SuiviActionService;
import com.tsix.immocrm.services.suivi.SuiviActionService.Filtres;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Suivi des actions")
@RestController
@RequestMapping("/api/suivi-actions")
public class SuiviActionController {

    private final SuiviActionService service;
    private final ExportTableur exportTableur;

    public SuiviActionController(SuiviActionService service, ExportTableur exportTableur) {
        this.service = service;
        this.exportTableur = exportTableur;
    }

    @GetMapping
    @Operation(
            summary = "Liste des actions commerciales, colonnes au choix",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page d'actions"),
                    @ApiResponse(responseCode = "400", description = "Nom de colonne hors catalogue "
                            + "(le corps liste les noms valides) ; `groupeColonne` non groupable.",
                            content = @Content)
            })
    public PageDto<SuiviActionDto> findAll(
            @Parameter(description = "Début de période, **inclus** (`DocDate >= dateDebut`), ISO-8601.",
                    example = "2026-09-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Fin de période, **exclue** (`DocDate < dateFin`), ISO-8601 — comme "
                    + "la requête OMRAN.", example = "2026-10-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(example = "Khadija ELKORRI")
            @RequestParam(required = false) List<String> utilisateur,
            @Parameter(description = "Type(s) d'action (ComActionMessageType.id). Répétable, OU.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Catégorie(s) (ComActionMessageCategory.id). Répétable, OU.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "`true` = actions système seules, `false` = saisies seules, absent "
                    + "= tout (comme OMRAN).")
            @RequestParam(required = false) Boolean systeme,
            @Parameter(description = "Colonnes à servir, répétable ou séparées par des virgules. Absent : "
                    + "la grille OMRAN. `id` est toujours servi.",
                    example = "utilisateur,typeAction,dateAction,tiersNom,prospectionCode,trancheCode")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(description = "Déplie un groupe renvoyé par `/groupes` : le nom de la colonne "
                    + "groupée.", example = "dateAction")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(description = "La `cle` du groupe, telle que renvoyée par `/groupes`. Omise avec "
                    + "`groupeColonne` : le groupe sans valeur.")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date — la même qu'à "
                    + "`/groupes`. Défaut : MOIS.", example = "JOUR")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneSuiviAction groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneSuiviAction::resoudreGroupement);
        return PageDto.of(service.findAll(
                new Filtres(dateDebut, dateFin, trancheId, utilisateur, typeId, categorieId, systeme),
                groupe, groupeValeur, Groupements.granularite(groupe, granularite),
                ColonneSuiviAction.resoudre(colonnes), pageable));
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel du suivi des actions, filtres et colonnes appliqués")
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) List<String> utilisateur,
            @RequestParam(required = false) List<UUID> typeId,
            @RequestParam(required = false) List<UUID> categorieId,
            @RequestParam(required = false) Boolean systeme,
            @Parameter(example = "utilisateur,typeAction,dateAction,tiersNom")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(hidden = true) @SortDefault(sort = "docDate", direction = Sort.Direction.DESC) Sort sort) {
        ExportTableur.exigerXlsx(format);
        Set<ColonneSuiviAction> selection = ColonneSuiviAction.resoudre(colonnes);
        Filtres filtres = new Filtres(dateDebut, dateFin, trancheId, utilisateur, typeId, categorieId, systeme);
        List<SuiviActionDto> lignes = LignesExport.toutes(sort, page ->
                service.findAll(filtres, null, null, null, selection, page));
        return exportTableur.reponse("suivi-actions", "Suivi des actions", LignesExport.ordre(colonnes, selection),
                lignes);
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Actions repliées en groupes, avec effectifs — les comptes par jour, par utilisateur…",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", description = "`groupBy` absent, inconnu ou non "
                            + "groupable (`rang`, `alerteActive`, `etat`, `personnel`, `message`) ; "
                            + "`agregats` fourni.", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau.",
                    example = "dateAction", required = true)
            @RequestParam String groupBy,
            @Parameter(description = "Aucune colonne de ce catalogue n'est agrégeable : laisser vide.")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité si `groupBy` est une date. **JOUR** pour les comptes "
                    + "quotidiens. Défaut : MOIS.", example = "JOUR")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (croissant, défaut) ou NOMBRE (effectif décroissant).")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(description = "Inclure le groupe des actions sans valeur sur la colonne groupée. "
                    + "Faux par défaut.", example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) List<String> utilisateur,
            @RequestParam(required = false) List<UUID> typeId,
            @RequestParam(required = false) List<UUID> categorieId,
            @RequestParam(required = false) Boolean systeme,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneSuiviAction groupe = ColonneSuiviAction.resoudreGroupement(groupBy);
        return service.grouper(
                new Filtres(dateDebut, dateFin, trancheId, utilisateur, typeId, categorieId, systeme),
                groupe, Groupements.granularite(groupe, granularite),
                ColonneSuiviAction.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }
}
