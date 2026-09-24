package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
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
import com.tsix.immocrm.dto.prospection.FiltresAvancesProspection;
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
import com.tsix.immocrm.dto.prospection.EnvoyerOffrePrixRequest;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiOffreDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.services.prospection.ConfirmationVisiteService;
import com.tsix.immocrm.services.prospection.OffrePrixService;
import com.tsix.immocrm.services.prospection.DevisService;
import com.tsix.immocrm.services.prospection.NdaService;
import com.tsix.immocrm.services.prospection.OffreDevisDocumentService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.services.prospection.ProspectionQualificationService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.prospection.ProspectionFicheService;
import com.tsix.immocrm.services.prospection.document.NdaPdfRenderer;
import com.tsix.immocrm.services.prospection.document.OffreDevisPdfRenderer;
import com.tsix.immocrm.services.prospection.document.ProspectionFichePdfRenderer;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
        + "d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.\n\n"
        + "**Entités juridiques en exploitation.** Ne sont renvoyées que les prospections "
        + "rattachées à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et "
        + "GHRIZA, comme dans l'application de bureau. Restriction **globale et permanente**, "
        + "distincte du périmètre commercial : elle s'applique à tout appelant, rôles exemptés "
        + "compris. Une prospection **sans tranche** n'est rattachée à aucune entité et reste donc "
        + "visible — le lead n'est pas perdu.")
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
    private final ConfirmationVisiteService confirmationVisiteService;
    private final OffrePrixService offrePrixService;
    private final ProspectSubResourcesService subResourcesService;

    public ProspectionController(ProspectionService prospectionService,
                                  ProspectionQualificationService qualificationService,
                                  DevisService devisService,
                                  OffreDevisDocumentService offreDevisDocumentService,
                                  OffreDevisPdfRenderer offreDevisPdfRenderer,
                                  NdaService ndaService,
                                  NdaPdfRenderer ndaPdfRenderer,
                                  ProspectionFicheService prospectionFicheService,
                                  ProspectionFichePdfRenderer prospectionFichePdfRenderer,
                                  ConfirmationVisiteService confirmationVisiteService,
                                  OffrePrixService offrePrixService,
                                  ProspectSubResourcesService subResourcesService) {
        this.confirmationVisiteService = confirmationVisiteService;
        this.offrePrixService = offrePrixService;
        this.prospectionService = prospectionService;
        this.qualificationService = qualificationService;
        this.devisService = devisService;
        this.offreDevisDocumentService = offreDevisDocumentService;
        this.offreDevisPdfRenderer = offreDevisPdfRenderer;
        this.ndaService = ndaService;
        this.ndaPdfRenderer = ndaPdfRenderer;
        this.prospectionFicheService = prospectionFicheService;
        this.prospectionFichePdfRenderer = prospectionFichePdfRenderer;
        this.subResourcesService = subResourcesService;
    }

    @GetMapping
    @ApiResponse(responseCode = "400",
            description = "Un nom passé à `colonnes` ne figure pas au catalogue. Le corps "
                    + "(ProblemDetail) porte `colonne` et `colonnesValides`.")
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
            @Parameter(description = "Colonnes à renvoyer. Répétable ou séparé par des virgules. "
                    + "**Absent = comportement historique à l'identique** : les colonnes du groupe "
                    + "`ACTIVITE` sont omises et la vue légataire n'est pas interrogée. `id` est "
                    + "toujours renvoyé, demandé ou non. Un nom hors catalogue renvoie 400 — voir "
                    + "`GET /api/referentiels/colonnes/prospections` pour la liste exacte.",
                    example = "code,prospectNom,joursInactivite,nbInteractions")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(description = "Déplie un groupe : restreint la liste aux prospections d'un "
                    + "groupe renvoyé par `GET /api/prospections/groupes`. Attend le nom de la "
                    + "colonne qui a servi au groupement.", example = "etape")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(description = "Valeur du groupe à déplier — **la `cle` renvoyée telle quelle "
                    + "par `/groupes`**. Omise alors que `groupeColonne` est fourni : le groupe "
                    + "des prospections sans valeur sur cette colonne.")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date. Doit être "
                    + "identique à celle passée à `/groupes`. Défaut : MOIS.")
            @RequestParam(required = false) Granularite granularite,
            @ModelAttribute FiltresAvancesProspection filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        // Résolu ici pour que le 400 parte avant toute requête. L'omission des clés non demandées
        // est appliquée à la sérialisation par SelectionColonnesPropertyFilter.
        Set<ColonneProspection> selection = ColonneProspection.resoudre(colonnes);
        ColonneProspection groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneProspection::resoudreGroupement);
        return PageDto.of(prospectionService.findAll(etape, origine, commercial, trancheId, dateDebut, dateFin,
                search, ouvertes, filtres, selection, groupe, groupeValeur,
                Groupements.granularite(groupe, granularite), pageable));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des prospections repliée en groupes, avec effectifs et totaux",
            description = """
                    Reproduit la bande de groupement du logiciel de bureau : un groupe par valeur \
                    distincte de `groupBy`, portant son effectif et les sommes demandées.

                    **Les effectifs portent sur tout le périmètre filtré, pas sur une page** — \
                    c'est la raison d'être de cet endpoint : la liste étant paginée, grouper côté \
                    front ne grouperait qu'une page et fausserait tous les totaux.

                    **Filtres identiques à `GET /api/prospections`**, filtres avancés et périmètre \
                    commercial compris. Pour ouvrir un groupe : `GET /api/prospections\
                    ?groupeColonne=<groupBy>&groupeValeur=<cle>`.

                    **Seules les colonnes servies par l'entité sont groupables.** Les colonnes du \
                    groupe `ACTIVITE` (`joursInactivite`, `nbInteractions`…) et de `CONVERSION` \
                    viennent de la vue légataire `ComSaleProspectionView`, chargée par une requête \
                    distincte : elles n'existent pas dans le graphe d'entités et ne peuvent donc \
                    pas servir de clé de groupement. `GET /api/referentiels/colonnes/prospections` \
                    donne la liste exacte via le booléen `groupable`.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", description =
                            "`groupBy` absent, inconnu ou non groupable ; ou `agregats` contenant "
                            + "une colonne non agrégeable. Le corps porte la liste des noms "
                            + "acceptés.", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau.",
                    example = "etape", required = true)
            @RequestParam String groupBy,
            @Parameter(description = "Agrégats à calculer, répétable ou séparés par des virgules.\n\n"
                    + "Deux formes. Un **nom nu** (`prix`) vaut une **somme** et revient sous ce nom "
                    + "nu — c'est le comportement historique, inchangé. Un nom **qualifié** "
                    + "(`prix:AVG`) applique la fonction nommée et revient sous le jeton complet "
                    + "`prix:AVG`. Les deux formes peuvent coexister dans le même appel.\n\n"
                    + "Fonctions : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT` — mais **pas sur toutes les "
                    + "colonnes**. Sommer une date n'a pas de sens, et une colonne calculée par "
                    + "différence n'a pas d'extremum exprimable. La liste autorisée par colonne est "
                    + "publiée par le catalogue (`fonctions`) : le menu Σ doit s'y piloter, pas sur "
                    + "une liste en dur. Une fonction refusée rend 400 avec les fonctions acceptées "
                    + "par cette colonne-là.\n\n"
                    + "`AVG` est calculée à partir de la somme et de l'effectif renseigné, en "
                    + "décimal exact et arrondie au centime — jamais en virgule flottante.\n\n"
                    + "Absent : seul l'effectif de chaque groupe est renvoyé.",
                    example = "prix,prix:MAX")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité si `groupBy` est une date. Défaut : MOIS — "
                    + "grouper sur la date exacte rendrait 16 588 groupes pour 17 939 lignes.",
                    example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (croissant, défaut) ou NOMBRE (effectif décroissant).")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(description = "Inclure le groupe des lignes dont la colonne groupée est "
                    + "vide. **Faux par défaut** : l'écran veut les valeurs qui existent, et sur "
                    + "une colonne peu renseignée le groupe vide écraserait tous les autres. "
                    + "Passer `true` pour le récupérer — la somme des effectifs retombe alors sur "
                    + "le `totalElements` de la liste non groupée.", example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<String> etape,
            @RequestParam(required = false) List<String> origine,
            @RequestParam(required = false) List<UUID> commercial,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean ouvertes,
            @ModelAttribute FiltresAvancesProspection filtres,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneProspection groupe = ColonneProspection.resoudreGroupement(groupBy);
        return prospectionService.grouperProspections(etape, origine, commercial, trancheId,
                dateDebut, dateFin, search, ouvertes, filtres, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneProspection.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
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
                    + "imbriquées (interactions, devis, qualification, documents).\n\n"
                    + "**Deux origines de données, deux comportements** :\n"
                    + "- les champs issus de `ComProspection` et de ses associations (société, "
                    + "type de tiers, coordonnées, codes de référentiel, audit) sont servis "
                    + "**dans tous les cas**, pré-prospection comprise ;\n"
                    + "- le bloc `activite` vient de `ComSaleProspectionView`, qui exclut les "
                    + "pré-prospections : il vaut alors `null` **en entier**. `null` signifie "
                    + "« non disponible », jamais « zéro ».\n\n"
                    + "Les champs mesurés morts (`prix`, `surface`, avance, financement, "
                    + "`proposeur*`) restent exposés pour ne pas rompre le contrat existant, mais "
                    + "ne doivent pas être affichés — voir leur `@Schema`.\n\n"
                    + "**Tri manuel et déduplication** : `marqueurTri` (`DOUBLON`, `SPAM`, "
                    + "`TRAITE`) et `doublonDe*` exposent un mécanisme que l'équipe tenait dans "
                    + "des champs libres. **L'endpoint les expose sans rien filtrer** — 798 "
                    + "pré-prospections sont marquées doublon ou spam et 795 d'entre elles sont "
                    + "encore à l'étape `1 A TRAITER` : c'est au client de décider du traitement. "
                    + "Voir `instructions/FEATURE-marqueurs-tri-pre-prospection.md`.\n\n"
                    + "**Sur une pré-prospection**, `fermee` (0 %), les champs `article*` (0,2 %) "
                    + "et `proposeur*` (0 %) sont structurellement vides — ne pas les attendre."
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
                    + "préalable via `POST /api/prospects`, déjà câblé côté front).\n\n"
                    + "**Champs obligatoires (alignés sur l'écran légataire OMRAN)** : `prospectId`, "
                    + "`notes` (Notes, non vide), `origineId` (Source de contact), `typeContactId` "
                    + "(Type — attention, c'est `ComActionMessageType`, à ne pas confondre avec la "
                    + "vocation `vocationId`), `etapeId` (Nature) et `trancheId` (Tranche). Un de "
                    + "ces champs manquant ⟶ 400 nommant le champ. `commercialId` reste optionnel "
                    + "(OMRAN ne le bloque pas à la création). Les autres champs sont optionnels et "
                    + "suivent la même sémantique que `PATCH /api/prospections/{id}` (mise à jour "
                    + "uniquement des champs fournis). `code` est toujours généré serveur (compteur "
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
            @ApiResponse(responseCode = "400", description = "Un champ obligatoire est manquant "
                    + "(`prospectId`, `notes`, `origineId`, `typeContactId`, `etapeId`, "
                    + "`trancheId`), ou un id fourni "
                    + "(prospect/commercial/tranche/article/origine/type/nature/...) est "
                    + "introuvable, ou l'article fourni n'appartient pas à la tranche fournie."),
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
    @Operation(
            summary = "Transforme une pré-prospection en prospection (commercial + tranche)",
            description = "Dernière étape de qualification d'un lead : affecte le commercial et "
                    + "**garantit qu'une tranche (projet) est rattachée**. Requiert qu'un prospect "
                    + "soit déjà lié via `POST /api/pre-prospections/{id}/creer-prospect` — 400 sinon.\n\n"
                    + "**La tranche est obligatoire.** Règle métier plus stricte que le légataire "
                    + "(qui n'impose rien au niveau base — seule `PKey` est NOT NULL sur "
                    + "`ComProspection`), justifiée sur données réelles : 99,83 % des prospections "
                    + "qualifiées (14 700/14 725) portent déjà une tranche. Fournir `trancheId` pour "
                    + "la poser/remplacer ici (avec périmètre commercial et cohérence `articleId`, "
                    + "comme `PATCH .../tranche-principale`) ; si `trancheId` est omis, la "
                    + "prospection doit **déjà** en porter une, faute de quoi la transformation "
                    + "est refusée (400)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection transformée : commercial "
                    + "affecté et tranche garantie."),
            @ApiResponse(responseCode = "400", description = "`commercialId` manquant ; aucun "
                    + "prospect lié ; commercial/tranche/article introuvable ; l'article ne "
                    + "appartient pas à la tranche ; ou aucune tranche fournie ni déjà présente."),
            @ApiResponse(responseCode = "403", description = "Le projet de la tranche fournie n'est "
                    + "pas dans le périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ProspectionDetailDto transformer(@PathVariable UUID id, @RequestBody TransformerProspectionRequest request) {
        return prospectionService.transformer(id, request);
    }

    @PatchMapping("/{id}/etape")
    public ProspectionDetailDto changerEtape(@PathVariable UUID id, @RequestBody ChangerEtapeRequest request) {
        return prospectionService.changerEtape(id, request);
    }

    @PatchMapping("/{id}/archiver")
    @Operation(
            summary = "Archive une prospection (clôture avec motif)",
            description = "Sort la prospection du pipeline : `fermee = true`, `dateFermeture` = "
                    + "maintenant, motif posé depuis `raisonId` (référentiel `ComProspectionClosingReason`) "
                    + "et/ou `raisonTexte` (libre) — **au moins l'un des deux est requis** (400 sinon).\n\n"
                    + "Reproduit la séquence du légataire relevée dans sa trace d'audit : les "
                    + "**relances en cours de la prospection sont désarmées** (`WithoutRevival`), "
                    + "comme lors d'une transformation en vente — elles disparaissent du tableau de "
                    + "bord des relances. Les relances rattachées à une échéance de dossier ne sont "
                    + "pas touchées.\n\n"
                    + "Réversible par `PATCH /api/prospections/{id}/desarchiver`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection archivée (`fermee = true`)."),
            @ApiResponse(responseCode = "400", description = "Ni `raisonId` ni `raisonTexte` fourni, "
                    + "ou `raisonId` inconnu."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant.")
    })
    public ProspectionDetailDto archiver(@PathVariable UUID id, @RequestBody ArchiverProspectionRequest request) {
        return prospectionService.archiver(id, request);
    }

    @PatchMapping("/{id}/desarchiver")
    @Operation(
            summary = "Désarchive une prospection (la rouvre dans le pipeline)",
            description = "Inverse de `archiver` : `fermee` repasse à `false` et la prospection "
                    + "réapparaît dans la liste `?ouvertes=true`. Sans corps.\n\n"
                    + "**Fidèle au légataire : seule `fermee` change.** `dateFermeture`, "
                    + "`raisonFermeture*` sont **conservés** (OMRAN ne les efface pas — 88 prospections "
                    + "vente ouvertes en base réelle portent encore date et motif de clôture, ce sont "
                    + "des désarchivées), et les relances désarmées à l'archivage ne sont pas "
                    + "réarmées. **Le badge « Archivée » doit donc se brancher sur `fermee`, jamais "
                    + "sur la présence de `dateFermeture`.**"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospection rouverte (`fermee = false`, "
                    + "date et motif de clôture toujours renseignés)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409", description = "La prospection n'est pas archivée "
                    + "(`fermee` false ou absent) — rien à rouvrir.")
    })
    public ProspectionDetailDto desarchiver(
            @Parameter(description = "Id de la prospection archivée.") @PathVariable UUID id) {
        return prospectionService.desarchiver(id);
    }

    @GetMapping("/{id}/interactions")
    @Operation(
            summary = "Journal des interactions d'une prospection",
            description = "Historique des interactions (appels, messages, notes, visites...) "
                    + "loggées sur cette prospection, les plus récentes en premier.\n\n"
                    + "**Afficher `note`, pas `description`.** `note` porte le texte saisi par "
                    + "le commercial. `description` est un libellé généré valant "
                    + "`JJ/MM/AAAA HH:MM <auteur>` sur 100 % des lignes réelles — soit "
                    + "exactement `date` + `employeNom`, déjà renvoyés séparément. Il n'est "
                    + "conservé que pour ne rien casser."
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
                    + "calendrier est créée, visible ensuite via GET /api/calendrier/evenements.\n\n"
                    + "**Une confirmation part alors automatiquement au prospect, par email ET "
                    + "par WhatsApp** (date, heure, adresse du projet, conseiller ; les plans "
                    + "seulement par email). Les deux canaux sont indépendants — 57 % des "
                    + "prospects ont une adresse email, 99 % un numéro exploitable, donc "
                    + "l'essentiel est prévenu par au moins un des deux. L'envoi a lieu après "
                    + "validation de la transaction et hors du temps de réponse : il ne peut ni "
                    + "retarder ni faire échouer cet appel, et la réponse ne dit pas s'il a "
                    + "abouti. Pour connaître l'issue, ou pour renvoyer après avoir corrigé une "
                    + "coordonnée, voir "
                    + "POST /api/prospections/{id}/interactions/{interactionId}/email-visite.\n\n"
                    + "**Le corps de requête ne change pas** : `description` reste le champ "
                    + "d'entrée et reste obligatoire. Seule sa destination en base a été "
                    + "corrigée — il alimente désormais le champ `note` de la réponse, et non "
                    + "plus le libellé. Le libellé `description` de la réponse est **généré par "
                    + "le serveur** (`JJ/MM/AAAA HH:MM <auteur>`) et ne peut pas être imposé : "
                    + "c'est ce qui rend les interactions créées ici indiscernables de celles "
                    + "de l'application de bureau.\n\n"
                    + "L'**auteur** est déduit du jeton d'authentification, jamais du corps : "
                    + "le laisser choisir fausserait les KPI de performance commerciale."
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

    @PostMapping("/{id}/interactions/{interactionId}/email-visite")
    @Operation(
            summary = "Renvoie au prospect la confirmation de sa visite (email + WhatsApp)",
            description = "Réexpédie la confirmation déjà envoyée automatiquement à la création de "
                    + "l'interaction. Deux usages : le prospect n'avait ni adresse email ni numéro "
                    + "exploitable au moment de la prise de rendez-vous et vient d'en fournir un, "
                    + "ou il déclare n'avoir rien reçu.\n\n"
                    + "**Deux canaux, tentés indépendamment.** La réponse porte un bloc `email` et "
                    + "un bloc `whatsapp`, chacun avec son propre `envoye`/`motif`/`destinataire` ; "
                    + "le `envoye` de premier niveau vaut vrai dès qu'**au moins un** a abouti. "
                    + "L'asymétrie est la norme et non l'exception : **99 %** des prospections avec "
                    + "rendez-vous portent un numéro exploitable, contre **57 %** une adresse "
                    + "email.\n\n"
                    + "Le contenu est **recalculé à chaque appel**, jamais rejoué : une adresse de "
                    + "projet corrigée ou un plan déposé depuis la prise de rendez-vous seront donc "
                    + "bien dans le message renvoyé.\n\n"
                    + "**Un envoi qui n'a pas lieu n'est pas une erreur.** Un prospect sans "
                    + "destinataire exploitable donne **200** avec `envoye = false` et un motif "
                    + "explicite : le commercial doit voir pourquoi rien n'est parti, ce qu'un code "
                    + "4xx ne dirait pas. `DESTINATAIRE_ABSENT` signifie qu'il faut obtenir "
                    + "l'information ; `NUMERO_INEXPLOITABLE` qu'un numéro existe mais qu'il est à "
                    + "réparer dans la fiche (43 cas réels : longueurs anormales et numéros fixes). "
                    + "Seuls l'introuvable (404) et l'interaction qui ne planifie aucun rendez-vous "
                    + "(409) sont des erreurs.\n\n"
                    + "**Contenu des messages** : date et heure du rendez-vous, projet visité, "
                    + "adresse (celle de la tranche si elle est renseignée, sinon celle du projet — "
                    + "le bloc est omis plutôt qu'approximatif quand la donnée n'est pas "
                    + "exploitable), nom et mobile du conseiller. Les **plans ne partent que par "
                    + "email** : l'envoi d'un document par UltraMsg exige une URL publique, or les "
                    + "URL du stockage expirent en quinze minutes. Ce sont ceux du lot lorsqu'il "
                    + "est connu — ce qui n'est le cas que dans **3 %** des prises de rendez-vous — "
                    + "sinon ceux déposés au niveau du projet ; `nombrePiecesJointes = 0` est donc "
                    + "la valeur normale aujourd'hui."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tentative effectuée — lire `envoye` "
                    + "et `motif` pour en connaître l'issue."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, "
                    + "prospection hors du périmètre commercial de l'appelant, ou interaction "
                    + "inconnue / rattachée à une autre prospection."),
            @ApiResponse(responseCode = "409", description = "Cette interaction ne planifie aucun "
                    + "rendez-vous : il n'y a rien à confirmer.")
    })
    public ResultatEnvoiVisiteDto renvoyerEmailVisite(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @Parameter(description = "Id de l'interaction portant le rendez-vous — celle dont le "
                    + "résultat vaut \"Rendez vous pris\", \"RDV reporté\" ou \"Le prospect veut "
                    + "faire une visite du projet\".") @PathVariable UUID interactionId) {
        return confirmationVisiteService.renvoyer(id, interactionId);
    }

    @PostMapping("/{id}/offre-prix")
    @Operation(
            summary = "Établit l'offre de prix et l'envoie au prospect (email + WhatsApp)",
            description = "Second temps de l'après-visite. Le **remerciement** part automatiquement "
                    + "au passage à l'étape « 4 APRES VISITES » (`PATCH /api/prospections/{id}/etape`) "
                    + "et ne demande rien ; **l'offre de prix**, elle, s'envoie explicitement par cet "
                    + "endpoint, parce qu'elle suppose un prix que seul un humain peut arrêter.\n\n"
                    + "**Pourquoi ce n'est pas automatique.** Le prix vient du lot, et le lot n'est "
                    + "renseigné que sur **7 %** des prospections de cette étape (94 sur 1 266). Sans "
                    + "lot il n'y a aucun montant à annoncer — d'où le **409** ci-dessous, qui est le "
                    + "cas majoritaire attendu et doit être traité comme une étape de parcours, pas "
                    + "comme une panne.\n\n"
                    + "**Ce que fait l'appel**, dans l'ordre : réutilise l'offre existante si la "
                    + "prospection en porte déjà une **et** que l'appel n'impose ni remise ni prix "
                    + "(réécrire les montants modifierait un document que le prospect détient) ; sinon "
                    + "crée une offre (`StlQuote`) numérotée comme le legacy ; rend la Fiche Offre en "
                    + "PDF ; expédie sur les deux canaux.\n\n"
                    + "**Le WhatsApp porte le montant en clair**, contrairement à la confirmation de "
                    + "visite. À cette étape le rapport s'inverse : **19,7 %** des prospects ont une "
                    + "adresse email contre **99,7 %** un numéro exploitable. Le PDF ne part que par "
                    + "email — UltraMsg exige une URL publique et les nôtres expirent en quinze "
                    + "minutes — et le message WhatsApp **ne mentionne le mail que s'il est "
                    + "réellement parti**.\n\n"
                    + "**Un envoi qui n'a pas lieu n'est pas une erreur** : 200 avec `envoye = false` "
                    + "et un motif par canal. L'offre est créée avant l'envoi et le reste quoi qu'il "
                    + "arrive — `offreId` et `offreCode` sont toujours renseignés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre établie et envoi tenté — lire "
                    + "`envoye` et le `motif` de chaque canal pour en connaître l'issue."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id, ou "
                    + "prospection hors du périmètre commercial de l'appelant."),
            @ApiResponse(responseCode = "409", description = "La prospection n'a pas de lot "
                    + "(rattachez-le via `PATCH /api/prospections/{id}/tranche-principale` puis "
                    + "relancez), ou elle n'est pas à l'étape « 4 APRES VISITES ».")
    })
    public ResultatEnvoiOffreDto envoyerOffrePrix(
            @Parameter(description = "Id de la prospection.") @PathVariable UUID id,
            @RequestBody(required = false) EnvoyerOffrePrixRequest request) {
        return offrePrixService.envoyer(id, request);
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

    @GetMapping("/{id}/contacts")
    @Operation(
            summary = "Contacts du prospect de la prospection",
            description = "Les contacts (conjoint, notaire, apporteur…) du prospect lié à cette "
                    + "prospection — lignes `CfgTierContact`, jointes au `CfgTier` de chaque "
                    + "personne pour le nom, le mobile, le téléphone et l'e-mail. Destiné à "
                    + "accompagner l'affichage du détail de la prospection.\n\n"
                    + "Une pré-prospection (sans prospect encore lié) renvoie une **liste vide**, "
                    + "jamais une erreur : le lead n'est pas perdu."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des contacts (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public List<ContactProspectDto> getContacts(@PathVariable UUID id) {
        return subResourcesService.contactsDeProspection(id);
    }

    @PostMapping("/{id}/contacts")
    @Operation(
            summary = "Ajoute un contact au prospect de la prospection",
            description = "Rattache un contact au prospect lié à la prospection. La personne-contact "
                    + "est soit un `CfgTier` existant (`contactProspectId`), soit **créée à la "
                    + "volée** (`nouveauContact` : nom, prénom, mobile, téléphone, e-mail…) puis "
                    + "liée — c'est ce que fait l'écran légataire, qui insère un nouveau `CfgTier` "
                    + "puis le lien `CfgTierContact`. Fournir l'un **ou** l'autre. `relationTypeId` "
                    + "(type de relation, p.ex. « Conjoint ») est obligatoire.\n\n"
                    + "Même effet que `POST /api/prospects/{prospectId}/contacts`, mais adressé par "
                    + "la prospection : le prospect est résolu depuis elle."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact créé/lié et rattaché."),
            @ApiResponse(responseCode = "400", description = "`relationTypeId` manquant ; ni "
                    + "`contactProspectId` ni `nouveauContact` (ou les deux) ; référentiel "
                    + "introuvable ; ou la prospection n'a pas encore de prospect."),
            @ApiResponse(responseCode = "404", description = "Aucune prospection avec cet id.")
    })
    public ContactProspectDto ajouterContact(@PathVariable UUID id, @RequestBody CreerContactProspectRequest request) {
        return subResourcesService.ajouterContactAProspection(id, request);
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
