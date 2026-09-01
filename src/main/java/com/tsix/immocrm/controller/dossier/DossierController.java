package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.AutorisationVersementData;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.GenererDocumentsLotRequest;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.dossier.ModifierStatutReglementRequest;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.dto.dossier.SchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.SimulerSchemaFinancementRequest;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.FicheBancaireData;
import com.tsix.immocrm.dto.dossier.FicheDossierVenteData;
import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.dto.dossier.FicheTropPercuData;
import com.tsix.immocrm.dto.dossier.RecuReglementData;
import com.tsix.immocrm.dto.dossier.FicheVisiteTechniqueData;
import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
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
import com.tsix.immocrm.services.dossier.RecuReglementService;
import com.tsix.immocrm.services.dossier.ReglementPieceJointeService;
import com.tsix.immocrm.services.dossier.ReglementStatutService;
import com.tsix.immocrm.services.dossier.FicheVisiteTechniqueService;
import com.tsix.immocrm.services.dossier.PieceJointeService;
import com.tsix.immocrm.services.dossier.document.ActeReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.AutorisationVersementPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheBancairePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheDossierVentePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheTropPercuPdfRenderer;
import com.tsix.immocrm.services.dossier.document.RecuReglementPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheVisiteTechniquePdfRenderer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Dossiers", description = "Dossiers de vente (ComFolder) — cycle de vie, échéancier, "
        + "pièces jointes et génération de documents.\n\n"
        + "Collection soumise au périmètre commercial : sur **toutes** les routes portant un "
        + "identifiant de dossier — y compris les routes imbriquées (échéancier, règlements, pièces "
        + "jointes, historique, générateurs de documents) — un dossier hors du périmètre de "
        + "l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un "
        + "identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 "
        + "habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur "
        + "GET /api/mon-compte.")
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
                              ReglementPieceJointeService reglementPieceJointeService) {
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
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des dossiers de vente",
            description = "Filtre optionnellement par statut (multi-sélection, une valeur pour "
                    + "un seul statut se comporte comme avant), commercial, entité (projet), "
                    + "tranche(s), plage de dates sur un jalon au choix, et recherche libre sur "
                    + "le code du dossier OU le nom/prénom/raison sociale du client lié. Voir "
                    + "instructions/filtres-avances.md pour le détail des filtres avancés.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), cette liste ne "
                    + "contient que les dossiers de ses projets affectés — filtrage côté serveur, "
                    + "appliqué en plus des filtres ci-dessus. Le front ne doit pas refiltrer par "
                    + "projet lui-même. Les dossiers dont le projet ne se résout pas restent "
                    + "visibles de tous."
    )
    public PageDto<DossierListItemDto> findAll(
            @Parameter(description = "Code(s) StrStatus à filtrer. Répéter le paramètre pour "
                    + "sélectionner plusieurs statuts (?statut=Réservation&statut=Concrétisation). "
                    + "Absent = tous les statuts.", example = "Réservation")
            @RequestParam(required = false) List<String> statut,
            @Parameter(description = "Id du commercial (CfgTier.id, cfgCommercial).")
            @RequestParam(required = false) UUID commercial,
            @Parameter(description = "Id de l'entité/projet (CfgProject.id), matché via l'article "
                    + "principal du dossier.")
            @RequestParam(required = false) UUID projet,
            @Parameter(description = "Id(s) de tranche (CfgTranche.id, StkItem.stkHierarchy). "
                    + "Répéter le paramètre pour plusieurs tranches. Renvoie les dossiers dont au "
                    + "moins un article appartient à une des tranches demandées.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Jalon sur lequel appliquer dateDebut/dateFin. Ignoré si "
                    + "aucun des deux n'est fourni. Défaut : RESERVATION (= date de création du "
                    + "dossier).", example = "CONCRETISATION")
            @RequestParam(required = false) ChampDateDossier champDate,
            @Parameter(description = "Borne inférieure (incluse) de la plage sur champDate, "
                    + "format ISO-8601.", example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) de la plage sur champDate, "
                    + "format ISO-8601.", example = "2026-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @Parameter(description = "Recherche libre sur le code du dossier OU le nom/prénom/"
                    + "raison sociale du client lié (sous-chaîne, insensible à la casse).")
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(dossierService.findAll(statut, commercial, projet, trancheId, champDate, dateDebut, dateFin, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'un dossier de vente",
            description = "Fiche complète du dossier : statut courant et étapes atteignables, "
                    + "client/commercial/notaire, articles liés, ventilation des montants et "
                    + "prospection d'origine.\n\n"
                    + "**Périmètre commercial** : un dossier hors du périmètre de l'appelant "
                    + "renvoie **404**, et non 403 — un 403 confirmerait son existence et "
                    + "permettrait d'énumérer les identifiants. La même règle s'applique à *toutes* "
                    + "les routes imbriquées de ce dossier (échéancier, règlements, pièces jointes, "
                    + "historique, générateurs de documents)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossier trouvé."),
            @ApiResponse(responseCode = "404", description = "Aucun dossier avec cet id, ou dossier "
                    + "hors du périmètre commercial de l'appelant — les deux cas sont "
                    + "volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque "
                    + "l'identifiant fourni n'est pas un UUID valide : sur les collections soumises "
                    + "au périmètre, un identifiant non résoluble est refusé sans être transmis au "
                    + "contrôleur.")
    })
    public DossierDetailDto findById(
            @Parameter(description = "Identifiant du dossier (ComFolder.PKey).")
            @PathVariable UUID id) {
        return dossierService.findById(id);
    }

    @PostMapping("/simulation-financement")
    @Operation(
            summary = "Prévisualise le schéma de financement (étape avant création du dossier)",
            description = "Calcule le \"Schéma de financement\" (AUTO-FINANCEMENT / AVANCE / "
                    + "FRAIS SYNDIC) que `POST /api/dossiers` générerait pour ce(s) article(s) et "
                    + "ce prix, **sans rien créer ni persister** — ni dossier, ni échéance. Étape "
                    + "de simulation dans l'assistant de création : le front appelle cet endpoint "
                    + "à chaque changement de prix/article pour afficher le récapitulatif avant "
                    + "validation finale via `POST /api/dossiers`. `articleIds` accepte le même "
                    + "format que la création réelle (le premier article détermine la tranche — "
                    + "barème AVANCE/SYNDIC — et la vocation — catégorie d'échéance). Les articles "
                    + "n'ont pas besoin d'être DISPONIBLE ici : rien n'est réservé. Si "
                    + "`remisePercent` est fourni, AVANCE et AUTO-FINANCEMENT sont calculés sur le "
                    + "prix **après** remise (`prix` étant alors le prix avant remise, articles "
                    + "suivants comptés comme annexes) — mêmes montants que ceux que "
                    + "`POST /api/dossiers` persistera pour les mêmes paramètres. FRAIS SYNDIC ne "
                    + "dépend pas du prix (barème de la tranche) et n'est donc jamais remisé."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schéma calculé. `lignes` peut être "
                    + "vide si aucune catégorie AUTO-FINANCEMENT/AVANCE/FRAIS SYNDIC n'existe pour "
                    + "la vocation de l'article (cas très inhabituel — même règle qu'à la création)."),
            @ApiResponse(responseCode = "400", description = "`articleIds` manquant/vide, `prix` "
                    + "manquant, `remisePercent` hors de [0 ; 100], ou un des articles est introuvable.")
    })
    public SchemaFinancementDto simulerSchemaFinancement(@RequestBody SimulerSchemaFinancementRequest request) {
        return dossierService.simulerSchemaFinancement(request);
    }

    @PostMapping
    @Operation(
            summary = "Crée un dossier de vente à partir d'une prospection",
            description = "Voir `POST /api/dossiers/simulation-financement` pour prévisualiser le "
                    + "schéma de financement avant cet appel final. Si `prix` est fourni ici, le "
                    + "schéma de financement est généré et persisté automatiquement (mêmes "
                    + "montants que la simulation, à condition que les mêmes articles/prix soient "
                    + "utilisés), et la ventilation HT/TTC/remise du dossier est calculée comme "
                    + "sur l'onglet \"Montants\" d'une Offre (prix commercial de l'article, taux "
                    + "de TVA) — sauf que le total du dossier (`totalToPay`) inclut en plus les "
                    + "Frais Syndic de la tranche, contrairement au total d'une Offre.\n\n"
                    + "**Périmètre commercial** : créer un dossier sur un article dont le projet "
                    + "n'est pas affecté à l'appelant renvoie **403** (et non 404 : ici c'est "
                    + "l'appelant qui a nommé l'article, il n'y a rien à cacher). Le projet est "
                    + "déduit du premier article via sa tranche.\n\n"
                    + "**Remise commerciale** — deux modes, selon `remisePercent` :\n"
                    + "- `remisePercent` **fourni** (écran de création où le commercial saisit la "
                    + "remise) : `prix` est le prix **avant** remise et sert de base à la place du "
                    + "prix commercial de l'article ; les annexes (articles suivants de "
                    + "`articleIds`) s'y ajoutent ; la remise est appliquée sur ce total et se "
                    + "répercute sur `prixApresRemise`, `prix`, `totalAPayer` **et** sur le schéma "
                    + "de financement (AVANCE / AUTO-FINANCEMENT calculés sur le prix après "
                    + "remise). Les FRAIS SYNDIC viennent du barème de la tranche et ne sont "
                    + "jamais remisés.\n"
                    + "- `remisePercent` **absent** (comportement historique, inchangé) : `prix` "
                    + "est le prix négocié **après** remise, et `remise`/`remiseMontant` sont "
                    + "déduits de l'écart avec le prix commercial de l'article."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dossier créé."),
            @ApiResponse(responseCode = "400", description = "Champ requis manquant "
                    + "(`prospectionId`, `articleIds`, `modePaieId`, `cin`), prospection/article/"
                    + "mode de paiement introuvable, prospection sans prospect lié, article déjà "
                    + "occupé, ou `remisePercent` hors de [0 ; 100]."),
            @ApiResponse(responseCode = "403", description = "Le projet de l'article n'est pas dans "
                    + "le périmètre commercial de l'appelant.")
    })
    public ResponseEntity<DossierDetailDto> create(@RequestBody CreerDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.create(request));
    }

    @PatchMapping("/{id}/statut")
    public DossierDetailDto changerStatut(@PathVariable UUID id, @RequestBody ChangerStatutRequest request) {
        return dossierService.changerStatut(id, request);
    }

    @GetMapping("/{id}/statut/etapes-suivantes")
    public List<com.tsix.immocrm.dto.dossier.EtapeSuivanteDto> getEtapesSuivantes(@PathVariable UUID id) {
        return dossierService.etapesSuivantes(id);
    }

    /**
     * Unified, chronological, traceable history of the Dossier: status changes (with note),
     * échéances created, règlements posted, pièces jointes added. See
     * {@link DossierHistoriqueService}.
     */
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
    public ResponseEntity<EcheanceDto> addEcheance(@PathVariable UUID id, @RequestBody CreerEcheanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addEcheance(id, request));
    }

    @PatchMapping("/{id}/echeancier/{echeanceId}")
    public EcheanceDto updateEcheance(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                       @RequestBody ModifierEcheanceRequest request) {
        return echeancierService.updateEcheance(id, echeanceId, request);
    }

    @PostMapping("/{id}/echeancier/{echeanceId}/reglements")
    @Operation(
            summary = "Enregistre un règlement sur une échéance",
            description = "Crée un `StlSettlement` + sa `StlSettlementLine` de rattachement à "
                    + "l'échéance `echeanceId`. Le règlement naît toujours au statut `IN_WORK`.\n\n"
                    + "**Saisie complète obligatoire** (400 sur chaque manquement) : `montant` "
                    + "(strictement positif et au plus égal au reste à payer de l'échéance), "
                    + "`dateEcheance`, `typeId` et `payeurId`. `reference` (n° de chèque...) est "
                    + "requise dès que le type choisi a `TypeReglementDto.referenceObligatoire=true` "
                    + "(CHÈQUE, EFFET...) ; `banqueId` est requise dès que "
                    + "`TypeReglementDto.banqueObligatoire=true` — ce flag était annoncé mais pas "
                    + "appliqué jusqu'ici.\n\n"
                    + "`typeId` et `payeurId` sont exigés parce qu'ils alimentent le « Mode de "
                    + "versement » et le « Versement effectué par » de l'**autorisation "
                    + "d'encaissement**, à ouvrir juste après création via "
                    + "`GET /api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement` "
                    + "(le `reglementId` est l'`id` renvoyé ici).\n\n"
                    + "`dateReglement` et `statut=POINTED` sont **refusés** (400) : pointer un "
                    + "règlement exige une preuve de paiement, impossible à fournir avant que le "
                    + "règlement existe. Pour saisir un règlement déjà encaissé, enchaîner : ce "
                    + "`POST`, puis `POST .../reglements/{reglementId}/pieces-jointes`, puis "
                    + "`PATCH .../reglements/{reglementId}/statut`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Règlement enregistré, au statut `IN_WORK`."),
            @ApiResponse(responseCode = "400", description = "Champ obligatoire absent (`montant`, "
                    + "`dateEcheance`, `typeId`, `payeurId`), montant négatif/nul ou dépassant le "
                    + "reste à payer, `reference`/`banqueId` absente alors que le type la requiert, "
                    + "ou `dateReglement`/`statut=POINTED` fournis."),
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
    @Operation(
            summary = "Historique complet des règlements d'une échéance",
            description = "Contrepartie en lecture du `POST` ci-dessus — tous les versements "
                    + "déjà enregistrés sur cette échéance, plus récent d'abord, avec le détail "
                    + "complet de chacun : type de règlement (`typeId`/`typeCode`/`typeLibelle`, "
                    + "voir `GET /api/referentiels/types-reglement`), banque (`banqueId`/`banqueNom`, "
                    + "pertinent seulement si le type le requiert), tiers réglant "
                    + "(`payeurId`/`payeurNom` — peut différer de l'acquéreur principal), date du "
                    + "document (saisie), date d'échéance, date de règlement effectif (souvent "
                    + "absente), référence (n° de chèque...) et notes. "
                    + "`GET /api/dossiers/{id}/echeancier` ne renvoie que le total réglé/reste "
                    + "par échéance (`montantRegle`/`reste`) — cet endpoint donne le détail "
                    + "ligne par ligne derrière ce total."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des règlements (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier ou échéance introuvable (ou l'échéance n'appartient pas à ce dossier).")
    })
    public List<ReglementDto> getReglements(@PathVariable UUID id, @PathVariable UUID echeanceId) {
        return echeancierService.findReglements(id, echeanceId);
    }

    @GetMapping("/{id}/montants")
    public MontantsDossierDto montants(@PathVariable UUID id) {
        return dossierService.montants(id);
    }

    // -- Pièces jointes (AdmAttachedFile — see PieceJointeService for the confirmed linkage) --

    @GetMapping("/{id}/pieces-jointes")
    @Operation(
            summary = "Liste les pièces jointes du dossier",
            description = "Retourne toutes les pièces jointes rattachées au dossier `id` "
                    + "(`AdmAttachedFile.CategoryId = ComFolder.PKey`, voir la doc de "
                    + "`AdmAttachedFileSummary`/`AdmAttachedFileSummaryRepository#findByDossierId` "
                    + "pour le rattachement confirmé), triées par date de dépôt décroissante. "
                    + "Inclut toutes les catégories legacy (\"Dossier de vente...\", "
                    + "\"Yes.Com.Sales.Sale\", etc.) — aucun filtrage sur `Category` n'est "
                    + "appliqué. Chaque pièce jointe expose son `type` de document (ex. \"PIECE "
                    + "D'IDENTITE\", \"CONTRAT\"...), issu du référentiel `AdmAttachedFileType` "
                    + "— peut être `null` si non renseigné en base. Les lignes \"cochées mais "
                    + "jamais déposées\" (checklist sans fichier réel) sont exclues. Liste vide "
                    + "si aucune pièce jointe."
    )
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
    @Operation(
            summary = "Dépose une pièce jointe sur le dossier",
            description = "Téléverse un fichier (multipart/form-data) et l'associe au dossier "
                    + "`id` (stocké dans `AdmAttachedFile`, voir la doc de `AdmAttachedFileSummary` "
                    + "pour le rattachement confirmé à `ComFolder`). `description` est une légende "
                    + "libre (ex. \"CIN client\", \"Contrat signé\") ; si absente, le nom de "
                    + "fichier original est utilisé. `typeId` référence un `AdmAttachedFileType` "
                    + "(voir `GET /api/referentiels/types-piece-jointe`) ; absent = type non "
                    + "renseigné. `creePar` est déduit du token de l'utilisateur connecté — jamais "
                    + "demandé au front — et vaut `null` si non authentifiable. Le fichier "
                    + "réapparaît immédiatement dans `GET /api/dossiers/{id}/pieces-jointes`."
    )
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
    public ResponseEntity<byte[]> getFichier(@PathVariable UUID id, @PathVariable UUID pieceId) {
        AdmAttachedFile fichier = pieceJointeService.findFichier(id, pieceId);
        String extension = fichier.getExtension() != null ? fichier.getExtension().toLowerCase(Locale.ROOT) : "";
        MediaType contentType = com.tsix.immocrm.web.ContentTypeParExtension.resolve(extension);
        String nomFichier = (fichier.getDescription() != null ? fichier.getDescription() : "piece-jointe") + extension;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(fichier.getAttachedFile());
    }

    // -- Règlements : statut + preuves de paiement (voir ReglementStatutService) --

    @PatchMapping("/{id}/reglements/{reglementId}/statut")
    @Operation(
            summary = "Change le statut d'un règlement (pointage / annulation)",
            description = "Fait évoluer `StlSettlement.Status`. Transitions autorisées : "
                    + "`IN_WORK → POINTED` et `IN_WORK → CANCELED` — les deux cibles sont "
                    + "terminales, donc toute transition partant de `POINTED` ou `CANCELED` "
                    + "renvoie 409 avec la liste `transitionsAutorisees`.\n\n"
                    + "**Pointer exige une preuve de paiement** : au moins une pièce jointe doit "
                    + "être présente sur le règlement (409 sinon). Déposez-la d'abord via "
                    + "`POST /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes`. Cette "
                    + "règle ne s'applique qu'**au moment de la transition** : les règlements "
                    + "legacy déjà pointés sans pièce jointe restent lisibles, et retirer une "
                    + "pièce jointe ne dépointe rien.\n\n"
                    + "`dateReglement` est **requise** pour `POINTED` (400 sinon) et écrit "
                    + "`StlSettlement.SettlementDate` : statut et date d'encaissement sont deux "
                    + "encodages du même fait et n'ont jamais divergé sur les 3 244 règlements "
                    + "réels. `CANCELED` laisse cette date à `null`."
    )
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
    @Operation(
            summary = "Liste les preuves de paiement d'un règlement",
            description = "Pièces jointes rattachées au règlement (`AdmAttachedFile.CategoryId = "
                    + "StlSettlement.PKey`), plus récente d'abord. Ce sont les justificatifs "
                    + "d'encaissement — chèques scannés, avis de virement, reçus : 452 règlements "
                    + "réels en portent déjà. C'est leur présence qui autorise le passage à "
                    + "`POINTED`. Liste vide si aucune pièce jointe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des pièces jointes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Dossier ou règlement introuvable, ou règlement non rattaché à ce dossier.")
    })
    public List<PieceJointeDto> getPiecesJointesReglement(@PathVariable UUID id, @PathVariable UUID reglementId) {
        return reglementPieceJointeService.findByReglement(id, reglementId);
    }

    @PostMapping(value = "/{id}/reglements/{reglementId}/pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Dépose une preuve de paiement sur un règlement",
            description = "Téléverse un justificatif (multipart/form-data) et l'associe au "
                    + "règlement. `description` est une légende libre (ex. \"Chèque BMCE n° 123\", "
                    + "\"Avis de virement\") ; si absente, le nom de fichier original est utilisé. "
                    + "`typeId` est facultatif et vaut `null` par défaut — les preuves de paiement "
                    + "réelles n'en portent aucune. `creePar` est déduit du token, jamais demandé "
                    + "au front.\n\nUne fois au moins une pièce déposée, le règlement peut passer "
                    + "à `POINTED` via `PATCH .../reglements/{reglementId}/statut`."
    )
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
    @Operation(
            summary = "Télécharge une preuve de paiement",
            description = "Renvoie le contenu binaire de la pièce jointe, avec le `Content-Type` "
                    + "déduit de son extension (PDF et images en pratique)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenu du fichier."),
            @ApiResponse(responseCode = "404", description = "Pièce jointe introuvable pour ce règlement, ou sans fichier attaché.")
    })
    public ResponseEntity<byte[]> getFichierReglement(@PathVariable UUID id, @PathVariable UUID reglementId,
                                                       @PathVariable UUID pieceId) {
        AdmAttachedFile fichier = reglementPieceJointeService.findFichier(id, reglementId, pieceId);
        String extension = fichier.getExtension() != null ? fichier.getExtension().toLowerCase(Locale.ROOT) : "";
        MediaType contentType = com.tsix.immocrm.web.ContentTypeParExtension.resolve(extension);
        String nomFichier = (fichier.getDescription() != null ? fichier.getDescription() : "preuve-paiement") + extension;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(fichier.getAttachedFile());
    }

    // -- Génération de documents ("Fiche de réservation" pour l'instant — voir instructions/templates) --

    @GetMapping("/{id}/documents/fiche-reservation")
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
            summary = "Génère la \"Fiche Bancaire\" du dossier (PDF)",
            description = "Modélisé sur le template legacy \"FICHE BANCAIRE\" — fiche de soutien "
                    + "à une demande de crédit immobilier : état civil complet de l'acquéreur "
                    + "principal et de chaque co-acquéreur (`ComFolderCustomer`), désignation "
                    + "complète du bien, et détail prix/frais dossier/frais syndic avec la part "
                    + "déjà réglée et le reliquat (réutilise le même calcul que "
                    + "`GET /api/dossiers/{id}/montants`, jamais re-dérivé). PDF uniquement — pas "
                    + "de variante Word pour ce document."
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
            summary = "Génère la \"Fiche Visite Technique\" du dossier (PDF)",
            description = "Modélisé sur le template legacy \"FICHE VISITE TECHNIQUE\" — checklist "
                    + "d'inspection avant livraison (\"FICHE D'INTERVENTION AVANT LIVRAISON\"), "
                    + "référencée par la clause \"SERVICE APRES VENTE\" de l'Acte de réservation. "
                    + "Les colonnes Localisation/Date/Observations de la checklist sont "
                    + "volontairement laissées vides dans le PDF — remplies à la main lors de la "
                    + "visite physique, rien dans le schéma ne les alimente. Inclut aussi la "
                    + "synthèse des obligations du règlement de copropriété et la clause de "
                    + "contact SAV (adresse bureau, responsable de tranche/projet selon le code de "
                    + "la tranche, durée de garantie). Le template legacy est bilingue "
                    + "français/arabe ; seul le contenu français est reproduit ici. PDF uniquement."
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
            summary = "Génère la \"Fiche Dossier de Vente\" du dossier (PDF)",
            description = "Reproduit la mise en page du template legacy Stimulsoft "
                    + "\"FICHE COMPLETE\" (`ComSaleFiche.yes`) sur 3 pages — récapitulatif interne "
                    + "complet du dossier : page 1 ENTETE/MONTANTS/DATE/ANNULATION, page 2 AUTRES, "
                    + "page 3 SCHEMA DE FINANCEMENT (avec détail des règlements)/SOUS "
                    + "ARTICLES/ACQUEREURS/HISTORIQUE (réutilise "
                    + "`GET /api/dossiers/{id}/historique`). Le bloc MONTANTS a 9 lignes fixes "
                    + "recalculées depuis l'échéancier via les catégories dédiées "
                    + "(Frais/Modification/Avance/Crédit/Auto-financement/Pénalité/Résiliation), "
                    + "distinct de `GET /api/dossiers/{id}/montants` qui groupe par texte libre. "
                    + "Deux champs du template legacy n'ont pas d'équivalent dans ce modèle de "
                    + "données et restent vides : \"Présentation\" et \"Décision MEILLEURTAUX\". "
                    + "PDF uniquement."
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
            summary = "Génère la \"Fiche Trop Perçu\" du dossier (PDF)",
            description = "Modélisé bande par bande sur le template legacy Stimulsoft "
                    + "`ComSaleFicheTropPercu.yes` — reçu attestant que les versements du client "
                    + "dépassent le prix du dossier : état civil de l'acquéreur et de ses "
                    + "éventuels co-acquéreurs, désignation complète du bien (avec sous-articles), "
                    + "prix et total des frais (réutilise `GET /api/dossiers/{id}/montants` pour "
                    + "le total des frais, jamais re-dérivé), puis le détail chronologique de "
                    + "chaque versement et le \"Reste à payer\" (négatif quand il y a "
                    + "effectivement un trop perçu — c'est cette valeur, en valeur absolue, qui "
                    + "est reprise dans le montant du trop perçu, y compris en toutes lettres). "
                    + "PDF uniquement — pas de variante Word pour ce document."
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
            summary = "Génère le \"Reçu de règlement\" pour un règlement précis du dossier (PDF)",
            description = "Modélisé sur la page vente (\"Page1\") du template legacy Stimulsoft "
                    + "`instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_*.yes` (la "
                    + "variante locative de ce même fichier, basée sur RntDocumentView, n'est pas "
                    + "couverte) — une \"ATTESTATION DE RÉCEPTION CHEQUE\" pour le règlement "
                    + "`reglementId`, avec le texte inversé (client↔société) quand "
                    + "`StlSettlement.isCredit` est vrai (remboursement plutôt que versement). "
                    + "Inclut la désignation du bien, puis la \"situation des chèques reçus\" "
                    + "(tous les règlements du dossier, pas seulement celui de ce reçu) et le "
                    + "\"Reste à payer\" (réutilise `GET /api/dossiers/{id}/montants`, hors FRAIS "
                    + "DOSSIER/FRAIS SYNDIC, jamais re-dérivé). PDF uniquement."
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
            summary = "Génère l'\"Autorisation d'Encaissement\" pour un règlement précis du dossier (PDF)",
            description = "Modélisé sur le template legacy Stimulsoft "
                    + "`instructions/templates/autorisation_virement/ComSaleDocumentAutorisationVersement.yes` — "
                    + "un document à remettre à la banque du client, l'autorisant à accepter l'encaissement du "
                    + "règlement `reglementId` (`StlSettlement.Amount`) pour le compte de la société. "
                    + "**Scopé au règlement (`StlSettlement`), pas à l'échéance** : bien que le template legacy "
                    + "clé techniquement sur l'échéance (`ComDocument`), les champs qui donnent sa valeur au "
                    + "document (compte destinataire, mode de versement, tiers payeur) sont confirmés quasi "
                    + "systématiquement renseignés sur le règlement en données réelles "
                    + "(`StlAccountId` 76%, `StlSettlementTypeId` 99,9%, `TierId` 99,7%) et quasi jamais sur "
                    + "l'échéance correspondante (2/958, 1/958) — un document scopé échéance ressortait vide "
                    + "sur la quasi-totalité des dossiers réels. Inclut la désignation de l'opération/du bien, "
                    + "le prix de vente TTC du dossier, la liste des clients (acquéreur + co-acquéreurs), le "
                    + "RIB du compte destinataire (`StlSettlement.StlAccountId`), le délai de retour "
                    + "(`CfgTranche.WithdrawalDelay`) et se termine par un encadré \"Cadre réservé à la banque\" "
                    + "à faire remplir et signer. Le \"Responsable Commercial\" affiché est "
                    + "`ComFolder.CfgCommercialId` (pas de session utilisateur en jeu ici, contrairement à "
                    + "l'app desktop legacy). PDF uniquement — pas de variante Word pour ce document."
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

    // -- Génération de documents en lot (plusieurs dossiers → un seul PDF multi-pages) --
    // Un endpoint par type de document dossier, sauf le "Reçu de règlement" (scopé à un
    // règlement, pas à un dossier — pas de "sélection multiple" naturelle pour lui) et sans
    // variante Word (PDF uniquement, voir GenererDocumentsLotRequest).

    @PostMapping("/documents/fiche-reservation")
    @Operation(
            summary = "Génère la \"Fiche de réservation\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier que `GET /api/dossiers/{id}/documents/"
                    + "fiche-reservation` (format PDF), concaténés dans l'ordre de `dossierIds` — "
                    + "un groupe de pages par dossier. PDF uniquement (pas de variante Word en lot)."
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
            summary = "Génère l'\"Acte de réservation\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier que `GET /api/dossiers/{id}/documents/"
                    + "acte-reservation` (format PDF), concaténés dans l'ordre de `dossierIds`. "
                    + "PDF uniquement (pas de variante Word en lot)."
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
            summary = "Génère la \"Fiche Visite Technique\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier que `GET /api/dossiers/{id}/documents/"
                    + "fiche-visite-technique`, concaténés dans l'ordre de `dossierIds`."
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
            summary = "Génère la \"Fiche Dossier de Vente\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier (3 pages chacun, voir `GET /api/dossiers/{id}/"
                    + "documents/fiche-dossier-vente`) que le endpoint single-dossier, concaténés "
                    + "dans l'ordre de `dossierIds`."
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
            summary = "Génère la \"Fiche Trop Perçu\" de plusieurs dossiers en un seul PDF",
            description = "Même contenu par dossier que `GET /api/dossiers/{id}/documents/"
                    + "fiche-trop-percu`, concaténés dans l'ordre de `dossierIds` — le scénario "
                    + "d'origine de cette fonctionnalité : sélectionner plusieurs dossiers en "
                    + "trop-perçu et imprimer toutes leurs fiches en un seul PDF."
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

    /** Shared PDF/Word response builder for the "Génération de documents" endpoints above. */
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
            throw new com.tsix.immocrm.exceptions.InvalidRequestException(
                    "format doit être 'pdf' ou 'docx' (reçu: " + format + ")");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(pdfSupplier.get());
    }
}
