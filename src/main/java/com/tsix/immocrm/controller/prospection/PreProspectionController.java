package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

@Tag(name = "Pré-prospections", description = "Leads bruts non encore qualifiés (ComProspection "
        + "sans CfgProspectId ni CfgCommercialId) — même forme que /api/prospections.\n\n"
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

    public PreProspectionController(ProspectionService prospectionService) {
        this.prospectionService = prospectionService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des pré-prospections",
            description = "Mêmes filtres que GET /api/prospections (étape, origine, commercial, "
                    + "tranche(s), plage de dates, recherche libre, ouvertes) appliqués aux leads "
                    + "non encore qualifiés. Voir instructions/filtres-avances.md point 5.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint, cette liste ne "
                    + "contient que les pré-prospections de ses projets affectés. À noter — une "
                    + "pré-prospection n'a par construction **aucun commercial** (c'est justement "
                    + "ce qui la définit) : seul le projet de sa tranche visée peut la scoper, le "
                    + "filtre `commercial` ne renverra jamais rien ici. Les pré-prospections sans "
                    + "tranche restent visibles de tous."
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
                    pré-prospections (`CfgProspectId IS NULL AND CfgCommercialId IS NULL`).

                    **Les effectifs portent sur tout le périmètre filtré, pas sur une page.** Pour \
                    ouvrir un groupe : `GET /api/pre-prospections?groupeColonne=<groupBy>\
                    &groupeValeur=<cle>`.

                    Deux colonnes n'ont ici aucun intérêt et rendront un groupe unique à valeur \
                    nulle : `prospectNom` et `commercialNom`, nuls par définition sur ce \
                    périmètre. Elles restent acceptées plutôt que refusées par un cas particulier \
                    — le résultat est exact, simplement sans information.""",
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
