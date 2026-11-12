package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.CreerLotRequest;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.DemandeAchatLotDto;
import com.tsix.immocrm.dto.projet.ExecutionTransitionLotDto;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.LotListItemDto;
import com.tsix.immocrm.dto.projet.ModifierLotRequest;
import com.tsix.immocrm.dto.projet.ReferentielsProjetDto;
import com.tsix.immocrm.dto.projet.SuiviLotDto;
import com.tsix.immocrm.dto.projet.TotauxLotsDto;
import com.tsix.immocrm.dto.projet.TransitionLotDto;
import com.tsix.immocrm.services.projet.CopieLot;
import com.tsix.immocrm.services.projet.EcrituresLotService;
import com.tsix.immocrm.services.projet.FiltresLots;
import com.tsix.immocrm.services.projet.LotsProjetService;
import com.tsix.immocrm.services.projet.ModificationLot;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Projet")
@RestController
@RequestMapping("/api/projet")
public class LotsProjetController {

    private static final String ID_LOT = "Identifiant du lot (PrjMarket.PKey).";
    private static final String LOT_INTROUVABLE = "Lot introuvable : id inconnu, lot sans projet, projet hors périmètre ou statut invisible.";

    private static final String FILTRE_MAL_FORME = "Filtre mal formé (UUID).";
    // Colonnes de la liste OMRAN (MarketMonitoringList.cs:grid_InitializeLayout_1), dans son ordre.
    private static final List<ColonneFixe> COLONNES_EXPORT = List.of(ColonneFixe.texte("code", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("fournisseurNom", "Fournisseur"),
            ColonneFixe.de("avancement", "Avancement", TypeExport.NOMBRE),
            ColonneFixe.de("coutPrevisionnelHT", "Coût Prév. HT", TypeExport.MONTANT),
            ColonneFixe.de("coutPrevisionnelTTC", "Coût Prév. TTC", TypeExport.MONTANT),
            ColonneFixe.de("coutPrevisionnelNetAPayer", "Net à payer", TypeExport.MONTANT),
            ColonneFixe.de("coutPrevisionnelRegle", "Réglé", TypeExport.MONTANT),
            ColonneFixe.de("coutPrevisionnelReste", "Reste", TypeExport.MONTANT),
            ColonneFixe.texte("notes", "Autres Notes"), ColonneFixe.texte("statutDescription", "Statut"));

    private final LotsProjetService service;
    private final EcrituresLotService ecritures;
    private final ModificationLot modification;
    private final CopieLot copie;
    private final ExportTableur exportTableur;

    public LotsProjetController(LotsProjetService service, EcrituresLotService ecritures, ModificationLot modification,
                                CopieLot copie, ExportTableur exportTableur) {
        this.service = service;
        this.ecritures = ecritures;
        this.modification = modification;
        this.copie = copie;
        this.exportTableur = exportTableur;
    }

    @GetMapping("/referentiels")
    @Operation(summary = "Listes de choix des filtres Lots et Décomptes")
    public ReferentielsProjetDto referentiels() {
        return service.referentiels();
    }

    @GetMapping("/lots")
    @Operation(summary = "Liste paginée des lots des projets visibles")
    public PageDto<LotListItemDto> lots(
            @ParameterObject @ModelAttribute FiltresLots filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(service.lots(filtres, pageable));
    }

    @GetMapping("/lots/totaux")
    @Operation(summary = "Totaux de la liste des lots : mêmes filtres, toutes pages")
    @ApiResponse(responseCode = "200", description = "Totaux ; à zéro si aucun lot n'est visible.")
    @ApiResponse(responseCode = "400", description = FILTRE_MAL_FORME, content = @Content)
    public TotauxLotsDto totaux(@ParameterObject @ModelAttribute FiltresLots filtres) {
        return service.totaux(filtres);
    }

    @GetMapping("/lots/export")
    @Operation(summary = "Export Excel de la liste des lots, mêmes filtres et lignes que la liste, ligne Total en bas")
    @Parameter(name = "sort", in = ParameterIn.QUERY, example = "code,desc",
            description = "Tri `propriete,asc|desc`, répétable, comme la liste.")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx (feuille DONNEES).",
            content = @Content(schema = @Schema(type = "string", format = "binary"),
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @ApiResponse(responseCode = "400", description = "Filtre mal formé ou format autre que xlsx.", content = @Content)
    @ApiResponse(responseCode = "403", description = "Droit OMRAN MarketMonitoringList.toolExport absent.", content = @Content)
    public ResponseEntity<StreamingResponseBody> exporter(
            @Parameter(example = ExportTableur.FORMAT_XLSX)
            @RequestParam(defaultValue = ExportTableur.FORMAT_XLSX) String format,
            @ParameterObject @ModelAttribute FiltresLots filtres,
            @Parameter(hidden = true) Sort sort) {
        ExportTableur.exigerXlsx(format);
        return exportTableur.reponse("lots", "Lots", List.of(service.feuilleExport(filtres, sort, COLONNES_EXPORT)));
    }

    @PostMapping("/lots")
    @Operation(summary = "Créer un lot (fiche minimale)")
    @ApiResponse(responseCode = "201", description = "Lot créé ; en-tête Location = sa fiche.")
    @ApiResponse(responseCode = "400", description = "Champ obligatoire absent, genre inconnu, catégorie ou type inexistant.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "Droit de création du genre absent (contrôlé avant le corps).",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Projet inconnu ou hors périmètre de l'appelant.", content = @Content)
    public ResponseEntity<LotDetailDto> creer(@RequestBody(required = false) CreerLotRequest requete) {
        LotDetailDto lot = ecritures.creer(requete);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(lot.lot().id()).toUri()).body(lot);
    }

    @DeleteMapping("/lots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un lot")
    @ApiResponse(responseCode = "204", description = "Lot et enfants en cascade supprimés.")
    @ApiResponse(responseCode = "403", description = "Droit OMRAN MarketMonitoringList.toolDelete absent, même sur un id inconnu.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Lot utilisé ailleurs (décomptes, achats…).", content = @Content)
    public void supprimer(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        ecritures.supprimer(id);
    }

    @GetMapping("/lots/{id}/transitions")
    @Operation(summary = "Transitions de statut exécutables par l'appelant depuis le statut actuel du lot")
    @ApiResponse(responseCode = "200", description = "Transitions (éventuellement vide).")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public List<TransitionLotDto> transitions(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.transitions(id);
    }

    @PostMapping("/lots/{id}/transition")
    @Operation(summary = "Exécuter une transition de statut du lot")
    @ApiResponse(responseCode = "200", description = "Ligne de liste du lot au nouveau statut.")
    @ApiResponse(responseCode = "400", description = "transitionId absent, ou commentaire exigé par la transition absent.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Transition absente de GET …/transitions pour l'appelant.",
            content = @Content)
    public LotListItemDto executerTransition(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                             @RequestBody ExecutionTransitionLotDto requete) {
        return ecritures.executerTransition(id, requete);
    }

    @GetMapping("/lots/{id}")
    @Operation(summary = "Fiche d'un lot")
    @ApiResponse(responseCode = "200", description = "Fiche du lot.")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public LotDetailDto lot(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.lot(id);
    }

    @PatchMapping("/lots/{id}")
    @Operation(summary = "Enregistrer la fiche d'un lot (champ absent = inchangé, null = vidé)")
    @ApiResponse(responseCode = "200", description = "Fiche du lot enregistrée.")
    @ApiResponse(responseCode = "400", description = "Champ non modifiable, valeur invalide, référence inconnue ou dates hors ordre.",
            content = @Content)
    @ApiResponse(responseCode = "403", description = "Estimation manuelle changée sans le droit ManualEstimationAdministrator.",
            content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    @ApiResponse(responseCode = "409", description = "Statut du lot en lecture seule (StrStatus.IsReadOnly).", content = @Content)
    public LotDetailDto modifier(@Parameter(description = ID_LOT) @PathVariable UUID id,
                                 @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                         content = @Content(schema = @Schema(implementation = ModifierLotRequest.class)))
                                 @RequestBody JsonNode corps) {
        return modification.modifier(id, corps);
    }

    @PostMapping("/lots/{id}/copie")
    @Operation(summary = "Copier un lot : en-tête, lignes de bordereau et leurs groupes")
    @ApiResponse(responseCode = "201", description = "Copie créée ; en-tête Location = sa fiche.")
    @ApiResponse(responseCode = "403", description = "Droit de création du genre du lot absent (aucun des deux : avant le 404).",
            content = @Content)
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public ResponseEntity<LotDetailDto> copier(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        LotDetailDto copieLot = this.copie.copier(id);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/projet/lots/{id}")
                .buildAndExpand(copieLot.lot().id()).toUri()).body(copieLot);
    }

    @GetMapping("/lots/{id}/demandes-achat")
    @Operation(summary = "Demandes internes d'achat du lot, par date décroissante")
    @ApiResponse(responseCode = "200", description = "Demandes du lot (éventuellement vide).")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public List<DemandeAchatLotDto> demandesAchat(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.demandesAchat(id);
    }

    @GetMapping("/lots/{id}/suivi")
    @Operation(summary = "Onglets Suivi et Retenues de la fiche d'un lot")
    @ApiResponse(responseCode = "200", description = "Colonnes, avances, retenue de garantie et retenues du lot.")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public SuiviLotDto suivi(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.suivi(id);
    }

    @GetMapping("/lots/{id}/bordereau")
    @Operation(summary = "Bordereau définitif d'un lot")
    @ApiResponse(responseCode = "200", description = "Lignes du bordereau et leurs totaux.")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public BordereauDto bordereau(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.bordereau(id);
    }

    @GetMapping("/lots/{id}/decomptes")
    @Operation(summary = "Décomptes d'un lot, par numéro croissant")
    @ApiResponse(responseCode = "200", description = "Décomptes du lot (éventuellement vide).")
    @ApiResponse(responseCode = "404", description = LOT_INTROUVABLE, content = @Content)
    public List<DecompteListItemDto> decomptesDuLot(@Parameter(description = ID_LOT) @PathVariable UUID id) {
        return service.decomptesDuLot(id);
    }
}
