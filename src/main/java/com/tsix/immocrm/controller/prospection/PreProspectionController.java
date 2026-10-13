package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.StatutLigneImport;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospection.CreerPreProspectionRequest;
import com.tsix.immocrm.dto.prospection.LierProspectRequest;
import com.tsix.immocrm.dto.prospection.PreProspectionCreeeDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.RapportImportPreProspectionsDto;
import com.tsix.immocrm.services.prospection.ImportPreProspectionService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.web.Groupements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Pré-prospections")
@RestController
@RequestMapping("/api/pre-prospections")
public class PreProspectionController {

    private final ProspectionService prospectionService;
    private final ImportPreProspectionService importService;

    public PreProspectionController(ProspectionService prospectionService,
                                    ImportPreProspectionService importService) {
        this.prospectionService = prospectionService;
        this.importService = importService;
    }


    @GetMapping
    @Operation(
            summary = "Liste paginée des pré-prospections"
    )
    public PageDto<ProspectionListItemDto> findAll(
            @Parameter(description = "Code(s) ComProspectionKind (\"Nature\"/étape) à filtrer. "
                    + "Répéter le paramètre pour plusieurs étapes.")
            @RequestParam(required = false) List<String> etape,
            @Parameter(description = "Code(s) ComProspectionOrigin à filtrer. Répéter le "
                    + "paramètre pour plusieurs origines.")
            @RequestParam(required = false) List<String> origine,
            @Parameter(description = "Id(s) du commercial (CfgTier.id, cfgCommercial). Répéter "
                    + "le paramètre pour plusieurs commerciaux.")
            @RequestParam(required = false) List<UUID> commercial,
            @Parameter(description = "Id(s) de tranche visée (CfgTranche.id, "
                    + "ComProspection.stkHierarchy). Répéter le paramètre pour plusieurs tranches.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id(s) de projet (CfgProject.id) de la tranche ; une ligne sans tranche est "
                    + "écartée. Répétable.")
            @RequestParam(required = false) List<UUID> projetId,
            @Parameter(description = "Borne inférieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @Parameter(description = "Déplie un groupe : restreint la liste aux pré-prospections "
                    + "d'un groupe renvoyé par `GET /api/pre-prospections/groupes`.",
                    example = "origine")
            @RequestParam(required = false) String groupeColonne,
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date. Défaut : MOIS.")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneProspection groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneProspection::resoudreGroupement);
        return PageDto.of(prospectionService.findPreProspections(etape, origine, commercial, trancheId, projetId, dateDebut,
                dateFin, search, ouvertes, groupe, groupeValeur,
                Groupements.granularite(groupe, granularite), pageable));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des pré-prospections repliée en groupes, avec effectifs et totaux",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", description =
                            "`groupBy` absent, inconnu ou non groupable ; ou `agregats` contenant "
                            + "une colonne non agrégeable.", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau.",
                    example = "origine", required = true)
            @RequestParam String groupBy,
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité si `groupBy` est une date. Défaut : MOIS.")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (croissant, défaut) ou NOMBRE (effectif décroissant).")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @Parameter(example = "true")
            @RequestParam(required = false) Boolean ouvertes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneProspection groupe = ColonneProspection.resoudreGroupement(groupBy);
        return prospectionService.grouperPreProspections(etape, origine, commercial, trancheId,
                dateDebut, dateFin, search, ouvertes, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneProspection.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @PostMapping
    @Operation(
            summary = "Crée une pré-prospection (lead brut)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pré-prospection créée ; `doublons` vide "
                    + "si aucun contact identique n'existe."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "La tranche fournie appartient à un projet "
                    + "hors du périmètre commercial de l'appelant.", content = @Content)
    })
    public ResponseEntity<PreProspectionCreeeDto> creer(@RequestBody CreerPreProspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerPreProspection(request));
    }

    @PostMapping(value = "/import/apercu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import CSV de leads — étape 1 : aperçu (rien n'est écrit)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rapport d'aperçu (`importe = false`, "
                    + "`preProspectionId` toujours null)."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide, colonne `Nom` "
                    + "introuvable, plus de 5 000 lignes, fichier de plus de 5 Mo.", content = @Content)
    })
    public RapportImportPreProspectionsDto apercuImport(
            @Parameter(description = "Le fichier CSV exporté, tel quel.")
            @RequestParam("fichier") MultipartFile fichier) {
        return importService.apercu(fichier);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import CSV de leads — étape 2 : création des statuts choisis, sur une seule tranche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import effectué ; rapport ligne par ligne."),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "La tranche appartient à un projet hors "
                    + "du périmètre commercial de l'appelant — rien n'est écrit.", content = @Content)
    })
    public RapportImportPreProspectionsDto importer(
            @Parameter(description = "Le fichier CSV exporté — le même que pour l'aperçu.")
            @RequestParam("fichier") MultipartFile fichier,
            @RequestParam(required = false) UUID trancheId,
            @Parameter(description = "Nature (ComProspectionKind). Défaut : `1 A TRAITER`.")
            @RequestParam(required = false) UUID etapeId,
            @Parameter(description = "Source de contact (ComProspectionOrigin). Défaut : `FACEBOOK`.")
            @RequestParam(required = false) UUID origineId,
            @Parameter(description = "Support d'origine (ComProspectionOriginSupport). Défaut : aucun.")
            @RequestParam(required = false) UUID origineSupportId,
            @Parameter(description = "Type (ComActionMessageType). Défaut : `WEB`.")
            @RequestParam(required = false) UUID typeContactId,
            @Parameter(description = "Action commerciale / campagne (ComAction). Défaut : aucune.")
            @RequestParam(required = false) UUID actionCommercialeId,
            @RequestParam(required = false) List<StatutLigneImport> statuts) {
        return importService.importer(fichier, trancheId, new ImportPreProspectionService.Rattachement(
                etapeId, origineId, origineSupportId, typeContactId, actionCommercialeId), statuts);
    }

    @PostMapping("/{id}/creer-prospect")
    @Operation(
            summary = "Crée la fiche prospect d'une pré-prospection et la lui rattache"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospect créé et rattaché."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "409")
    })
    public ResponseEntity<ProspectionDetailDto> creerProspect(
            @Parameter(description = "Identifiant de la pré-prospection (ComProspection.PKey).")
            @PathVariable UUID id,
            @RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerProspect(id, request));
    }

    @PostMapping("/{id}/lier-prospect")
    @Operation(
            summary = "Rattache un prospect existant à une pré-prospection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Aucune pré-prospection avec cet id, "
                    + "ou pré-prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409")
    })
    public ProspectionDetailDto lierProspect(
            @Parameter(description = "Identifiant de la pré-prospection (ComProspection.PKey).")
            @PathVariable UUID id,
            @RequestBody LierProspectRequest request) {
        return prospectionService.lierProspect(id, request);
    }
}
