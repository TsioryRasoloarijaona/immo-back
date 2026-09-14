package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.domain.ChampDateEncaissement;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.services.encaissement.EncaissementService;
import com.tsix.immocrm.web.Groupements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Encaissements", description = "Règlements (StlSettlement) rattachés à un Dossier de "
        + "vente — reproduit l'écran legacy \"Encaissements Vente\" (module Vente d'OMRAN Estate). "
        + "Pour l'historique des règlements d'une seule échéance, voir plutôt "
        + "`GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements`, endpoint plus léger et "
        + "déjà scopé à un dossier — celui-ci est la vue transverse \"tous les encaissements\" "
        + "avec les mêmes filtres que le tableau legacy (entité, type, état, période, compte, "
        + "dépôt...).\n\n"
        + "Collection soumise au périmètre commercial : sur **toutes** les routes portant un "
        + "identifiant, un encaissement hors du périmètre de l'appelant renvoie 404 (jamais 403 — "
        + "un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide "
        + "renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le "
        + "contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.\n\n"
        + "**Entités juridiques en exploitation.** Ne sont renvoyés que les règlements portés par "
        + "une entité juridique active (`StlSettlement.CfgCompanyId` → `CfgCompany.IsActive`) — "
        + "aujourd'hui ADMOD et GHRIZA. Restriction **globale et permanente**, distincte du "
        + "périmètre commercial : elle s'applique à tout appelant, rôles exemptés compris. Un "
        + "règlement masqué renvoie 404 sur toutes les routes portant son identifiant.")
@RestController
@RequestMapping("/api/encaissements")
public class EncaissementController {

    private final EncaissementService encaissementService;

    public EncaissementController(EncaissementService encaissementService) {
        this.encaissementService = encaissementService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des encaissements (règlements de Dossiers de vente)",
            description = "Toujours restreint aux règlements liés à au moins une échéance de "
                    + "Dossier de vente (les règlements d'achat/location, s'il y en a, ne "
                    + "remontent jamais ici), et aux **encaissements** : un décaissement "
                    + "(`StlSettlement.IsCredit` faux, typiquement un remboursement à montant "
                    + "négatif) ne remonte jamais, comme sur l'écran legacy. "
                    + "`montantBrutRestant` (calculé : montant brut moins "
                    + "la part déjà affectée à une échéance) et `montantNet` (montant brut moins "
                    + "retenue source/timbre) sont calculés côté serveur, jamais stockés. "
                    + "`etat` accepte les 3 valeurs confirmées sur données réelles (IN_WORK, "
                    + "POINTED, CANCELED) — l'écran legacy propose aussi \"Impayé(e)\"/\"Perdu(e)\" "
                    + "comme filtres mais aucune ligne réelle ne les porte à ce jour ; passés tel "
                    + "quel (comparaison insensible à la casse), donc utilisables dès qu'un vrai "
                    + "code sera confirmé, sans changement d'API.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), cette liste ne "
                    + "contient que les règlements portant au moins une ligne rattachée à un "
                    + "dossier d'un de ses projets affectés. Les règlements dont aucune ligne ne "
                    + "résout de projet restent visibles de tous.\n\n"
                    + "**Encaissements non affectés : volontairement absents.** Un règlement "
                    + "encaissé mais pas encore imputé à une échéance n'a aucune ligne, donc ne "
                    + "remonte pas ici. L'écran legacy les affiche (il les reconnaît sous "
                    + "`StlSettlementRelationView.Origin = 'StlSettlement-NonAffecte'`) : 16 "
                    + "pièces pour 6 435 400 DH sur les données réelles, toutes à l'état IN_WORK. "
                    + "Les servir suppose de changer le critère \"Vente\" — décision produit en "
                    + "attente, voir `instructions/FEATURE-encaissements-conformite-legataire.md` "
                    + "§4. Ne pas confondre avec un filtre trop strict : c'est un choix assumé."
    )
    public PageDto<EncaissementDto> findAll(
            @Parameter(description = "Id(s) d'entité juridique (CfgCompany). Répétable, OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> entiteId,
            @Parameter(description = "Id(s) de type de règlement (StlSettlementType, voir GET /api/referentiels/types-reglement). Répétable, OR.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "État(s) (StlSettlement.Status : IN_WORK/POINTED/CANCELED confirmés). Répétable, OR, insensible à la casse.", example = "IN_WORK")
            @RequestParam(required = false) List<String> etat,
            @Parameter(description = "Id du compte bancaire de destination (StlAccount, voir GET /api/referentiels/comptes).")
            @RequestParam(required = false) UUID compteId,
            @Parameter(description = "Id de la catégorie de dépôt (StlDeposit, voir GET /api/referentiels/depots).")
            @RequestParam(required = false) UUID depotId,
            @Parameter(description = "Restreint aux règlements d'un seul Dossier (ComFolder.id).")
            @RequestParam(required = false) UUID dossierId,
            @Parameter(description = "Id(s) de tranche (CfgTranche.id, voir GET /api/patrimoine/tranches). "
                    + "Répétable, OR. Un règlement remonte dès qu'une de ses échéances porte sur un "
                    + "lot de l'une des tranches citées. Absent = toutes.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id du tiers réglant (CfgTier.id).")
            @RequestParam(required = false) UUID tierId,
            @Parameter(description = "Sous-chaîne sur la référence (StlSettlement.Code, ex. n° de chèque).")
            @RequestParam(required = false) String reference,
            @Parameter(description = "Recherche libre : référence OU nom du tiers réglant.")
            @RequestParam(required = false) String search,
            @Parameter(description = "true = uniquement comptabilisés, false = uniquement non comptabilisés, absent = tous.")
            @RequestParam(required = false) Boolean comptabilise,
            @Parameter(description = "Montant brut minimum (inclus).")
            @RequestParam(required = false) BigDecimal montantDe,
            @Parameter(description = "Montant brut maximum (inclus).")
            @RequestParam(required = false) BigDecimal montantA,
            @Parameter(description = "Jalon sur lequel appliquer dateDebut/dateFin. Ignoré si "
                    + "aucun des deux n'est fourni. Défaut : DATE_PIECE.", example = "DATE_ECHEANCE")
            @RequestParam(required = false) ChampDateEncaissement champDate,
            @Parameter(description = "Borne inférieure (incluse) de la plage sur champDate, format ISO-8601.", example = "2026-08-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) de la plage sur champDate, format ISO-8601.", example = "2026-08-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @Parameter(description = "Colonnes à sérialiser, répétable ou séparées par des "
                    + "virgules. Absent : les 27 colonnes historiques. Catalogue et liste blanche : "
                    + "`GET /api/referentiels/colonnes/encaissements`. Un nom hors catalogue rend "
                    + "400 avec la liste des noms valides. `id` est toujours servi.",
                    example = "code,description,montantBrut,typeLibelle,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(description = "Déplie un groupe : restreint la liste aux encaissements d'un "
                    + "groupe renvoyé par `GET /api/encaissements/groupes`. Attend le nom de la "
                    + "colonne qui a servi au groupement.", example = "typeLibelle")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(description = "Valeur du groupe à déplier — **la `cle` renvoyée telle quelle "
                    + "par `/groupes`**, sans transformation. Omise alors que `groupeColonne` est "
                    + "fourni : le groupe des encaissements sans valeur sur cette colonne.",
                    example = "CHEQUE")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité appliquée quand `groupeColonne` est une date. "
                    + "Doit être identique à celle passée à `/groupes`, sinon la clé ne retombe pas "
                    + "sur le même groupe. Défaut : MOIS.", example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneEncaissement groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneEncaissement::resoudreGroupement);
        return PageDto.of(encaissementService.findAll(entiteId, typeId, etat, compteId, depotId, dossierId, trancheId,
                tierId, reference, search, comptabilise, montantDe, montantA, champDate, dateDebut, dateFin,
                groupe, groupeValeur, Groupements.granularite(groupe, granularite), pageable,
                ColonneEncaissement.resoudre(colonnes)));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des encaissements repliée en groupes, avec effectifs et totaux",
            description = """
                    Reproduit la bande de groupement du logiciel de bureau : au lieu des lignes, \
                    renvoie **un groupe par valeur distincte** de la colonne `groupBy`, chacun \
                    portant son effectif et les sommes demandées.

                    **Pourquoi cet endpoint plutôt qu'un groupement côté front** : la liste est \
                    paginée. Grouper une page ne donnerait que des groupes tronqués et des totaux \
                    faux. Les effectifs et totaux ci-dessous portent sur **tout le périmètre \
                    filtré**, pas sur une page.

                    **Les filtres sont exactement ceux de `GET /api/encaissements`** — mêmes noms, \
                    même sémantique, restriction `IsCredit = 1` et périmètre commercial compris. Un \
                    même jeu de filtres des deux côtés garantit que la somme des effectifs des \
                    groupes est égale au `totalElements` de la liste.

                    **Les groupes sont repliés** : pour obtenir les lignes de l'un d'eux, rappeler \
                    `GET /api/encaissements` avec `?groupeColonne=<groupBy>&groupeValeur=<la cle du \
                    groupe>`.

                    Colonnes groupables et agrégeables : \
                    `GET /api/referentiels/colonnes/encaissements` (booléens `groupable` / \
                    `agregeable`). **`dossierId` et `dossierCode` n'en sont pas**, et ce n'est pas \
                    un oubli : un règlement n'a pas de dossier, il a des lignes rattachées à des \
                    échéances. Mesuré sur les données réelles — 643 règlements, 654 couples \
                    (règlement, dossier), 8 règlements portant sur plusieurs dossiers — grouper \
                    par ce chemin compterait ces 8 plusieurs fois et la somme des effectifs ne \
                    retomberait plus sur le total de la liste. Même chose pour `montantBrutRestant` \
                    et `montantNet`, calculés en Java et absents de la base.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", description =
                            "`groupBy` absent, inconnu, ou désignant une colonne non groupable ; "
                            + "ou `agregats` contenant une colonne non agrégeable. Le corps "
                            + "(RFC 7807) porte la liste des noms acceptés.", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau de "
                    + "groupement est supporté.", example = "typeLibelle", required = true)
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
                    example = "montantBrutRestant,montantNet,montantBrut:AVG")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité de regroupement d'une colonne date. Ignorée si "
                    + "`groupBy` n'est pas une date. Défaut : MOIS — grouper sur la date exacte "
                    + "rendrait 390 groupes pour 643 règlements, soit une liste plate.",
                    example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "Ordre des groupes : LIBELLE (croissant, défaut) ou NOMBRE "
                    + "(effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(description = "Inclure le groupe des lignes dont la colonne groupée est "
                    + "vide. **Faux par défaut** : l'écran veut les valeurs qui existent, et sur "
                    + "une colonne peu renseignée — `depotCode` est à 0 sur 643, `compteCode` à 12 "
                    + "— le groupe vide écraserait tous les autres. Passer `true` pour le "
                    + "récupérer : la somme des effectifs retombe alors sur le `totalElements` de "
                    + "la liste non groupée.", example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<UUID> entiteId,
            @RequestParam(required = false) List<UUID> typeId,
            @RequestParam(required = false) List<String> etat,
            @RequestParam(required = false) UUID compteId,
            @RequestParam(required = false) UUID depotId,
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) UUID tierId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean comptabilise,
            @RequestParam(required = false) BigDecimal montantDe,
            @RequestParam(required = false) BigDecimal montantA,
            @RequestParam(required = false) ChampDateEncaissement champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneEncaissement groupe = ColonneEncaissement.resoudreGroupement(groupBy);
        return encaissementService.grouper(entiteId, typeId, etat, compteId, depotId,
                dossierId, trancheId, tierId, reference, search, comptabilise, montantDe, montantA,
                champDate, dateDebut, dateFin, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneEncaissement.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'un encaissement (règlement)",
            description = "Même forme que les lignes de `GET /api/encaissements` — alimente le "
                    + "panneau détail \"Pièce de règlement\" côté front (`EncaissementDetail.tsx`). "
                    + "`montantBrutRestant` et `montantNet` sont recalculés côté serveur, jamais "
                    + "stockés, comme sur la liste.\n\n"
                    + "**Périmètre commercial** : un encaissement hors du périmètre de l'appelant "
                    + "renvoie **404**, et non 403 — un 403 confirmerait son existence et "
                    + "permettrait d'énumérer les identifiants."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encaissement trouvé."),
            @ApiResponse(responseCode = "404", description = "Aucun StlSettlement pour cet id, ou "
                    + "encaissement hors du périmètre commercial de l'appelant — les deux cas sont "
                    + "volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque "
                    + "l'identifiant fourni n'est pas un UUID valide.")
    })
    public EncaissementDto findById(
            @Parameter(description = "Id du règlement (StlSettlement.id).")
            @PathVariable UUID id) {
        return encaissementService.findById(id);
    }
}
