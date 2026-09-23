package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospection.CreerPreProspectionRequest;
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

@Tag(name = "Pré-prospections", description = "Leads bruts non encore qualifiés : ComProspection de "
        + "type légataire `ComPreProspection` (`StrEntity`), le discriminant d'OMRAN lui-même — une "
        + "pré-prospection peut donc déjà porter un prospect ou un commercial tant qu'elle n'a pas "
        + "été transformée. Même forme que /api/prospections.\n\n"
        + "Collection soumise au périmètre commercial : sur **toutes** les routes portant un "
        + "identifiant, une ressource hors du périmètre de l'appelant renvoie 404 (jamais 403 — un "
        + "403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie "
        + "404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le "
        + "contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.\n\n"
        + "**Entités juridiques en exploitation.** Ne sont renvoyées que les pré-prospections "
        + "rattachées à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et "
        + "GHRIZA. Restriction **globale et permanente**, distincte du périmètre commercial : elle "
        + "s'applique à tout appelant, rôles exemptés compris. Une pré-prospection **sans tranche** "
        + "n'est rattachée à aucune entité et reste donc visible — le lead n'est pas perdu.")
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
            summary = "Liste paginée des pré-prospections",
            description = "Mêmes filtres que GET /api/prospections (étape, origine, commercial, "
                    + "tranche(s), plage de dates, recherche libre, ouvertes) appliqués aux leads "
                    + "non encore qualifiés. Voir instructions/filtres-avances.md point 5.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint, cette liste ne "
                    + "contient que les pré-prospections de ses projets affectés, scopées par le "
                    + "projet de leur tranche visée. Les pré-prospections sans tranche restent "
                    + "visibles de tous. Le filtre `commercial` s'applique : une pré-prospection "
                    + "saisie dans OMRAN peut déjà porter un commercial (~780 en base réelle)."
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
            @Parameter(description = "Borne inférieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) sur la date du document, "
                    + "format ISO-8601.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean ouvertes,
            @Parameter(description = "Déplie un groupe : restreint la liste aux pré-prospections "
                    + "d'un groupe renvoyé par `GET /api/pre-prospections/groupes`.",
                    example = "origine")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(description = "Valeur du groupe à déplier — la `cle` renvoyée telle quelle "
                    + "par `/groupes`. Omise avec `groupeColonne` fourni : le groupe sans valeur.")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date. Défaut : MOIS.")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneProspection groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneProspection::resoudreGroupement);
        return PageDto.of(prospectionService.findPreProspections(etape, origine, commercial, trancheId, dateDebut,
                dateFin, search, ouvertes, groupe, groupeValeur,
                Groupements.granularite(groupe, granularite), pageable));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des pré-prospections repliée en groupes, avec effectifs et totaux",
            description = """
                    Même mécanique que `GET /api/prospections/groupes`, appliquée au périmètre des \
                    pré-prospections (type légataire `ComPreProspection`).

                    **Les effectifs portent sur tout le périmètre filtré, pas sur une page.** Pour \
                    ouvrir un groupe : `GET /api/pre-prospections?groupeColonne=<groupBy>\
                    &groupeValeur=<cle>`.

                    `prospectNom` et `commercialNom` sont nuls sur la plupart des lignes (leads \
                    non encore qualifiés) : grouper dessus donne surtout le groupe sans valeur.""",
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
                    + "Absent : seul l'effectif de chaque groupe est renvoyé.")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité si `groupBy` est une date. Défaut : MOIS.")
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
            summary = "Crée une pré-prospection (lead brut)",
            description = """
                    Équivalent de l'écran « Pré-prospection » d'OMRAN, dont la trace d'audit fixe \
                    la séquence : code tiré du compteur `AdmSequence` « Prospection » (format \
                    `AA/NNNNN`, **le même compteur que les prospections**), type de document \
                    `ComPreProspection`, contact saisi en champs libres (`TempDescription`, \
                    `TempTel`, `TempEMail`) — **aucune fiche prospect n'est créée**.

                    Seul `nom` est obligatoire. Posés par le serveur : code, date du document \
                    (maintenant), `IsClosed = false`, auteur (utilisateur connecté), vocation \
                    `VENTE` si aucune n'est fournie. Jamais de prospect ni de commercial : c'est \
                    l'objet de `POST /api/pre-prospections/{id}/creer-prospect` puis \
                    `POST /api/prospections/{id}/transformer`.

                    **Doublons signalés, jamais refusés.** La réponse liste les prospections \
                    existantes au même contact (même nom, même téléphone sur ses 9 derniers \
                    chiffres, même email — un champ absent des deux côtés compte comme égal). \
                    OMRAN enregistre lui aussi une ligne par soumission ; à l'écran d'avertir. \
                    Seuls les doublons **visibles de l'appelant** (périmètre commercial) sont \
                    listés : un contact hors périmètre n'est jamais révélé.

                    La ligne créée apparaît aussitôt dans `GET /api/pre-prospections` et dans le \
                    logiciel de bureau.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pré-prospection créée ; `doublons` vide "
                    + "si aucun contact identique n'existe."),
            @ApiResponse(responseCode = "400", description = "`nom` absent ou blanc ; champ trop long "
                    + "(nom/telephone/email 255, notes 1 000) ; référentiel inconnu (tranche, article, "
                    + "étape, origine, support, type, action, vocation) ; `articleId` sans `trancheId` "
                    + "ou hors de la tranche.", content = @Content),
            @ApiResponse(responseCode = "403", description = "La tranche fournie appartient à un projet "
                    + "hors du périmètre commercial de l'appelant.", content = @Content)
    })
    public ResponseEntity<PreProspectionCreeeDto> creer(@RequestBody CreerPreProspectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerPreProspection(request));
    }

    private static final String REGLES_IMPORT = """
            **Format attendu** — l'export CSV des leads (Meta / Leads Center), en-tête : \
            `Créé, Nom, Adresse e-mail, Source, Formulaire, Canal, Étape, Propriétaire, Étiquettes, \
            Téléphone, Numéro de téléphone secondaire, Numéro WhatsApp`.
            - Colonnes reconnues **par leur nom** (casse et accents ignorés), dans n'importe quel \
            ordre ; les colonnes inconnues sont ignorées. Seule `Nom` est indispensable (400 sinon).
            - Séparateur `,` ou `;` (un fichier réenregistré par Excel en français), déduit de \
            l'en-tête. Encodage UTF-8 (avec ou sans BOM), à défaut Windows-1252.
            - 5 000 lignes au plus (400 au-delà) ; lignes vides ignorées.

            **Ce que devient chaque colonne**
            | Colonne | Pré-prospection |
            |---|---|
            | Créé (`09/23/2026 1:45am`, mois en premier) | date du document `DocDate` — la date de soumission du lead |
            | Nom | `TempDescription` |
            | Adresse e-mail | `TempEMail` |
            | Téléphone (à défaut WhatsApp, à défaut secondaire) | `TempTel`, tel qu'écrit |
            | Formulaire, Source, Canal, Étape, Étiquettes, Propriétaire (sauf « Unassigned »), autres numéros | recopiés dans `Notes` |

            **Sort de chaque ligne** (`statut`) :
            - `NOUVELLE` — créée.
            - `DOUBLON` — même contact (nom, téléphone sur ses 9 derniers chiffres, email ; casse \
            ignorée) qu'une prospection existante ou qu'une ligne plus haut, mais **autre date** : \
            c'est une nouvelle soumission, **créée quand même** et signalée.
            - `DEJA_IMPORTEE` — même contact **et même date** (à la minute) : le même lead \
            réimporté, **ignoré**. Réimporter un fichier, ou un export qui chevauche le précédent, \
            ne crée donc rien deux fois.

            **Périmètre commercial** : seuls les doublons visibles de l'appelant sont nommés \
            (`doublonIds`, codes dans `message`). Un lead déjà importé sur un projet hors \
            périmètre est bien ignoré, avec le message « Lead déjà importé » et rien d'autre ; un \
            simple homonyme hors périmètre ne fait pas un `DOUBLON`.
            - `ERREUR` — nom vide, date vide ou illisible, champ trop long : **ignorée**, les \
            autres lignes passent.""";

    @PostMapping(value = "/import/apercu", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import CSV de leads — étape 1 : aperçu (rien n'est écrit)",
            description = """
                    Première étape de l'import : envoie le fichier, reçoit **tout ce qui en a été \
                    extrait**, ligne par ligne, avec le sort que l'import réservera à chacune. \
                    **Rien n'est écrit.**

                    **Un fichier = une tranche.** L'export ne dit pas à quelle tranche les leads \
                    appartiennent : après l'aperçu, l'écran **demande la tranche** (liste \
                    déroulante), et cette tranche unique vaudra pour toutes les lignes du fichier. \
                    Pour la pré-sélectionner, l'aperçu propose `trancheSuggereeId` : la tranche dont \
                    le code est le premier mot du formulaire (`APF FORM 02*2026 - GISK` → `APF`), \
                    quand tous les formulaires du fichier désignent la même. Un fichier qui mêle \
                    plusieurs tranches revient avec un `avertissements` : le découper.

                    Étape 2 : `POST /api/pre-prospections/import` avec **le même fichier** et la \
                    tranche choisie.

                    """ + REGLES_IMPORT)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rapport d'aperçu (`importe = false`, "
                    + "`preProspectionId` toujours null)."),
            @ApiResponse(responseCode = "400", description = "Fichier absent ou vide, colonne `Nom` "
                    + "introuvable, plus de 5 000 lignes.", content = @Content)
    })
    public RapportImportPreProspectionsDto apercuImport(
            @Parameter(description = "Le fichier CSV exporté, tel quel.")
            @RequestParam("fichier") MultipartFile fichier) {
        return importService.apercu(fichier);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import CSV de leads — étape 2 : création, toutes lignes sur une seule tranche",
            description = """
                    Crée une pré-prospection par ligne `NOUVELLE` ou `DOUBLON` du fichier, **toutes \
                    rattachées à `trancheId`** — la tranche choisie par l'utilisateur après \
                    l'aperçu (`POST /api/pre-prospections/import/apercu`). **Une seule tranche par \
                    fichier** : un fichier qui en mêle plusieurs doit être découpé avant l'import.

                    Chaque ligne créée est une pré-prospection complète, exactement comme \
                    `POST /api/pre-prospections` : code `AA/NNNNN` du compteur « Prospection », type \
                    `ComPreProspection`, `IsClosed = false`, auteur = utilisateur connecté, \
                    vocation `VENTE`. Référentiels posés par défaut — le motif des leads déjà \
                    injectés depuis ces formulaires — chacun remplaçable par son paramètre : \
                    Nature `1 A TRAITER` (`etapeId`), source `FACEBOOK` (`origineId`), type `WEB` \
                    (`typeContactId`). Un défaut absent du référentiel est laissé vide.

                    **Tout ou rien** pour les erreurs de requête (tranche inconnue ou hors \
                    périmètre : aucune ligne écrite). Les lignes en `ERREUR` n'empêchent pas les \
                    autres. Le rapport renvoyé a la forme de l'aperçu, avec `importe = true` et \
                    l'identifiant et le code de chaque pré-prospection créée.

                    """ + REGLES_IMPORT)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import effectué ; rapport ligne par ligne."),
            @ApiResponse(responseCode = "400", description = "`trancheId` absent ou inconnu ; "
                    + "fichier absent, vide, sans colonne `Nom` ou de plus de 5 000 lignes ; "
                    + "référentiel fourni inconnu.", content = @Content),
            @ApiResponse(responseCode = "403", description = "La tranche appartient à un projet hors "
                    + "du périmètre commercial de l'appelant — rien n'est écrit.", content = @Content)
    })
    public RapportImportPreProspectionsDto importer(
            @Parameter(description = "Le fichier CSV exporté — le même que pour l'aperçu.")
            @RequestParam("fichier") MultipartFile fichier,
            @Parameter(description = "Tranche (CfgTranche.id) choisie dans la liste déroulante. "
                    + "**Obligatoire** : toutes les lignes du fichier y sont rattachées.")
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
            @RequestParam(required = false) UUID actionCommercialeId) {
        return importService.importer(fichier, trancheId, new ImportPreProspectionService.Rattachement(
                etapeId, origineId, origineSupportId, typeContactId, actionCommercialeId));
    }

    @PostMapping("/{id}/creer-prospect")
    @Operation(
            summary = "Crée la fiche prospect d'une pré-prospection et la lui rattache",
            description = "Première étape de qualification d'un lead brut : crée un `CfgTier` à "
                    + "partir des informations fournies et le pose comme prospect de la "
                    + "pré-prospection. Valide uniquement sur une pré-prospection (aucun prospect "
                    + "déjà lié) — 400 sinon. L'étape suivante est "
                    + "`POST /api/prospections/{id}/transformer`, qui assigne le commercial et "
                    + "achève la transformation en prospection.\n\n"
                    + "**Périmètre commercial** : une pré-prospection hors du périmètre de "
                    + "l'appelant renvoie 404, comme partout ailleurs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospect créé et rattaché."),
            @ApiResponse(responseCode = "400", description = "Cette prospection a déjà un prospect lié."),
            @ApiResponse(responseCode = "404", description = "Aucune pré-prospection avec cet id, "
                    + "ou pré-prospection hors du périmètre commercial de l'appelant — les deux cas "
                    + "sont volontairement indiscernables. Également renvoyé (plutôt qu'un 400) "
                    + "lorsque l'identifiant fourni n'est pas un UUID valide.")
    })
    public ResponseEntity<ProspectionDetailDto> creerProspect(
            @Parameter(description = "Identifiant de la pré-prospection (ComProspection.PKey).")
            @PathVariable UUID id,
            @RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectionService.creerProspect(id, request));
    }
}
