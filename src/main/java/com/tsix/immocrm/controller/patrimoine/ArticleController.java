package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.services.patrimoine.ArticlePieceJointeService;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Articles", description = "Articles et sous-articles (StkItem) — lots vendables d'une "
        + "tranche, avec leur disponibilité et leurs éventuels sous-lots (cave/parking annexés).")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final ArticlePieceJointeService pieceJointeService;

    public ArticleController(ArticleService articleService, ArticlePieceJointeService pieceJointeService) {
        this.articleService = articleService;
        this.pieceJointeService = pieceJointeService;
    }

    @GetMapping
    @Operation(
            summary = "Catalogue transverse des articles",
            description = "Liste paginée de tous les articles racine et sous-articles, toutes "
                    + "tranches confondues — pour parcourir/rechercher le patrimoine sans devoir "
                    + "d'abord choisir une Tranche (contrairement à `GET /api/tranches/{id}/articles`, "
                    + "point d'entrée à privilégier quand la tranche est déjà connue). `search` "
                    + "filtre en sous-chaîne sur `code` OU `description` (insensible à la casse). "
                    + "`trancheId` restreint à une ou plusieurs tranches (OR) sans changer d'écran. "
                    + "`statut` filtre sur la disponibilité mais, à la différence des autres "
                    + "endpoints Article, **n'a pas de valeur par défaut ici** : absent = tous les "
                    + "statuts (DISPONIBLE + OCCUPE), pour une vraie vue catalogue. Restreint aux "
                    + "entités juridiques actives (ADMOD/GHRIZA), comme les autres listings "
                    + "Projet/Tranche/Article. Filtres de parité avec la grille legacy \"Articles\" "
                    + "(instructions/from_front.md #4) : `entiteId` (CfgCompany, voir "
                    + "`GET /api/referentiels/entites`), `natureId` (StkKind, voir "
                    + "`GET /api/referentiels/natures`), `vocationId` (StkVocation, voir "
                    + "`GET /api/referentiels/vocations`), `etageId` (StkFloor, voir "
                    + "`GET /api/referentiels/etages`), et les plages `prixDe`/`prixA` "
                    + "(sur `comSalePrice`) et `surfaceDe`/`surfaceA` (sur `area`), bornes "
                    + "inclusives et indépendamment optionnelles."
    )
    public PageDto<ArticleListItemDto> findAll(
            @Parameter(description = "Sous-chaîne recherchée dans le code ou la description de l'article.", example = "A-204")
            @RequestParam(required = false) String search,
            @Parameter(description = "Id(s) de CfgTranche. Répétable, correspondance OR. Absent = toutes les tranches (des entités actives).")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE et OCCUPE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Id de CfgCompany (entité juridique). Absent = toutes (parmi les entités actives).")
            @RequestParam(required = false) UUID entiteId,
            @Parameter(description = "Id de StkKind (nature du bien). Absent = toutes.")
            @RequestParam(required = false) UUID natureId,
            @Parameter(description = "Id de StkVocation. Absent = toutes.")
            @RequestParam(required = false) UUID vocationId,
            @Parameter(description = "Id de StkFloor (étage). Absent = tous.")
            @RequestParam(required = false) UUID etageId,
            @Parameter(description = "Borne basse (incluse) sur le prix de vente (StkItem.ComSalePrice).")
            @RequestParam(required = false) BigDecimal prixDe,
            @Parameter(description = "Borne haute (incluse) sur le prix de vente (StkItem.ComSalePrice).")
            @RequestParam(required = false) BigDecimal prixA,
            @Parameter(description = "Borne basse (incluse) sur la superficie (StkItem.Area).")
            @RequestParam(required = false) BigDecimal surfaceDe,
            @Parameter(description = "Borne haute (incluse) sur la superficie (StkItem.Area).")
            @RequestParam(required = false) BigDecimal surfaceA,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.findAll(search, trancheId, statut, categorieId, typeId,
                entiteId, natureId, vocationId, etageId, prixDe, prixA, surfaceDe, surfaceA, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'un article ou sous-article",
            description = "Fonctionne indifféremment pour un article racine ou un sous-article "
                    + "(même table StkItem) — la \"recherche inverse\" (partir d'un sous-article "
                    + "pour retrouver sa tranche et son article parent) passe par ce même endpoint : "
                    + "`trancheId`/`trancheCode`/`trancheDescription` et `lotParentId`/`lotParentCode`/"
                    + "`lotParentDescription` sont toujours renseignés quand applicable, sans appel "
                    + "supplémentaire. `sousLots` liste tous les sous-articles de cet article, quel "
                    + "que soit leur statut. `immeubleId`/`immeubleCode`/`immeubleLibelle` (StkBuilding), "
                    + "`etageId`/`etageCode`/`etageLibelle` (StkFloor) et `natureId`/`natureCode`/"
                    + "`natureLibelle` (StkKind) sont chacun des référentiels distincts de "
                    + "Catégorie/Type — tous null si l'article n'est rattaché à aucune valeur "
                    + "(ex. un terrain n'a ni immeuble ni étage). `salePriceFloor`/`comSalePriceFloor` "
                    + "(prix plancher) sont distincts de `salePrice`/`comSalePrice` (prix commercial) "
                    + "— ce dernier peut valoir 0 sur un article dont le prix commercial n'a jamais "
                    + "été saisi alors qu'un prix plancher existe déjà (confirmé sur données réelles) ; "
                    + "au front de décider s'il affiche le plancher en repli dans ce cas. `tauxTVA` "
                    + "(StkItem.Vat, ratio brut) et `presentationTypeId`/`presentationTypeCode`/"
                    + "`presentationTypeLibelle` (StkPresentationType, \"Type Présentation\" de la "
                    + "grille legacy — codes courts F/C/D/H..., scopés par tranche) répondent à "
                    + "instructions/from_front.md #4. `superficieSousArticles` est la somme des "
                    + "superficies des sous-articles rattachés (0 si aucun), distincte de `area` "
                    + "(superficie de l'article lui-même)."
    )
    public ArticleDetailDto findById(@PathVariable UUID id) {
        return articleService.findById(id);
    }

    @GetMapping("/{id}/sous-articles")
    @Operation(
            summary = "Sous-articles disponibles (ou occupés) d'un article",
            description = "Liste paginée des sous-articles (StkItem.ParentId) d'un article donné "
                    + "— ex. les places de parking ou caves annexées à un lot. `statut` filtre sur "
                    + "la disponibilité (défaut: DISPONIBLE) ; `categorieId`/`typeId` filtrent "
                    + "comme sur `GET /api/tranches/{id}/articles`. Chaque sous-article renvoyé "
                    + "porte son propre `description` (son nom), pas seulement son `code`."
    )
    public PageDto<ArticleListItemDto> findSousArticles(
            @PathVariable UUID id,
            @Parameter(description = "Filtre sur la disponibilité du sous-article. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(articleService.findSousArticles(id, statut, categorieId, typeId, pageable));
    }

    // -- Pièces jointes (AdmAttachedFile, CategoryId = StkItem.PKey — voir ArticlePieceJointeService) --

    @GetMapping("/{id}/pieces-jointes")
    @Operation(
            summary = "Liste les pièces jointes de l'article",
            description = "Retourne toutes les pièces jointes rattachées à l'article `id` "
                    + "(`AdmAttachedFile.CategoryId = StkItem.PKey`, même convention polymorphe "
                    + "sans FK déclarée que pour les pièces jointes de Dossier). Ce sont "
                    + "essentiellement des plans (types `PLAN`/`PLAN_ARCHITECTE`/`PLAN_ETAGE`), "
                    + "mais aucun filtrage sur `type`/`Category` n'est appliqué ici — tout ce qui "
                    + "est rattaché à l'article ressort. Triées par date de dépôt décroissante. "
                    + "Les lignes \"cochées mais jamais déposées\" (checklist sans fichier réel) "
                    + "sont exclues. Liste vide si aucune pièce jointe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des pièces jointes (peut être vide)."),
            @ApiResponse(responseCode = "404", description = "Article introuvable.")
    })
    public List<PieceJointeDto> getPiecesJointes(
            @Parameter(description = "Identifiant de l'article (StkItem.PKey).")
            @PathVariable UUID id) {
        return pieceJointeService.findByArticle(id);
    }

    @GetMapping("/{id}/pieces-jointes/{pieceId}/fichier")
    @Operation(
            summary = "Télécharge/affiche une pièce jointe de l'article (ex. un plan)",
            description = "**Renvoie une redirection `302` vers une URL présignée**, pas le contenu "
                    + "binaire : le fichier est stocké sur S3/MinIO et transite directement entre le "
                    + "client et le stockage, sans passer par le backend.\n\n"
                    + "L'URL signée est valable quelques minutes et vaut autorisation d'accès ; elle "
                    + "n'est délivrée qu'après vérification que la pièce appartient bien à cet article."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirection vers l'URL présignée du fichier."),
            @ApiResponse(responseCode = "404", description = "Article introuvable, pièce jointe introuvable "
                    + "pour cet article, ou aucun fichier stocké pour cette pièce.")
    })
    public ResponseEntity<Void> getFichier(
            @Parameter(description = "Identifiant de l'article (StkItem.PKey).")
            @PathVariable UUID id,
            @Parameter(description = "Identifiant de la pièce jointe (AdmAttachedFile.PKey).")
            @PathVariable UUID pieceId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(pieceJointeService.urlFichier(id, pieceId))
                .build();
    }
}
