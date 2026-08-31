package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospection.AjouterAutreTrancheRequest;
import com.tsix.immocrm.dto.prospection.AjouterCategorieRequest;
import com.tsix.immocrm.dto.prospection.AjouterProfilRequest;
import com.tsix.immocrm.dto.prospection.AjouterTypeBienRequest;
import com.tsix.immocrm.dto.prospection.AjouterVilleRequest;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.CreerProspectionRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ModifierTranchePrincipaleRequest;
import com.tsix.immocrm.dto.prospection.NdaData;
import com.tsix.immocrm.dto.prospection.OffreDevisData;
import com.tsix.immocrm.dto.prospection.ProspectionFicheData;
import com.tsix.immocrm.dto.prospection.ProspectionAutreTrancheDto;
import com.tsix.immocrm.dto.prospection.ProspectionCategorieDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.ProspectionProfilDto;
import com.tsix.immocrm.dto.prospection.ProspectionTypeBienDto;
import com.tsix.immocrm.dto.prospection.ProspectionVilleDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.services.prospection.DevisService;
import com.tsix.immocrm.services.prospection.NdaService;
import com.tsix.immocrm.services.prospection.OffreDevisDocumentService;
import com.tsix.immocrm.services.prospection.ProspectionQualificationService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.prospection.ProspectionFicheService;
import com.tsix.immocrm.services.prospection.document.NdaPdfRenderer;
import com.tsix.immocrm.services.prospection.document.OffreDevisPdfRenderer;
import com.tsix.immocrm.services.prospection.document.ProspectionFichePdfRenderer;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Prospections", description = "Prospections (ComProspection) — pipeline commercial, "
        + "qualification, interactions, devis.\n\n"
        + "**Guide d'implémentation front — tranche/article principal vs. \"Autres tranches\"**\n"
        + "Une prospection porte DEUX notions distinctes de tranche/article, à ne pas confondre "
        + "côté UI :\n"
        + "1. **Tranche/article principal** (`ProspectionDetailDto.trancheId`/`articleId`, + leurs "
        + "`...Code`/`...Description` pour l'affichage) — LE couple sur lequel porte la "
        + "prospection. Modifié exclusivement via `PATCH /api/prospections/{id}/tranche-principale` "
        + "(jamais via le PATCH générique `PATCH /api/prospections/{id}`, qui ne touche plus à ces "
        + "deux champs). Cet endpoint garantit que l'article appartient bien à la tranche fournie "
        + "(400 sinon) — avant ce garde-fou, un mauvais appel avait déjà pu créer une incohérence "
        + "réelle en base (article d'une tranche assigné alors que la tranche principale en "
        + "pointait une autre).\n"
        + "2. **Autres tranches** (`GET/POST/DELETE /api/prospections/{id}/autres-tranches`, "
        + "`ComProspectionProject`) — des intérêts SECONDAIRES et indépendants, sur d'autres "
        + "tranches/articles que la tranche principale (ex. le prospect est aussi intéressé par un "
        + "autre programme). N'affecte jamais `trancheId`/`articleId` de la prospection. Chaque "
        + "entrée porte elle aussi `trancheDescription`/`articleDescription` pour l'affichage.\n"
        + "**Côté front** : le champ \"Tranche\"/\"Article\" principal du formulaire de "
        + "qualification doit appeler l'endpoint (1) ; l'onglet \"Autres tranches\" doit appeler "
        + "l'endpoint (2). Ce sont deux actions utilisateur différentes, jamais interchangeables.\n\n"
        + "Collection soumise au périmètre commercial : sur **toutes** les routes portant un "
        + "identifiant de prospection — y compris les routes imbriquées (interactions, devis, "
        + "qualification, documents) — une prospection hors du périmètre de l'appelant renvoie 404 "
        + "(jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un "
        + "UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant "
        + "d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.")
@RestController
@RequestMapping("/api/prospections")
public class ProspectionController {

    private final ProspectionService prospectionService;
    private final ProspectionQualificationService qualificationService;
    private final DevisService devisService;
    private final OffreDevisDocumentService offreDevisDocumentService;
    private final OffreDevisPdfRenderer offreDevisPdfRenderer;
    private final NdaService ndaService;
    private final NdaPdfRenderer ndaPdfRenderer;
    private final ProspectionFicheService prospectionFicheService;
    private final ProspectionFichePdfRenderer prospectionFichePdfRenderer;

    public ProspectionController(ProspectionService prospectionService,
                                  ProspectionQualificationService qualificationService,
                                  DevisService devisService,
                                  OffreDevisDocumentService offreDevisDocumentService,
                                  OffreDevisPdfRenderer offreDevisPdfRenderer,
                                  NdaService ndaService,
                                  NdaPdfRenderer ndaPdfRenderer,
                                  ProspectionFicheService prospectionFicheService,
                                  ProspectionFichePdfRenderer prospectionFichePdfRenderer) {
        this.prospectionService = prospectionService;
        this.qualificationService = qualificationService;
        this.devisService = devisService;
        this.offreDevisDocumentService = offreDevisDocumentService;
        this.offreDevisPdfRenderer = offreDevisPdfRenderer;
        this.ndaService = ndaService;
        this.ndaPdfRenderer = ndaPdfRenderer;
        this.prospectionFicheService = prospectionFicheService;
        this.prospectionFichePdfRenderer = prospectionFichePdfRenderer;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des prospections",
            description = "Filtre optionnellement par étape/origine/commercial (multi-sélection, "
                    + "une valeur pour un seul choix se comporte comme avant), tranche(s) visée(s), "
                    + "plage de dates sur la date du document, et recherche libre. Exclut les "
                    + "pré-prospections (voir GET /api/pre-prospections). Voir "
                    + "instructions/filtres-avances.md pour le détail des filtres avancés.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), cette liste ne "
                    + "contient que les prospections de ses projets affectés — filtrage côté "
                    + "serveur, en plus des filtres ci-dessus. Le projet d'une prospection est "
                    + "celui de sa tranche visée ; celles sans tranche restent visibles de tous."
    )
    public PageDto<ProspectionListItemDto> findAll(
            @Parameter(description = "Code(s) ComProspectionKind (\"Nature\"/étape) à filtrer. "
                    + "Répéter le paramètre pour plusieurs étapes. Absent = toutes les étapes.",
                    example = "3 QUALIFIÉ")
            @RequestParam(required = false) List<String> etape,
            @Parameter(description = "Code(s) ComProspectionOrigin à filtrer. Répéter le "
                    + "paramètre pour plusieurs origines.", example = "FACEBOOK")
            @RequestParam(required = false) List<String> origine,
            @Parameter(description = "Id(s) du commercial (CfgTier.id, cfgCommercial). Répéter "
                    + "le paramètre pour plusieurs commerciaux.")
            @RequestParam(required = false) List<UUID> commercial,
            @Parameter(description = "Id(s) de tranche visée (CfgTranche.id, "
                    + "ComProspection.stkHierarchy). Répéter le paramètre pour plusieurs tranches.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Borne inférieure (incluse) sur la date du document, "
                    + "format ISO-8601.", example = "2026-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur la date du document, "
                    + "format ISO-8601.", example = "2026-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean ouvertes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectionService.findAll(etape, origine, commercial, trancheId, dateDebut, dateFin,
                search, ouvertes, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'une prospection (ou d'une pré-prospection)",
            description = "Sert aussi bien une prospection qualifiée qu'une pré-prospection : "
                    + "`GET /api/pre-prospections` n'expose pas de route de détail propre, les "
                    + "deux partagent cette fiche.\n\n"
                    + "**Périmètre commercial** : une prospection hors du périmètre de l'appelant "
                    + "renvoie **404**, et non 403 — un 403 confirmerait son existence et "
                    + "permettrait d'énumérer les identifiants. Même règle pour toutes les routes "
                    + "imbriquées (interactions, devis, qualification, documents)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection trouvée."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant — les deux cas sont "
                    + "volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque "
                    + "l'identifiant fourni n'est pas un UUID valide.")
    })
    public ProspectionDetailDto findById(
            @Parameter(description = "Identifiant de la prospection (ComProspection.PKey).")
            @PathVariable UUID id) {
        return prospectionService.findById(id);
    }

    @PostMapping
    @Operation(
            summary = "Crée une prospection qualifiée pour un prospect existant",
            description = "instructions/from_front.md demande #9 — le point d'entrée du bouton "
                    + "\"Nouvelle prospection\", pour le cas où le prospect existe déjà (créé au "
                    + "préalable via `POST /api/prospects`, déjà câblé côté front). `prospectId` "
                    + "est **obligatoire** ; tous les autres champs sont optionnels et suivent la "
                    + "même sémantique que `PATCH /api/prospections/{id}` (mise à jour uniquement "
                    + "des champs fournis). `code` est toujours généré serveur (compteur "
                    + "`AdmSequence` \"Prospection\", format \"AA/NNNNN\"), jamais fourni par le "
                    + "client. `commercialId` peut être fourni directement ici pour obtenir une "
                    + "prospection immédiatement complète en un seul appel — sinon "
                    + "`POST /api/prospections/{id}/transformer` peut l'assigner ensuite. "
                    + "`trancheId`/`articleId` suivent la même validation croisée que "
                    + "`PATCH .../tranche-principale` (l'article doit appartenir à la tranche "
                    + "fournie). Pour un lead brut/non identifié (walk-in, appel entrant sans "
                    + "prospect connu), voir plutôt le flux pré-prospection "
                    + "(`GET /api/pre-prospections` + `POST .../creer-prospect` + "
                    + "`POST .../transformer`) — non couvert par cet endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospection créée."),
            @ApiResponse(responseCode = "400", description = "`prospectId` manquant, ou un id "
                    + "fourni (prospect/commercial/tranche/article/origine/...) est introuvable, "
                    + "ou l'article fourni n'appartient pas à la tranche fournie."),
            @ApiResponse(responseCode = "403", description = "Le projet de la `trancheId` fournie "
                    + "n'est pas dans le périmètre commercial de l'appelant (403 et non 404 : "
                    + "c'est l'appelant qui a nommé la tranche).")
    })
    public ResponseEntity<ProspectionDetailDto> creerProspection(@RequestBody CreerProspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerProspection(request));
    }

    @GetMapping("/{id}/historique")
    @Operation(
            summary = "Historique complet du parcours d'une prospection",
            description = "Piste d'audit paginée (plus récent en premier) des transitions "
                    + "pré-prospect → prospect → prospection tracées par cette API : \"Prospect "
                    + "créé et lié\" (POST /api/pre-prospections/{id}/creer-prospect) et "
                    + "\"Transformée en prospection\" (POST /api/prospections/{id}/transformer). "
                    + "Comble une lacune constatée sur données réelles : Omran ne trace nulle part "
                    + "ces deux transitions (ComProspection.UserUpdateDateTime reste toujours NULL "
                    + "même sur des lignes déjà transformées, la même ligne étant mutée en place). "
                    + "La date de création du pré-prospect reste disponible séparément via "
                    + "ProspectionDetailDto.dateDocument (pas dupliquée ici). Le stade \"Dossier de "
                    + "vente\" a son propre historique, voir GET /api/dossiers/{id}/historique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public PageDto<HistoriqueDto> historique(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(prospectionService.historique(id, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Modifie les champs de qualification d'une prospection",
            description = "Mise à jour partielle (`null` = inchangé) : origine, vocation, budget, "
                    + "notes... **Ne modifie plus la tranche/l'article principal** — utiliser "
                    + "`PATCH /api/prospections/{id}/tranche-principale` pour ça (voir le guide "
                    + "dans la description du tag \"Prospections\")."
    )
    public ProspectionDetailDto modifierProspection(@PathVariable UUID id, @RequestBody ModifierProspectionRequest request) {
        return prospectionService.modifierProspection(id, request);
    }

    @PatchMapping("/{id}/tranche-principale")
    @Operation(
            summary = "Remplace la tranche/l'article principal",
            description = "Seul endpoint qui modifie `trancheId`/`articleId` d'une prospection. "
                    + "`trancheId` est obligatoire ; `articleId` est optionnel mais, s'il est "
                    + "fourni, DOIT appartenir à `trancheId` (l'API renvoie 400 sinon — un article "
                    + "d'une autre tranche ne peut pas devenir article principal). Si `articleId` "
                    + "est omis et que l'article actuellement assigné n'appartient plus à la "
                    + "nouvelle tranche, il est automatiquement désassigné (jamais laissé dans un "
                    + "état incohérent). Distinct de `POST /api/prospections/{id}/autres-tranches` "
                    + "(intérêts secondaires, indépendants) — voir le guide sur le tag.\n\n"
                    + "**Périmètre commercial** : rattacher la prospection à une tranche dont le "
                    + "projet n'est pas affecté à l'appelant renvoie **403** — sans quoi le "
                    + "périmètre se contournerait en déplaçant la ressource hors de son projet."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tranche/article principal mis à jour."),
            @ApiResponse(responseCode = "400", description = "`trancheId` manquant, tranche/article "
                    + "introuvable, ou l'article fourni n'appartient pas à la tranche fournie."),
            @ApiResponse(responseCode = "403", description = "Le projet de la tranche cible n'est "
                    + "pas dans le périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant.")
    })
    public ProspectionDetailDto modifierTranchePrincipale(@PathVariable UUID id,
            @RequestBody ModifierTranchePrincipaleRequest request) {
        return prospectionService.modifierTranchePrincipale(id, request);
    }

    @PostMapping("/{id}/transformer")
    public ProspectionDetailDto transformer(@PathVariable UUID id, @RequestBody TransformerProspectionRequest request) {
        return prospectionService.transformer(id, request);
    }

    @PatchMapping("/{id}/etape")
    public ProspectionDetailDto changerEtape(@PathVariable UUID id, @RequestBody ChangerEtapeRequest request) {
        return prospectionService.changerEtape(id, request);
    }

    @PatchMapping("/{id}/archiver")
    public ProspectionDetailDto archiver(@PathVariable UUID id, @RequestBody ArchiverProspectionRequest request) {
        return prospectionService.archiver(id, request);
    }

    @GetMapping("/{id}/interactions")
    @Operation(
            summary = "Journal des interactions d'une prospection",
            description = "Historique des interactions (appels, messages, notes, visites...) "
                    + "loggées sur cette prospection, les plus récentes en premier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'interactions (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public PageDto<InteractionDto> findInteractions(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(prospectionService.findInteractions(id, pageable));
    }

    @PostMapping("/{id}/interactions")
    @Operation(
            summary = "Ajoute une interaction à une prospection",
            description = "Journalise un appel/message/visite/note sur cette prospection. "
                    + "`actionTypeId` (\"Action\") et `resultatId` (\"Résultat\") sont "
                    + "optionnels mais recommandés pour un suivi exploitable — voir "
                    + "GET /api/referentiels/types-contact-prospection et "
                    + "GET /api/referentiels/resultats-interaction. Si le résultat choisi est "
                    + "\"Rendez vous pris\", \"RDV reporté\" ou \"Le prospect veut faire une "
                    + "visite du projet\", `dateRdv` devient obligatoire et une entrée "
                    + "calendrier est créée, visible ensuite via GET /api/calendrier/evenements."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Interaction créée."),
            @ApiResponse(responseCode = "400", description = "`description` manquante, "
                    + "`actionTypeId`/`resultatId` inconnu, ou `dateRdv` manquante alors que le "
                    + "résultat choisi l'exige."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ResponseEntity<InteractionDto> addInteraction(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @RequestBody CreerInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.addInteraction(id, request));
    }

    // -- GAP-04: qualification sub-resources --

    @GetMapping("/{id}/villes")
    public List<ProspectionVilleDto> getVilles(@PathVariable UUID id) {
        return qualificationService.villes(id);
    }

    @PostMapping("/{id}/villes")
    public ProspectionVilleDto ajouterVille(@PathVariable UUID id, @RequestBody AjouterVilleRequest request) {
        return qualificationService.ajouterVille(id, request);
    }

    @DeleteMapping("/{id}/villes/{itemId}")
    public ResponseEntity<Void> retirerVille(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerVille(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/categories")
    public List<ProspectionCategorieDto> getCategories(@PathVariable UUID id) {
        return qualificationService.categories(id);
    }

    @PostMapping("/{id}/categories")
    public ProspectionCategorieDto ajouterCategorie(@PathVariable UUID id, @RequestBody AjouterCategorieRequest request) {
        return qualificationService.ajouterCategorie(id, request);
    }

    @DeleteMapping("/{id}/categories/{itemId}")
    public ResponseEntity<Void> retirerCategorie(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerCategorie(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/types-bien")
    public List<ProspectionTypeBienDto> getTypesBien(@PathVariable UUID id) {
        return qualificationService.typesBien(id);
    }

    @PostMapping("/{id}/types-bien")
    public ProspectionTypeBienDto ajouterTypeBien(@PathVariable UUID id, @RequestBody AjouterTypeBienRequest request) {
        return qualificationService.ajouterTypeBien(id, request);
    }

    @DeleteMapping("/{id}/types-bien/{itemId}")
    public ResponseEntity<Void> retirerTypeBien(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerTypeBien(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/profils")
    public List<ProspectionProfilDto> getProfils(@PathVariable UUID id) {
        return qualificationService.profils(id);
    }

    @PostMapping("/{id}/profils")
    public ProspectionProfilDto ajouterProfil(@PathVariable UUID id, @RequestBody AjouterProfilRequest request) {
        return qualificationService.ajouterProfil(id, request);
    }

    @DeleteMapping("/{id}/profils/{itemId}")
    public ResponseEntity<Void> retirerProfil(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerProfil(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/autres-tranches")
    @Operation(
            summary = "Liste les \"autres tranches\" (intérêts secondaires)",
            description = "Indépendant de la tranche/l'article principal (`ProspectionDetailDto."
                    + "trancheId`/`articleId`) — voir le guide dans la description du tag "
                    + "\"Prospections\" pour la distinction."
    )
    public List<ProspectionAutreTrancheDto> getAutresTranches(@PathVariable UUID id) {
        return qualificationService.autresTranches(id);
    }

    @PostMapping("/{id}/autres-tranches")
    @Operation(
            summary = "Ajoute un intérêt secondaire sur une autre tranche",
            description = "N'affecte jamais la tranche/l'article principal — utiliser "
                    + "`PATCH /api/prospections/{id}/tranche-principale` pour ça. `articleId`, "
                    + "s'il est fourni, doit appartenir à `trancheId` (400 sinon)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intérêt secondaire ajouté."),
            @ApiResponse(responseCode = "400", description = "`trancheId` manquant/introuvable, ou "
                    + "l'article fourni n'appartient pas à la tranche fournie."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ProspectionAutreTrancheDto ajouterAutreTranche(@PathVariable UUID id, @RequestBody AjouterAutreTrancheRequest request) {
        return qualificationService.ajouterAutreTranche(id, request);
    }

    @DeleteMapping("/{id}/autres-tranches/{itemId}")
    public ResponseEntity<Void> retirerAutreTranche(@PathVariable UUID id, @PathVariable UUID itemId) {
        qualificationService.retirerAutreTranche(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // -- GAP-07 (low priority): devis --

    @GetMapping("/{id}/devis")
    @Operation(
            summary = "Liste les offres (devis) d'une prospection",
            description = "Chaque offre inclut son détail HT/TTC complet (voir `montants`), "
                    + "reproduisant l'onglet \"Montants\" du formulaire Offre legacy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page d'offres (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public PageDto<DevisDto> getDevis(@PathVariable UUID id,
                                       @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(devisService.findByProspection(id, pageable));
    }

    @GetMapping("/{id}/devis/{devisId}")
    @Operation(
            summary = "Détail d'une offre (devis)",
            description = "Détail complet d'une offre, y compris le HT/TTC de l'onglet "
                    + "\"Montants\" (`montants`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'offre."),
            @ApiResponse(responseCode = "404", description = "Aucune offre avec cet id sur cette prospection.")
    })
    public DevisDto getDevisById(@PathVariable UUID id, @PathVariable UUID devisId) {
        return devisService.findById(id, devisId);
    }

    @PostMapping("/{id}/devis")
    @Operation(
            summary = "Crée une offre (devis) sur une prospection",
            description = "Si `articleId` est fourni, les montants HT/TTC/TVA/prix au m² sont "
                    + "calculés à partir du prix commercial de l'article (`ComSalePrice(HT)`, "
                    + "`Vat`) et, s'ils sont fournis, des `sousArticleIds` — voir `CreerDevisRequest` "
                    + "pour le détail du calcul et des remplacements possibles (`prix`, "
                    + "`remisePercent`). Sans `articleId`, l'offre est \"libre\": `prix` devient "
                    + "directement le prix de vente TTC, sans ventilation HT/m²/sous-articles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre créée."),
            @ApiResponse(responseCode = "400", description = "`trancheId` manquant/introuvable, "
                    + "`prix` manquant alors qu'aucun `articleId` n'est fourni, ou un id "
                    + "(article/sous-article/prospect/commercial/mode de paiement) est introuvable."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public DevisDto creerDevis(@PathVariable UUID id, @RequestBody CreerDevisRequest request) {
        return devisService.creerDevis(id, request);
    }

    @GetMapping("/{id}/devis/{devisId}/document")
    @Operation(
            summary = "Génère la \"Fiche Offre vente\" (PDF) d'une offre",
            description = "Reproduit exactement le formulaire Offre legacy (entête, onglet "
                    + "\"Montants\" HT/TTC, tableau des sous-articles avec leur propre remise) — "
                    + "voir `instructions/templates/priceOffer/OFFRE DE PRIX.pdf`. Le tableau "
                    + "\"SOUS ARTICLES\" n'apparaît que si l'offre a été créée avec des "
                    + "`sousArticleIds`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le PDF de la fiche offre vente."),
            @ApiResponse(responseCode = "404", description = "Aucune offre avec cet id sur cette prospection.")
    })
    public ResponseEntity<byte[]> getDevisDocument(@PathVariable UUID id, @PathVariable UUID devisId) {
        OffreDevisData data = offreDevisDocumentService.collect(id, devisId);
        byte[] pdf = offreDevisPdfRenderer.render(data);
        String nomFichier = "offre-" + (data.code() != null ? data.code().replace('/', '-') : devisId) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @GetMapping("/{id}/documents/nda")
    @Operation(
            summary = "Génère la \"NDA AYAD PRIVATE\" (PDF) du prospect de la prospection",
            description = "Formulaire \"Expression of Interest Form / Non-Disclosure Agreement\" "
                    + "(en anglais, reproduisant le formulaire legacy tel quel) — état civil et "
                    + "coordonnées du prospect lié à la prospection (`ComProspection.cfgProspect`). "
                    + "L'en-tête/pied de page AYAD Private est fixe (même contenu quel que soit le "
                    + "projet visé par la prospection — le NDA se signe en amont du choix d'un "
                    + "programme)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public ResponseEntity<byte[]> getNda(@PathVariable UUID id) {
        NdaData data = ndaService.collect(id);
        byte[] pdf = ndaPdfRenderer.render(data);
        String nomFichier = "nda-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }

    @GetMapping("/{id}/documents/fiche")
    @Operation(
            summary = "Génère la \"Fiche Prospection\" (PDF)",
            description = "Modélisé sur le template legacy \"FICHE\" (prospection) — récapitulatif "
                    + "imprimable reprenant les mêmes champs que `GET /api/prospections/{id}` "
                    + "(entête, budget/superficie recherchés, financement, alerte, notes) plus les "
                    + "15 dernières actions journalisées (`GET /api/prospections/{id}/interactions`). "
                    + "La section \"PROFIL\" (catégorie/type/profil de prospection) du template "
                    + "legacy n'est pas reproduite ici — hors périmètre de cette itération."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF généré."),
            @ApiResponse(responseCode = "404", description = "Prospection introuvable.")
    })
    public ResponseEntity<byte[]> getFiche(@PathVariable UUID id) {
        ProspectionFicheData data = prospectionFicheService.collect(id);
        byte[] pdf = prospectionFichePdfRenderer.render(data);
        String nomFichier = "fiche-prospection-" + (data.code() != null ? data.code().replace('/', '-') : id) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(pdf);
    }
}
