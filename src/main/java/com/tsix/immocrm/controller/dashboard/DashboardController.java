package com.tsix.immocrm.controller.dashboard;

import com.tsix.immocrm.domain.EcheanceRelance;
import com.tsix.immocrm.domain.PorteeRelance;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dashboard.DashboardDto;
import com.tsix.immocrm.dto.dashboard.NouveauPreProspectDto;
import com.tsix.immocrm.dto.dashboard.RelanceProspectionDto;
import com.tsix.immocrm.dto.dashboard.SyntheseAnnuelleDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dashboard.DashboardService;
import com.tsix.immocrm.services.dashboard.RelanceDashboardService;
import com.tsix.immocrm.services.dashboard.SyntheseAnnuelleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Tableau de bord", description = "Agrégats de la page d'accueil : valeur du pipeline, "
        + "panier moyen, répartition des dossiers par étape, relances en retard et compteurs de "
        + "prospections.\n\n"
        + "**Entités juridiques en exploitation.** Tous les agrégats ne portent que sur les "
        + "entités juridiques actives (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. "
        + "L'assiette du tableau de bord est donc la même que celle de GET /api/dossiers et de "
        + "GET /api/prospections ; les totaux ne sont pas comparables à ceux d'avant cette "
        + "restriction.\n\n"
        + "**Deux notions de « relance » cohabitent, et elles ne désignent pas la même chose.** "
        + "`GET /api/dashboard/relances` sert les relances **commerciales** : une échéance posée "
        + "par un commercial sur une interaction, encore armée — 3 765 en base réelle. Le champ "
        + "`relancesEnRetard` de `GET /api/dashboard`, lui, parle des *dossiers* via "
        + "`ComFolder.Revival1`/`Revival2`, colonnes **mortes en base réelle (0 ligne sur 358)** : "
        + "ce champ est structurellement vide. Voir "
        + "`instructions/FEATURE-dashboard-relances.md`.")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final RelanceDashboardService relanceDashboardService;
    private final SyntheseAnnuelleService syntheseAnnuelleService;

    public DashboardController(DashboardService dashboardService,
                               RelanceDashboardService relanceDashboardService,
                               SyntheseAnnuelleService syntheseAnnuelleService) {
        this.dashboardService = dashboardService;
        this.relanceDashboardService = relanceDashboardService;
        this.syntheseAnnuelleService = syntheseAnnuelleService;
    }

    @GetMapping
    @Operation(
            summary = "Agrégats du tableau de bord",
            description = "Tous les chiffres sont calculés côté serveur à la volée, jamais "
                    + "stockés. `valeurPipeline` et `nombre de dossiers actifs` (qui donne le "
                    + "`panierMoyen`) excluent les dossiers en statut de sortie (annulation, "
                    + "désistement...). `relancesEnRetard` liste les dossiers dont une date de "
                    + "validité de relance est dépassée — **en pratique toujours vide**, les "
                    + "colonnes `Revival1`/`Revival2` n'étant renseignées sur aucune des 358 "
                    + "lignes réelles ; pour les relances commerciales, voir "
                    + "`GET /api/dashboard/relances`. `prospectionsOuvertes`/"
                    + "`prospectionsTotal` excluent les pré-prospections.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), *tous* les agrégats "
                    + "ci-dessus ne portent que sur ses projets affectés — un chiffre d'affaires "
                    + "global serait précisément la donnée que le périmètre masque. Les dossiers "
                    + "et prospections dont le projet ne se résout pas restent comptés pour tout "
                    + "le monde."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agrégats calculés. Des listes vides "
                    + "et des montants à zéro signifient qu'aucun dossier n'entre dans le "
                    + "périmètre de l'appelant, pas une erreur.")
    })
    public DashboardDto get() {
        return dashboardService.get();
    }

    @GetMapping("/relances")
    @Operation(
            summary = "Les relances commerciales à traiter",
            description = """
                    Les échéances de relance posées par les commerciaux sur leurs interactions, \
                    encore armées. C'est la file de travail de la journée.

                    **Ce qui est exclu, et pourquoi c'est l'essentiel du filtre.** Une relance \
                    désarmée (`WithoutRevival`) n'apparaît jamais : c'est le drapeau posé quand la \
                    prospection est transformée en vente, et il couvre **86 % des 39 335 alertes \
                    de la base**. Sont également exclus les rappels de paiement rattachés à une \
                    échéance (`ComDocumentId` non nul), qui ne sont pas du travail commercial. Il \
                    reste 3 765 relances vivantes, une par prospection.

                    **`echeance=RETARD` : les relances en retard des prospections du mois.** Sont \
                    servies les relances échues (date d'alerte antérieure à aujourd'hui) des \
                    prospections **créées dans le mois civil en cours** (`dateDocument` du 1er du \
                    mois au dernier jour), **qualifiées** (les pré-prospections sont exclues) et \
                    **ouvertes**. C'est exactement la population de `GET /api/prospections` filtrée \
                    sur le mois : le compteur du tableau de bord et le nombre d'alertes en retard \
                    de la liste sont donc égaux. Aucune borne sur l'ancienneté de l'alerte — \
                    `joursDeRetard` peut dépasser 30 si une date d'alerte a été saisie dans le \
                    passé. L'arriéré porté par les prospections des mois précédents (plus de \
                    3 000 relances) n'est pas servi ici : passer par les filtres de \
                    `GET /api/prospections`. **Changement du 28/09/2026** : cette tranche servait \
                    auparavant une fenêtre glissante de 30 jours sur la date de l'alerte, \
                    pré-prospections et prospections anciennes comprises.

                    `echeance=AUJOURDHUI` est inchangé : toute relance armée dont l'échéance est \
                    aujourd'hui, pré-prospections comprises.

                    **Les relances non affectées sont servies quelle que soit la `portee`**, y \
                    compris `MOI` : 108 relances (2,9 %) ne désignent personne — ni par \
                    l'affectation de la prospection, ni par l'auteur de l'interaction — et les 21 \
                    posées sur des pré-prospections en font toutes partie, une pré-prospection \
                    étant *définie* par l'absence de commercial. Les réserver à un périmètre les \
                    rendrait invisibles à tout le monde. Elles portent `nonAffectee: true`.

                    Trié par échéance croissante : le plus ancien retard d'abord.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de relances. **Une page vide "
                    + "n'est pas une erreur** : un jour ouvré normal en compte 1 à 11, et un "
                    + "compte non rattaché à un employé ne possède aucune relance — il ne reçoit "
                    + "alors, sous `portee=MOI`, que les relances non affectées."),
            @ApiResponse(responseCode = "400", description = "`echeance` ou `portee` hors des "
                    + "valeurs admises.")
    })
    public PageDto<RelanceProspectionDto> relances(
            @Parameter(description = "`AUJOURDHUI` : échéance au jour même. `RETARD` : relances "
                    + "échues des prospections qualifiées et ouvertes créées dans le mois civil "
                    + "en cours.", example = "AUJOURDHUI")
            @RequestParam(defaultValue = "AUJOURDHUI") EcheanceRelance echeance,

            @Parameter(description = "`MOI` : les relances dont le commercial résolu est "
                    + "l'appelant. `EQUIPE` : tout son périmètre visible. Dans les deux cas les "
                    + "relances non affectées sont incluses.", example = "MOI")
            @RequestParam(defaultValue = "MOI") PorteeRelance portee,

            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(relanceDashboardService.relances(echeance, portee, principal, pageable));
    }

    @GetMapping("/nouveaux-pre-prospects")
    @Operation(
            summary = "Les leads arrivés dans la journée",
            description = """
                    Les pré-prospections créées le jour demandé — un lead non encore qualifié, \
                    de type légataire `ComPreProspection`, même discriminant que \
                    `GET /api/pre-prospections`.

                    **Les arrivées se font par rafales, pas en continu**, l'injection des leads \
                    étant automatique : sur les 21 derniers jours mesurés, la plupart sont à zéro \
                    puis viennent des pointes à 11, 20, 31 et 34. **Le week-end n'est pas creux** \
                    ici, contrairement aux interactions — 34 leads sont arrivés un dimanche. Une \
                    journée vide est donc le cas courant et se lit « aucun lead », jamais « panne ».

                    Pas de paramètre `portee` : une pré-prospection n'ayant pas de commercial, \
                    « les miennes » n'aurait aucun sens. Seul le périmètre par projet s'applique.

                    Trié du plus récent au plus ancien.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de leads du jour. Vide le plus "
                    + "souvent — ce n'est pas une erreur."),
            @ApiResponse(responseCode = "400", description = "`jour` non parsable.")
    })
    public PageDto<NouveauPreProspectDto> nouveauxPreProspects(
            @Parameter(description = "Jour observé. Défaut : aujourd'hui. Sert surtout à rattraper "
                    + "la veille après un week-end chargé.", example = "2026-09-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate jour,

            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(relanceDashboardService.nouveauxPreProspects(jour, pageable));
    }

    @GetMapping("/annuel")
    @Operation(
            summary = "Synthese annuelle facon logiciel de bureau",
            description = """
                    Reprend l'ecran d'accueil d'OMRAN : l'echeancier mois par mois, les ventes                     concretisees et les encaissements de l'annee. L'assiette de chaque bloc est                     celle du logiciel de bureau, pour que les chiffres restent comparables poste                     a poste pendant la transition.

                    **`echeancier` n'est pas un realise.** Il somme les montants *dus*                     (`ComDocument.Amount`), pas ce qui est entre en caisse. Des mois a venir y                     sont donc renseignes : en septembre 2026, octobre et novembre portaient deja                     380 000 et 50 000 DH. Une courbe qui s'arreterait au mois courant masquerait                     de la donnee vraie. Pour l'argent reellement encaisse, c'est le bloc                     `encaissements`.

                    **`ventesConcretisees` ne couvre pas tous les dossiers.** L'assiette est la                     date d'avance (`ComFolder.AdvanceDate`), et **232 des 387 dossiers reels n'en                     ont pas** : ils sont structurellement absents, quelle que soit l'annee. Le                     champ `dossiersSansDateAvance` donne ce nombre pour que l'ecart soit                     affichable plutot que subi.

                    **Les 12 mois sont toujours servis**, un mois vide valant zero : le client                     n'a jamais a deviner quels points manquent.

                    **Perimetre commercial** : les trois blocs y sont soumis, comme le logiciel                     de bureau qui pose le meme filtre d'habilitation sur chacune de ses requetes.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Synthese calculee. Des compteurs a "
                    + "zero signifient qu'aucun dossier n'entre dans le perimetre de l'appelant "
                    + "sur cette annee, pas une erreur."),
            @ApiResponse(responseCode = "400", description = "`annee` non numerique.")
    })
    public SyntheseAnnuelleDto annuel(
            @Parameter(description = "Annee observee. Defaut : l'annee courante.", example = "2026")
            @RequestParam(required = false) Integer annee) {
        return syntheseAnnuelleService.get(annee);
    }
}
