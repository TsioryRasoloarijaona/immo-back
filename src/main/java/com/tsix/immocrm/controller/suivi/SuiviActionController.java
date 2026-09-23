package com.tsix.immocrm.controller.suivi;

import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import com.tsix.immocrm.services.suivi.SuiviActionService;
import com.tsix.immocrm.services.suivi.SuiviActionService.Filtres;
import com.tsix.immocrm.web.Groupements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Suivi des actions", description = """
        L'écran OMRAN « Suivi des actions » : les actions commerciales (appels, visites, messages, \
        notes…) saisies par chaque utilisateur, sur une période. Doc OMRAN : \
        https://help.omranestate.com/SUIVIDESACTIONS.html — détail dans \
        `instructions/FEATURE-suivi-actions.md`.

        **Même requête qu'OMRAN**, relevée dans sa trace d'audit : \
        `ComActionMessageView WHERE CfgTrancheId IN (…) AND DocDate >= début AND DocDate < fin \
        [AND UserName = …]`. Mêmes colonnes, même tranche résolue (catégorie Prospection → \
        tranche de la prospection, sinon de l'article), même rang, même état — construits sur la \
        table plutôt que sur la vue, qui coûte 6,6 s la page.

        **Compter les appels d'un commercial jour par jour** — le cas d'usage principal (94 % des \
        actions de l'année sont des appels sortants) : \
        `GET /api/suivi-actions/groupes?utilisateur=<nom>&typeId=<Téléphone Sortant>\
        &groupBy=dateAction&granularite=JOUR&dateDebut=…&dateFin=…`. L'effectif de chaque groupe \
        est le nombre d'actions ; la somme des effectifs égale le `totalElements` de la liste aux \
        mêmes filtres.

        **Qui voit quoi.** Le suivi **général** est réservé au rôle `Administrateur`. Tout autre \
        utilisateur ne voit que **ses propres actions** — sur la liste comme dans les groupes : \
        celles écrites à son nom (`UserName`), plus celles de son compte hors lignes « Système » \
        (actions générées pendant sa session, qu'OMRAN attribue à « Système »). Pour lui, le \
        filtre `utilisateur` ne peut que restreindre : demander quelqu'un d'autre rend une page \
        vide, jamais un 403 qui confirmerait l'existence de ses actions. Le front masque donc le \
        sélecteur d'utilisateur hors administrateur.

        **Périmètre commercial** : appliqué par le service, liste et groupes — une action dont le \
        projet (par la prospection, puis par le dossier) est hors périmètre n'apparaît ni dans la \
        liste ni dans un effectif. Actions d'entités juridiques inactives exclues, comme partout.""")
@RestController
@RequestMapping("/api/suivi-actions")
public class SuiviActionController {

    private final SuiviActionService service;

    public SuiviActionController(SuiviActionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Liste des actions commerciales, colonnes au choix",
            description = """
                    La grille de l'écran OMRAN. Tous les filtres sont optionnels et se combinent en ET.

                    **Période fin exclue**, comme OMRAN : `dateDebut <= DocDate < dateFin`. Un jour J \
                    = `dateDebut=J T00:00` et `dateFin=J+1 T00:00`.

                    **Colonnes** : `?colonnes=` choisit les champs servis (catalogue et liste \
                    blanche : `GET /api/referentiels/colonnes/suivi-actions`). `rang`, \
                    `alerteActive`, `etat` et `personnel` sont calculées pour la page servie, et \
                    seulement quand elles sont demandées : ni triables ni groupables.

                    **Tri** : `sort=docDate,desc` par défaut ; seules les propriétés de l'action \
                    (`docDate`, `userName`, `alertValidityDate`…) sont triables.

                    Les actions **système** (relances, événements générés) sont listées, comme dans \
                    OMRAN — `systeme=false` pour ne garder que les saisies.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page d'actions"),
                    @ApiResponse(responseCode = "400", description = "Nom de colonne hors catalogue "
                            + "(le corps liste les noms valides) ; `groupeColonne` non groupable.",
                            content = @Content)
            })
    public PageDto<SuiviActionDto> findAll(
            @Parameter(description = "Début de période, **inclus** (`DocDate >= dateDebut`), ISO-8601.",
                    example = "2026-09-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Fin de période, **exclue** (`DocDate < dateFin`), ISO-8601 — comme "
                    + "la requête OMRAN.", example = "2026-10-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @Parameter(description = "Tranche(s) résolue(s) à la manière d'OMRAN (catégorie Prospection → "
                    + "tranche de la prospection, sinon de l'article). Répétable, OU.")
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Utilisateur(s) — `UserName` de l'action, **égalité exacte**, la "
                    + "colonne que filtre OMRAN. Valeurs présentes : "
                    + "`/api/suivi-actions/groupes?groupBy=utilisateur`. Répétable, OU. **Utile à "
                    + "l'administrateur seulement** : pour un autre rôle, la liste est déjà réduite à "
                    + "ses propres actions et ce filtre ne peut que la restreindre davantage.",
                    example = "Khadija ELKORRI")
            @RequestParam(required = false) List<String> utilisateur,
            @Parameter(description = "Type(s) d'action (ComActionMessageType.id). Répétable, OU.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Catégorie(s) (ComActionMessageCategory.id). Répétable, OU.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "`true` = actions système seules, `false` = saisies seules, absent "
                    + "= tout (comme OMRAN).")
            @RequestParam(required = false) Boolean systeme,
            @Parameter(description = "Colonnes à servir, répétable ou séparées par des virgules. Absent : "
                    + "la grille OMRAN. `id` est toujours servi.",
                    example = "utilisateur,typeAction,dateAction,tiersNom,prospectionCode,trancheCode")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(description = "Déplie un groupe renvoyé par `/groupes` : le nom de la colonne "
                    + "groupée.", example = "dateAction")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(description = "La `cle` du groupe, telle que renvoyée par `/groupes`. Omise avec "
                    + "`groupeColonne` : le groupe sans valeur.")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(description = "Granularité si `groupeColonne` est une date — la même qu'à "
                    + "`/groupes`. Défaut : MOIS.", example = "JOUR")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneSuiviAction groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneSuiviAction::resoudreGroupement);
        return PageDto.of(service.findAll(
                new Filtres(dateDebut, dateFin, trancheId, utilisateur, typeId, categorieId, systeme),
                groupe, groupeValeur, Groupements.granularite(groupe, granularite),
                ColonneSuiviAction.resoudre(colonnes), pageable));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Actions repliées en groupes, avec effectifs — les comptes par jour, par utilisateur…",
            description = """
                    La bande de groupement de la grille OMRAN, calculée côté serveur : **un groupe \
                    par valeur** de `groupBy`, avec son effectif = **le nombre d'actions**. Les \
                    effectifs portent sur tout le périmètre filtré, pas sur une page.

                    **Mêmes filtres que la liste**, mêmes noms, même périmètre : la somme des \
                    effectifs égale le `totalElements` de `GET /api/suivi-actions` aux mêmes \
                    filtres (avec `inclureSansValeur=true`).

                    Recettes :
                    - **appels passés par jour** d'un commercial : \
                    `utilisateur=<nom>&typeId=<Téléphone Sortant>&groupBy=dateAction&granularite=JOUR` ;
                    - **activité par utilisateur** sur une période : `groupBy=utilisateur` ;
                    - **par type d'action** : `groupBy=typeAction` ;
                    - **par tranche** : `groupBy=trancheCode`.

                    Pour ouvrir un groupe : `GET /api/suivi-actions?groupeColonne=<groupBy>\
                    &groupeValeur=<cle>` (+ la même `granularite`).

                    **Aucune colonne n'est agrégeable** : la mesure est l'effectif. `agregats` rend \
                    donc 400. Colonnes groupables : le catalogue (`groupable = true`).""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", description = "`groupBy` absent, inconnu ou non "
                            + "groupable (`rang`, `alerteActive`, `etat`, `personnel`, `message`) ; "
                            + "`agregats` fourni.", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau.",
                    example = "dateAction", required = true)
            @RequestParam String groupBy,
            @Parameter(description = "Aucune colonne de ce catalogue n'est agrégeable : laisser vide.")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(description = "Granularité si `groupBy` est une date. **JOUR** pour les comptes "
                    + "quotidiens. Défaut : MOIS.", example = "JOUR")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "LIBELLE (croissant, défaut) ou NOMBRE (effectif décroissant).")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(description = "Inclure le groupe des actions sans valeur sur la colonne groupée. "
                    + "Faux par défaut.", example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) List<String> utilisateur,
            @RequestParam(required = false) List<UUID> typeId,
            @RequestParam(required = false) List<UUID> categorieId,
            @RequestParam(required = false) Boolean systeme,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneSuiviAction groupe = ColonneSuiviAction.resoudreGroupement(groupBy);
        return service.grouper(
                new Filtres(dateDebut, dateFin, trancheId, utilisateur, typeId, categorieId, systeme),
                groupe, Groupements.granularite(groupe, granularite),
                ColonneSuiviAction.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }
}
