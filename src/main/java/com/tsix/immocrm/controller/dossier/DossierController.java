package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.EtapeSuivanteDto;
import com.tsix.immocrm.dto.dossier.FicheBancaireData;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData;
import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData;
import com.tsix.immocrm.dto.dossier.FicheVisiteTechniqueData;
import com.tsix.immocrm.dto.dossier.GenererDocumentsLotRequest;
import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.ModifierStatutReglementRequest;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.PayeurDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.dossier.RecuReglementData;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.dto.dossier.SchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.SimulerSchemaFinancementRequest;
import com.tsix.immocrm.dto.dossier.TransitionSuggereeDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.dossier.AutorisationVersementService;
import com.tsix.immocrm.services.dossier.DocumentLotService;
import com.tsix.immocrm.services.dossier.DossierHistoriqueService;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.dossier.EcheancierService;
import com.tsix.immocrm.services.dossier.FicheBancaireService;
import com.tsix.immocrm.services.dossier.FicheDossierVenteService;
import com.tsix.immocrm.services.dossier.FicheReservationService;
import com.tsix.immocrm.services.dossier.FicheTropPercuService;
import com.tsix.immocrm.services.dossier.FicheVisiteTechniqueService;
import com.tsix.immocrm.services.dossier.PayeurService;
import com.tsix.immocrm.services.dossier.PieceJointeService;
import com.tsix.immocrm.services.dossier.RecuReglementService;
import com.tsix.immocrm.services.dossier.ReglementPieceJointeService;
import com.tsix.immocrm.services.dossier.ReglementStatutService;
import com.tsix.immocrm.services.dossier.document.ActeReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.AutorisationVersementPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheBancairePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheDossierVentePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheTropPercuPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheVisiteTechniquePdfRenderer;
import com.tsix.immocrm.services.dossier.document.RecuReglementPdfRenderer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Dossiers")
@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierService dossierService;
    private final EcheancierService echeancierService;
    private final PieceJointeService pieceJointeService;
    private final FicheReservationService ficheReservationService;
    private final FicheReservationPdfRenderer ficheReservationPdfRenderer;
    private final FicheReservationDocxRenderer ficheReservationDocxRenderer;
    private final ActeReservationService acteReservationService;
    private final ActeReservationPdfRenderer acteReservationPdfRenderer;
    private final ActeReservationDocxRenderer acteReservationDocxRenderer;
    private final FicheBancaireService ficheBancaireService;
    private final FicheBancairePdfRenderer ficheBancairePdfRenderer;
    private final FicheVisiteTechniqueService ficheVisiteTechniqueService;
    private final FicheVisiteTechniquePdfRenderer ficheVisiteTechniquePdfRenderer;
    private final FicheDossierVenteService ficheDossierVenteService;
    private final FicheDossierVentePdfRenderer ficheDossierVentePdfRenderer;
    private final FicheTropPercuService ficheTropPercuService;
    private final FicheTropPercuPdfRenderer ficheTropPercuPdfRenderer;
    private final RecuReglementService recuReglementService;
    private final RecuReglementPdfRenderer recuReglementPdfRenderer;
    private final AutorisationVersementService autorisationVersementService;
    private final AutorisationVersementPdfRenderer autorisationVersementPdfRenderer;
    private final DossierHistoriqueService historiqueService;
    private final DocumentLotService documentLotService;
    private final ReglementStatutService reglementStatutService;
    private final ReglementPieceJointeService reglementPieceJointeService;
    private final PayeurService payeurService;

    public DossierController(DossierService dossierService, EcheancierService echeancierService,
                              PieceJointeService pieceJointeService, FicheReservationService ficheReservationService,
                              FicheReservationPdfRenderer ficheReservationPdfRenderer,
                              FicheReservationDocxRenderer ficheReservationDocxRenderer,
                              ActeReservationService acteReservationService,
                              ActeReservationPdfRenderer acteReservationPdfRenderer,
                              ActeReservationDocxRenderer acteReservationDocxRenderer,
                              FicheBancaireService ficheBancaireService,
                              FicheBancairePdfRenderer ficheBancairePdfRenderer,
                              FicheVisiteTechniqueService ficheVisiteTechniqueService,
                              FicheVisiteTechniquePdfRenderer ficheVisiteTechniquePdfRenderer,
                              FicheDossierVenteService ficheDossierVenteService,
                              FicheDossierVentePdfRenderer ficheDossierVentePdfRenderer,
                              FicheTropPercuService ficheTropPercuService,
                              FicheTropPercuPdfRenderer ficheTropPercuPdfRenderer,
                              RecuReglementService recuReglementService,
                              RecuReglementPdfRenderer recuReglementPdfRenderer,
                              AutorisationVersementService autorisationVersementService,
                              AutorisationVersementPdfRenderer autorisationVersementPdfRenderer,
                              DossierHistoriqueService historiqueService,
                              DocumentLotService documentLotService,
                              ReglementStatutService reglementStatutService,
                              ReglementPieceJointeService reglementPieceJointeService,
                              PayeurService payeurService) {
        this.dossierService = dossierService;
        this.echeancierService = echeancierService;
        this.pieceJointeService = pieceJointeService;
        this.ficheReservationService = ficheReservationService;
        this.ficheReservationPdfRenderer = ficheReservationPdfRenderer;
        this.ficheReservationDocxRenderer = ficheReservationDocxRenderer;
        this.acteReservationService = acteReservationService;
        this.acteReservationPdfRenderer = acteReservationPdfRenderer;
        this.acteReservationDocxRenderer = acteReservationDocxRenderer;
        this.ficheBancaireService = ficheBancaireService;
        this.ficheBancairePdfRenderer = ficheBancairePdfRenderer;
        this.ficheVisiteTechniqueService = ficheVisiteTechniqueService;
        this.ficheVisiteTechniquePdfRenderer = ficheVisiteTechniquePdfRenderer;
        this.ficheDossierVenteService = ficheDossierVenteService;
        this.ficheDossierVentePdfRenderer = ficheDossierVentePdfRenderer;
        this.ficheTropPercuService = ficheTropPercuService;
        this.ficheTropPercuPdfRenderer = ficheTropPercuPdfRenderer;
        this.recuReglementService = recuReglementService;
        this.recuReglementPdfRenderer = recuReglementPdfRenderer;
        this.autorisationVersementService = autorisationVersementService;
        this.autorisationVersementPdfRenderer = autorisationVersementPdfRenderer;
        this.historiqueService = historiqueService;
        this.documentLotService = documentLotService;
        this.reglementStatutService = reglementStatutService;
        this.reglementPieceJointeService = reglementPieceJointeService;
        this.payeurService = payeurService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un dossier de vente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier trouvé."),
            @ApiResponse(responseCode = "404")
    })
    public DossierDetailDto findById(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return dossierService.findById(id);
    }

    @PostMapping("/simulation-financement")
    @Operation(summary = "Prévisualise le schéma de financement (étape avant création du dossier)")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400")
    })
    public SchemaFinancementDto simulerSchemaFinancement(@RequestBody SimulerSchemaFinancementRequest request) {
        return dossierService.simulerSchemaFinancement(request);
    }

    @PostMapping
    @Operation(summary = "Crée un dossier de vente à partir d'une prospection")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dossier créé."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403", description = "Le projet de l'article n'est pas dans "
                    + "le périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "500")
    })
    public ResponseEntity<DossierDetailDto> create(@RequestBody CreerDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.create(request));
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Change le statut du dossier de vente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut changé."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "403"),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "409")
    })
    public DossierDetailDto changerStatut(@PathVariable UUID id, @RequestBody ChangerStatutRequest request) {
        return dossierService.changerStatut(id, request);
    }

    @GetMapping("/{id}/statut/etapes-suivantes")
    @Operation(summary = "Statuts que l'utilisateur connecté peut donner au dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transitions exécutables, vide si aucune."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public List<EtapeSuivanteDto> getEtapesSuivantes(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).") @PathVariable UUID id) {
        return dossierService.etapesSuivantes(id);
    }

    @GetMapping("/{id}/statut/transition-suggeree")
    @Operation(summary = "Passage automatique de statut proposé")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proposition, champs nuls s'il n'y en a pas."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public TransitionSuggereeDto getTransitionSuggeree(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).") @PathVariable UUID id) {
        return dossierService.transitionSuggeree(id);
    }

    @GetMapping("/{id}/historique")
    public PageDto<HistoriqueEvenementDto> getHistorique(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(historiqueService.findHistorique(id, pageable));
    }

    @GetMapping("/{id}/echeancier")
    public PageDto<EcheanceDto> findEcheances(@PathVariable UUID id,
                                                @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(echeancierService.findEcheances(id, pageable));
    }

    @PostMapping("/{id}/echeancier")
    @Operation(summary = "Ajoute une ligne d'échéancier au dossier",
            description = "Société, module, TVA, HT et date à minuit sont écrits comme le logiciel de bureau.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Échéance créée."),
            @ApiResponse(responseCode = "400", description = "Catégorie (`categorieId`) absente ou introuvable."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public ResponseEntity<EcheanceDto> addEcheance(@PathVariable UUID id, @RequestBody CreerEcheanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addEcheance(id, request));
    }

    @PatchMapping("/{id}/echeancier/{echeanceId}")
    @Operation(summary = "Modifie une ligne d'échéancier",
            description = "Champs absents inchangés ; HT, TVA et société recalculés, la ligne garde une catégorie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Échéance modifiée."),
            @ApiResponse(responseCode = "400", description = "Catégorie introuvable, ou ligne laissée sans catégorie."),
            @ApiResponse(responseCode = "404", description = "Dossier ou échéance introuvable.")
    })
    public EcheanceDto updateEcheance(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                       @RequestBody ModifierEcheanceRequest request) {
        return echeancierService.updateEcheance(id, echeanceId, request);
    }

    @PostMapping("/{id}/echeancier/{echeanceId}/reglements")
    @Operation(summary = "Enregistre un règlement sur une échéance")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Règlement enregistré, au statut `IN_WORK`."),
            @ApiResponse(responseCode = "400"),
            @ApiResponse(responseCode = "404", description = "Dossier ou échéance introuvable "
                    + "(ou l'échéance n'appartient pas à ce dossier).")
    })
    public ResponseEntity<ReglementDto> addReglement(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                                       @RequestBody CreerReglementRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(echeancierService.addReglement(id, echeanceId, request, principal));
    }

    @GetMapping("/{id}/echeancier/{echeanceId}/reglements")
    @Operation(summary = "Historique complet des règlements d'une échéance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des règlements (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier ou échéance introuvable (ou l'échéance n'appartient pas à ce dossier).")
    })
    public List<ReglementDto> getReglements(@PathVariable UUID id, @PathVariable UUID echeanceId) {
        return echeancierService.findReglements(id, echeanceId);
    }

    @GetMapping("/{id}/montants")
    @Operation(summary = "Synthèse financière du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Aucun dossier avec cet id, ou dossier "
                    + "hors du périmètre commercial de l'appelant.")
    })
    public MontantsDossierDto montants(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return dossierService.montants(id);
    }

    // -- Pièces jointes (AdmAttachedFile, liaison décrite dans PieceJointeService) --

    @GetMapping("/{id}/pieces-jointes")
    @Operation(summary = "Liste les pièces jointes du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des pièces jointes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public List<PieceJointeDto> getPiecesJointes(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return pieceJointeService.findByDossier(id);
    }

    @PostMapping(value = "/{id}/pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Dépose une pièce jointe sur le dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pièce jointe créée."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, ou typeId ne correspond à aucun type connu.")
    })
    public ResponseEntity<PieceJointeDto> uploadPieceJointe(
            @PathVariable UUID id,
            @Parameter(description = "Contenu du fichier à déposer.")
            @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Légende libre. Absent = nom de fichier original.")
            @RequestParam(required = false) String description,
            @Parameter(description = "Type de document (AdmAttachedFileType.PKey), voir GET /api/referentiels/types-piece-jointe. Absent = non renseigné.")
            @RequestParam(required = false) UUID typeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        PieceJointeDto piece = pieceJointeService.create(id, fichier, description, typeId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(piece);
    }

    @GetMapping("/{id}/pieces-jointes/{pieceId}/fichier")
    @Operation(summary = "Affiche ou télécharge une pièce jointe du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, pièce jointe introuvable "
                    + "pour ce dossier, ou aucun fichier stocké pour cette pièce.")
    })
    public ResponseEntity<Void> getFichier(@PathVariable UUID id, @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(pieceJointeService.urlFichier(id, pieceId))
                .build();
    }

    @DeleteMapping("/{id}/pieces-jointes/{pieceId}")
    @Operation(summary = "Supprime une pièce jointe du dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pièce jointe supprimée."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, pièce jointe "
                    + "introuvable pour ce dossier, ou pièce déjà supprimée.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePieceJointe(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey), tel que renvoyé par `PieceJointeDto.id`.")
            @PathVariable UUID pieceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        pieceJointeService.supprimer(id, pieceId, principal);
    }

    @GetMapping("/{id}/payeurs")
    @Operation(summary = "Payeurs possibles d'un règlement de ce dossier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payeurs possibles (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public List<PayeurDto> getPayeurs(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return payeurService.findByDossier(id);
    }

    // -- Règlements : statut et preuves de paiement --

    @PatchMapping("/{id}/reglements/{reglementId}/statut")
    @Operation(summary = "Change le statut d'un règlement (pointage / annulation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié ; le règlement à jour est renvoyé."),
            @ApiResponse(responseCode = "400", description = "`statut` absent ou inconnu, ou "
                    + "`dateReglement` absente alors que la cible est `POINTED`."),
            @ApiResponse(responseCode = "404", description = "Dossier ou règlement introuvable, ou "
                    + "règlement non rattaché à ce dossier."),
            @ApiResponse(responseCode = "409", description = "Transition interdite (corps : "
                    + "`transitionsAutorisees`), ou passage à `POINTED` sans aucune pièce jointe.")
    })
    public ReglementDto modifierStatutReglement(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Identifiant du règlement (StlSettlement.PKey), tel que renvoyé par `ReglementDto.id`.")
            @PathVariable UUID reglementId,
            @RequestBody ModifierStatutReglementRequest request) {
        return reglementStatutService.modifierStatut(id, reglementId, request);
    }

    @GetMapping("/{id}/reglements/{reglementId}/pieces-jointes")
    @Operation(summary = "Liste les preuves de paiement d'un règlement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des pièces jointes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier ou règlement introuvable, ou règlement non rattaché à ce dossier.")
    })
    public List<PieceJointeDto> getPiecesJointesReglement(@PathVariable UUID id, @PathVariable UUID reglementId) {
        return reglementPieceJointeService.findByReglement(id, reglementId);
    }

    @PostMapping(value = "/{id}/reglements/{reglementId}/pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Dépose une preuve de paiement sur un règlement")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pièce jointe créée."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide."),
            @ApiResponse(responseCode = "404", description = "Dossier ou règlement introuvable, règlement "
                    + "non rattaché à ce dossier, ou `typeId` inconnu.")
    })
    public ResponseEntity<PieceJointeDto> uploadPieceJointeReglement(
            @PathVariable UUID id,
            @PathVariable UUID reglementId,
            @Parameter(description = "Contenu du fichier à déposer.")
            @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Légende libre. Absent = nom de fichier original.")
            @RequestParam(required = false) String description,
            @Parameter(description = "Type de document (AdmAttachedFileType.PKey). Absent = non renseigné, cas normal ici.")
            @RequestParam(required = false) UUID typeId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        PieceJointeDto piece = reglementPieceJointeService.create(id, reglementId, fichier, description, typeId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(piece);
    }

    @GetMapping("/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}/fichier")
    @Operation(summary = "Affiche ou télécharge une preuve de paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "404", description = "Règlement non rattaché à ce dossier, pièce "
                    + "jointe introuvable pour ce règlement, ou aucun fichier stocké.")
    })
    public ResponseEntity<Void> getFichierReglement(@PathVariable UUID id, @PathVariable UUID reglementId,
                                                    @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(reglementPieceJointeService.urlFichier(id, reglementId, pieceId))
                .build();
    }

    @DeleteMapping("/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}")
    @Operation(summary = "Supprime une preuve de paiement d'un règlement")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Preuve de paiement supprimée."),
            @ApiResponse(responseCode = "404"),
            @ApiResponse(responseCode = "409", description = "Dernière preuve de paiement d'un "
                    + "règlement au statut `POINTED`.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePieceJointeReglement(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Identifiant du règlement (StlSettlement.PKey).")
            @PathVariable UUID reglementId,
            @Parameter(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey), tel que renvoyé par `PieceJointeDto.id`.")
            @PathVariable UUID pieceId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        reglementPieceJointeService.supprimer(id, reglementId, pieceId, principal);
    }

    // -- Génération de documents (modèles légataires : instructions/templates) --

    @GetMapping("/{id}/documents/fiche-reservation")
    @Operation(summary = "Fiche de réservation (PDF ou DOCX)",
            description = "La première génération pose la date d'impression du contrat (`ContractPrintDate`).")
    public ResponseEntity<byte[]> getFicheReservation(@PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format) {
        FicheReservationData data = ficheReservationService.collect(id);
        String nomFichier = "fiche-reservation-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return documentResponse(format, nomFichier,
                () -> ficheReservationPdfRenderer.render(data),
                () -> ficheReservationDocxRenderer.render(data));
    }

    @GetMapping("/{id}/documents/acte-reservation")
    public ResponseEntity<byte[]> getActeReservation(@PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format) {
        ActeReservationData data = acteReservationService.collect(id);
        String nomFichier = "acte-reservation-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return documentResponse(format, nomFichier,
                () -> acteReservationPdfRenderer.render(data),
                () -> acteReservationDocxRenderer.render(data));
    }

    @GetMapping("/{id}/documents/fiche-bancaire")
    @Operation(
            summary = "Génère la \"Fiche Bancaire\" du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public ResponseEntity<byte[]> getFicheBancaire(@PathVariable UUID id) {
        FicheBancaireData data = ficheBancaireService.collect(id);
        String nomFichier = "fiche-bancaire-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(ficheBancairePdfRenderer.render(data));
    }

    @GetMapping("/{id}/documents/fiche-visite-technique")
    @Operation(
            summary = "Génère la \"Fiche Visite Technique\" du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public ResponseEntity<byte[]> getFicheVisiteTechnique(@PathVariable UUID id) {
        FicheVisiteTechniqueData data = ficheVisiteTechniqueService.collect(id);
        String nomFichier = "fiche-visite-technique-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(ficheVisiteTechniquePdfRenderer.render(data));
    }

    @GetMapping("/{id}/documents/fiche-dossier-vente")
    @Operation(
            summary = "Génère la \"Fiche Dossier de Vente\" du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public ResponseEntity<byte[]> getFicheDossierVente(@PathVariable UUID id) {
        FicheDossierVenteData data = ficheDossierVenteService.collect(id);
        String nomFichier = "fiche-dossier-vente-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(ficheDossierVentePdfRenderer.render(data));
    }

    @GetMapping("/{id}/documents/fiche-trop-percu")
    @Operation(
            summary = "Génère la \"Fiche Trop Perçu\" du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable.")
    })
    public ResponseEntity<byte[]> getFicheTropPercu(@PathVariable UUID id) {
        FicheTropPercuData data = ficheTropPercuService.collect(id);
        String nomFichier = "fiche-trop-percu-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(ficheTropPercuPdfRenderer.render(data));
    }

    @GetMapping("/{id}/reglements/{reglementId}/documents/recu")
    @Operation(
            summary = "Génère le \"Reçu de règlement\" pour un règlement précis du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, règlement "
                    + "introuvable, ou règlement non rattaché à ce dossier.")
    })
    public ResponseEntity<byte[]> getRecuReglement(
            @PathVariable UUID id,
            @Parameter(description = "Identifiant du règlement (StlSettlement.PKey).")
            @PathVariable UUID reglementId) {
        RecuReglementData data = recuReglementService.collect(id, reglementId);
        String nomFichier = "recu-reglement-" + (data.receiptCode() != null ? data.receiptCode() : reglementId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(recuReglementPdfRenderer.render(data));
    }

    @GetMapping("/{id}/reglements/{reglementId}/documents/autorisation-versement")
    @Operation(
            summary = "Génère l'\"Autorisation d'Encaissement\" pour un règlement précis du dossier (PDF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Dossier introuvable, règlement "
                    + "introuvable, ou règlement non rattaché à ce dossier.")
    })
    public ResponseEntity<byte[]> getAutorisationVersement(
            @PathVariable UUID id,
            @Parameter(description = "Identifiant du règlement (StlSettlement.PKey).")
            @PathVariable UUID reglementId) {
        AutorisationVersementData data = autorisationVersementService.collect(id, reglementId);
        String nomFichier = "autorisation-versement-" + (data.documentCode() != null ? data.documentCode() : reglementId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(autorisationVersementPdfRenderer.render(data));
    }

    // -- Documents en lot : un seul PDF multi-pages, sans variante Word. Pas de "Reçu de
    // règlement" : il est scopé à un règlement, pas à un dossier.

    @PostMapping("/documents/fiche-reservation")
    @Operation(
            summary = "Génère la \"Fiche de réservation\" de plusieurs dossiers en un seul PDF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (une page ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getFichesReservationEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("fiches-reservation", documentLotService.ficheReservation(request.dossierIds()));
    }

    @PostMapping("/documents/acte-reservation")
    @Operation(
            summary = "Génère l'\"Acte de réservation\" de plusieurs dossiers en un seul PDF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (une page ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getActesReservationEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("actes-reservation", documentLotService.acteReservation(request.dossierIds()));
    }

    @PostMapping("/documents/fiche-bancaire")
    @Operation(
            summary = "Génère la \"Fiche Bancaire\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier que `GET /api/dossiers/{id}/documents/"
                    + "fiche-bancaire`, concaténés dans l'ordre de `dossierIds`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (une page ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getFichesBancairesEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("fiches-bancaires", documentLotService.ficheBancaire(request.dossierIds()));
    }

    @PostMapping("/documents/fiche-visite-technique")
    @Operation(
            summary = "Génère la \"Fiche Visite Technique\" de plusieurs dossiers en un seul PDF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (une page ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getFichesVisiteTechniqueEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("fiches-visite-technique", documentLotService.ficheVisiteTechnique(request.dossierIds()));
    }

    @PostMapping("/documents/fiche-dossier-vente")
    @Operation(
            summary = "Génère la \"Fiche Dossier de Vente\" de plusieurs dossiers en un seul PDF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (3 pages ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getFichesDossierVenteEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("fiches-dossier-vente", documentLotService.ficheDossierVente(request.dossierIds()));
    }

    @PostMapping("/documents/fiche-trop-percu")
    @Operation(
            summary = "Génère la \"Fiche Trop Perçu\" de plusieurs dossiers en un seul PDF"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré (une page ou plus par dossier)."),
            @ApiResponse(responseCode = "400", description = "`dossierIds` manquant ou vide."),
            @ApiResponse(responseCode = "404", description = "Au moins un des `dossierIds` est introuvable.")
    })
    public ResponseEntity<byte[]> getFichesTropPercuEnLot(@RequestBody GenererDocumentsLotRequest request) {
        return lotResponse("fiches-trop-percu", documentLotService.ficheTropPercu(request.dossierIds()));
    }

    private static ResponseEntity<byte[]> lotResponse(String nomFichier, byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(pdf);
    }

    private static ResponseEntity<byte[]> documentResponse(String format, String nomFichier,
            java.util.function.Supplier<byte[]> pdfSupplier, java.util.function.Supplier<byte[]> docxSupplier) {
        if ("docx".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(nomFichier + ".docx").build().toString())
                    .body(docxSupplier.get());
        }
        if (!"pdf".equalsIgnoreCase(format)) {
            throw new InvalidRequestException(
                    "format doit être 'pdf' ou 'docx' (reçu: " + format + ")");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(pdfSupplier.get());
    }
}
