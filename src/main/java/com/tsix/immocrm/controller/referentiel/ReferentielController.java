package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.referentiel.OrigineProspectionDto;
import com.tsix.immocrm.dto.referentiel.PaysDto;
import com.tsix.immocrm.dto.referentiel.ProfilCategorieDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.dto.referentiel.ResultatInteractionDto;
import com.tsix.immocrm.dto.referentiel.TitreDto;
import com.tsix.immocrm.dto.referentiel.TypeReglementDto;
import com.tsix.immocrm.dto.referentiel.VilleDto;
import com.tsix.immocrm.services.referentiel.ReferentielService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.dto.referentiel.ColonneDto;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

/** GAP-02 — read-only lookup tables backing the Prospect/Prospection qualification forms. */
@Tag(name = "Référentiels", description = "Listes de référence pour peupler les dropdowns/cases "
        + "à cocher des formulaires (Prospection, Dossier, Patrimoine...).\n\n"
        + "**Guide d'implémentation front — sélection Article/Sous-article par Catégorie + Type**\n"
        + "Reproduit le formulaire legacy \"Tranche → Catégorie → Type → Article\" : l'utilisateur "
        + "coche une catégorie (ex. \"APPARTEMENT\") et un type (ex. \"2 CHAMBRES + SALON\"), et "
        + "voit apparaître les codes d'articles disponibles pour cette combinaison — pas l'inverse "
        + "(on ne choisit jamais un article directement par son code dans une longue liste).\n"
        + "1. `GET /api/referentiels/tranches?projetId=` → dropdown Tranche (ou "
        + "`GET /api/referentiels/projets` d'abord si le Projet n'est pas encore choisi).\n"
        + "2. `GET /api/referentiels/categories-bien` et `GET /api/referentiels/types-bien` → "
        + "cases à cocher \"Catégorie\"/\"Type\", indépendantes de la tranche (affichées une fois "
        + "pour toutes, pas rechargées à chaque tranche).\n"
        + "3. `GET /api/referentiels/articles?trancheId=&categorieId=&typeId=` (les deux derniers "
        + "répétables si plusieurs cases cochées) → la liste d'articles qui en résulte, chaque "
        + "entrée exposant `code`, `description` (le nom), `statut` (DISPONIBLE/OCCUPE) et déjà "
        + "sa catégorie/son type (`categorieLibelle`/`typeLibelle`) pour affichage sans appel "
        + "supplémentaire. Sans filtre `categorieId`, les **annexes** (`IsUnderItem` — parkings, "
        + "terrasses, jardins, mêmes racines sans parent) sont écartées, comme dans l'écran "
        + "Articles du logiciel de bureau ; cocher explicitement leur catégorie les fait "
        + "réapparaître. L'utilisateur choisit "
        + "l'article dans **cette** liste filtrée, pas dans "
        + "un dropdown de tous les articles de la tranche.\n"
        + "4. Une fois l'article choisi, `GET /api/articles/{id}` donne le détail complet "
        + "(tranche, catégorie/type, prix, `sousLots`). Si l'article a des sous-articles et que "
        + "l'utilisateur doit en choisir un, `GET /api/referentiels/sous-articles?articleId=` "
        + "(mêmes filtres `categorieId`/`typeId` optionnels) rejoue exactement la même mécanique "
        + "à l'étage du dessous — chaque sous-article y est identifié par son `description` (son "
        + "nom, ex. \"Place de parking sous-sol n°3\"), jamais par son seul `code`.\n"
        + "Tous ces appels par défaut ne renvoient que les éléments DISPONIBLE — passer "
        + "`statut=OCCUPE` explicitement pour voir les indisponibles.")
@RestController
@RequestMapping("/api/referentiels")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/roles")
    @Operation(
            summary = "Liste des rôles applicatifs (AdmRole)",
            description = "Rôles applicatifs existants (AdmRole), tels qu'ils sont assignés aux "
                    + "comptes dans Omran — Arpent ne crée ni n'assigne de rôle. Chaque code "
                    + "(ex. \"Commercial\", \"Administrateur\") pilote les autorisations d'accès "
                    + "(hasRole(...)) sur les endpoints réservés et apparaît dans "
                    + "`roles` de GET /api/utilisateurs et GET /api/mon-compte."
    )
    public List<ReferentielItemDto> getRoles() {
        return referentielService.roles();
    }

    @GetMapping("/etapes-prospection")
    public List<ReferentielItemDto> getEtapesProspection() {
        return referentielService.etapesProspection();
    }

    @GetMapping("/origines-prospection")
    public List<OrigineProspectionDto> getOriginesProspection() {
        return referentielService.originesProspection();
    }

    @GetMapping("/origines-prospection/{origineId}/supports")
    public List<ReferentielItemDto> getSupportsForOrigine(@PathVariable UUID origineId) {
        return referentielService.supportsForOrigine(origineId);
    }

    @GetMapping("/raisons-cloture")
    public List<ReferentielItemDto> getRaisonsCloture() {
        return referentielService.raisonsCloture();
    }

    @GetMapping("/types-contact-prospection")
    public List<ReferentielItemDto> getTypesContactProspection() {
        return referentielService.typesContactProspection();
    }

    @GetMapping("/resultats-interaction")
    @Operation(
            summary = "Résultats possibles pour une interaction (dropdown \"Résultat\")",
            description = "Liste les résultats utilisables comme `resultatId` dans "
                    + "`POST /api/prospections/{id}/interactions` (ex: \"Rendez vous pris\", "
                    + "\"Hors Budget\", \"Client injoignable ou Boite vocale\"...). Chaque entrée "
                    + "indique via `necessiteDateRdv`/`alerteRecommandee` comment adapter le "
                    + "formulaire, sans avoir à coder les libellés en dur côté front."
    )
    public List<ResultatInteractionDto> getResultatsInteraction() {
        return referentielService.resultatsInteraction();
    }

    @GetMapping("/actions-commerciales")
    public List<ReferentielItemDto> getActionsCommerciales() {
        return referentielService.actionsCommerciales();
    }

    @GetMapping("/vocations")
    public List<ReferentielItemDto> getVocations() {
        return referentielService.vocations();
    }

    @GetMapping("/natures")
    @Operation(
            summary = "Dropdown \"Nature\" pour le filtre natureId des endpoints Article",
            description = "Alimente `natureId` sur `GET /api/articles`/`GET /api/tranches/{id}/articles` "
                    + "(instructions/from_front.md #4) — `StkKind`, référentiel de la nature physique "
                    + "du bien (ex. \"TER\" pour terrain), distinct de Catégorie/Type."
    )
    public List<ReferentielItemDto> getNatures() {
        return referentielService.natures();
    }

    @GetMapping("/etages")
    @Operation(
            summary = "Dropdown \"Étage\" pour le filtre etageId des endpoints Article",
            description = "Alimente `etageId` sur `GET /api/articles`/`GET /api/tranches/{id}/articles` "
                    + "(instructions/from_front.md #4) — `StkFloor` (ex. code \"RDC\")."
    )
    public List<ReferentielItemDto> getEtages() {
        return referentielService.etages();
    }

    @GetMapping("/tier-types")
    public List<ReferentielItemDto> getTierTypes() {
        return referentielService.tierTypes();
    }

    @GetMapping("/tier-types/{typeId}/kinds")
    public List<ReferentielItemDto> getTierKindsForType(@PathVariable UUID typeId) {
        return referentielService.tierKindsForType(typeId);
    }

    @GetMapping("/types-employe")
    @Operation(
            summary = "Types d'employé (CfgTierKind)",
            description = "Types d'employé existants (ex. \"COM\" pour commercial), tels qu'ils "
                    + "qualifient les fiches employé saisies dans Omran — c'est le "
                    + "`typeEmployeCode` / `typeEmployeLibelle` de GET /api/utilisateurs. "
                    + "Équivalent de `GET /api/referentiels/tier-types/{typeId}/kinds` déjà "
                    + "filtré sur le CfgTierType \"EMP\", pour ne pas obliger le front à connaître "
                    + "son id."
    )
    public List<ReferentielItemDto> getTypesEmploye() {
        return referentielService.typesEmploye();
    }

    @GetMapping("/titres")
    public List<TitreDto> getTitres() {
        return referentielService.titres();
    }

    @GetMapping("/sexes")
    public List<ReferentielItemDto> getSexes() {
        return referentielService.sexes();
    }

    @GetMapping("/pays")
    public List<PaysDto> getPays() {
        return referentielService.pays();
    }

    @GetMapping("/nationalites")
    public List<ReferentielItemDto> getNationalites() {
        return referentielService.nationalites();
    }

    @GetMapping("/situations-familiales")
    public List<ReferentielItemDto> getSituationsFamiliales() {
        return referentielService.situationsFamiliales();
    }

    @GetMapping("/types-residence")
    public List<ReferentielItemDto> getTypesResidence() {
        return referentielService.typesResidence();
    }

    @GetMapping("/regimes-matrimoniaux")
    public List<ReferentielItemDto> getRegimesMatrimoniaux() {
        return referentielService.regimesMatrimoniaux();
    }

    @GetMapping("/types-piece-identite")
    public List<ReferentielItemDto> getTypesPieceIdentite() {
        return referentielService.typesPieceIdentite();
    }

    @GetMapping("/types-piece-jointe")
    @Operation(
            summary = "Types de pièce jointe disponibles",
            description = "Liste le référentiel `AdmAttachedFileType` (ex. \"PIECE D'IDENTITE\", "
                    + "\"CONTRAT\", \"FICHE PERMISSION REMISE DES CLES\"...), triée par "
                    + "description. À utiliser pour peupler le sélecteur de type lors du dépôt "
                    + "d'une pièce jointe de dossier (`POST /api/dossiers/{id}/pieces-jointes`, "
                    + "paramètre `typeId`)."
    )
    public List<ReferentielItemDto> getTypesPieceJointe() {
        return referentielService.typesPieceJointe();
    }

    @GetMapping("/professions")
    public List<ReferentielItemDto> getProfessions() {
        return referentielService.professions();
    }

    @GetMapping("/salaires")
    public List<ReferentielItemDto> getSalaires() {
        return referentielService.salaires();
    }

    @GetMapping("/categories-bien")
    public List<ReferentielItemDto> getCategoriesBien() {
        return referentielService.categoriesBien();
    }

    @GetMapping("/types-bien")
    public List<ReferentielItemDto> getTypesBien() {
        return referentielService.typesBien();
    }

    @GetMapping("/types-relation-contact")
    public List<ReferentielItemDto> getTypesRelationContact() {
        return referentielService.typesRelationContact();
    }

    @GetMapping("/villes")
    public PageDto<VilleDto> getVilles(@RequestParam(required = false) UUID paysId,
                                        @RequestParam(required = false) String search,
                                        @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(referentielService.villes(paysId, search, pageable));
    }

    @GetMapping("/modes-paiement")
    public List<ReferentielItemDto> getModesPaiement() {
        return referentielService.modesPaiement();
    }

    @GetMapping("/types-reglement")
    @Operation(
            summary = "Dropdown \"Type\" pour l'ajout d'un règlement",
            description = "Alimente `CreerReglementRequest.typeId` (`POST "
                    + ".../echeancier/{echeanceId}/reglements`) — table `StlSettlementType`, "
                    + "**distincte** de `GET /api/referentiels/modes-paiement` (qui alimente "
                    + "`CreerDossierRequest.modePaieId`, un champ différent utilisé uniquement à la "
                    + "création du dossier). Chaque entrée porte `banqueObligatoire` : quand vrai "
                    + "(ex. CHÈQUE, VIREMENT), le formulaire doit aussi demander une banque via "
                    + "`GET /api/referentiels/banques` ; quand faux (ex. ESPÈCES), ce champ n'a pas "
                    + "de sens et doit être masqué. Porte aussi `referenceObligatoire` : quand vrai "
                    + "(ex. CHÈQUE, EFFET), le formulaire doit demander une référence "
                    + "(`CreerReglementRequest.reference`) — **enforcé côté serveur** (400 si "
                    + "absente à la création), contrairement à `banqueObligatoire` qui reste "
                    + "indicatif."
    )
    public List<TypeReglementDto> getTypesReglement() {
        return referentielService.typesReglement();
    }

    @GetMapping("/banques")
    @Operation(
            summary = "Dropdown \"Banque\" pour l'ajout d'un règlement",
            description = "Alimente `CreerReglementRequest.banqueId` — `CfgTier` de type \"BAN\". "
                    + "N'a de sens que lorsque le type de règlement choisi "
                    + "(`GET /api/referentiels/types-reglement`) a `banqueObligatoire=true`."
    )
    public List<ReferentielItemDto> getBanques() {
        return referentielService.banques();
    }

    @GetMapping("/entites")
    @Operation(
            summary = "Dropdown \"Entité\" pour GET /api/encaissements",
            description = "Entités juridiques actives (CfgCompany, ADMOD/GHRIZA) — mêmes valeurs "
                    + "que le filtre \"Entité\" de l'écran legacy \"Encaissements Vente\"."
    )
    public List<ReferentielItemDto> getEntites() {
        return referentielService.entites();
    }

    @GetMapping("/comptes")
    @Operation(
            summary = "Dropdown \"Compte\" pour GET /api/encaissements",
            description = "Comptes bancaires (StlAccount) — un compte par entité juridique en "
                    + "données réelles."
    )
    public List<ReferentielItemDto> getComptes() {
        return referentielService.comptes();
    }

    @GetMapping("/depots")
    @Operation(
            summary = "Dropdown \"Dépôt\" pour GET /api/encaissements",
            description = "Catégories de dépôt (StlDeposit) — 3 lignes en données réelles : "
                    + "FOURNISSEUR/CLIENT/BANQUE."
    )
    public List<ReferentielItemDto> getDepots() {
        return referentielService.depots();
    }

    @GetMapping("/categories-echeance")
    public List<ReferentielItemDto> getCategoriesEcheance() {
        return referentielService.categoriesEcheance();
    }

    @GetMapping("/profils-prospection")
    public List<ProfilCategorieDto> getProfilsProspection() {
        return referentielService.profilsProspection();
    }

    @GetMapping("/projets")
    @Operation(
            summary = "Dropdown \"Projet\"",
            description = "Tous les projets (CfgProject), pour peupler un sélecteur par nom/code "
                    + "plutôt que par id — premier maillon de la cascade Projet → Tranche → Article."
    )
    public List<ReferentielItemDto> getProjets() {
        return referentielService.projets();
    }

    @GetMapping("/tranches")
    @Operation(
            summary = "Dropdown \"Tranche\"",
            description = "Toutes les tranches (CfgTranche), ou uniquement celles du projet "
                    + "`projetId` si fourni — deuxième maillon de la cascade Projet → Tranche → "
                    + "Article. Contrairement à `GET /api/tranches`, cette liste n'est pas paginée : "
                    + "elle est prévue pour peupler un `<select>` directement."
    )
    public List<ReferentielItemDto> getTranches(
            @Parameter(description = "Id du projet (CfgProject.id) pour restreindre les tranches. Absent = toutes.")
            @RequestParam(required = false) UUID projetId) {
        return referentielService.tranches(projetId);
    }

    @GetMapping("/articles")
    @Operation(
            summary = "Dropdown \"Article\" d'une tranche",
            description = "Articles racine (hors sous-articles) de la tranche `trancheId` "
                    + "(obligatoire), filtrés par disponibilité (défaut: DISPONIBLE) et "
                    + "optionnellement par catégorie (`categorieId`, ex. \"APPARTEMENT\") et type "
                    + "(`typeId`, ex. \"1 CHAMBRES + SALON\") — reproduit la cascade de cases à "
                    + "cocher \"Catégorie\"/\"Type\" du formulaire de qualification legacy avant de "
                    + "choisir l'article. Chaque entrée expose son `code` ET sa `description` "
                    + "(le nom de l'article), plus l'id/code/libellé de sa catégorie et de son "
                    + "type, pour peupler ces cases à cocher sans appel séparé. Dernier maillon de "
                    + "la cascade Projet → Tranche → Article avant sélection d'un sous-article "
                    + "éventuel via `GET /api/referentiels/sous-articles`."
    )
    public List<ArticleListItemDto> getArticles(
            @Parameter(description = "Id de la tranche (CfgTranche.id). Obligatoire.")
            @RequestParam UUID trancheId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId) {
        return referentielService.articles(trancheId, statut, categorieId, typeId);
    }

    @GetMapping("/sous-articles")
    @Operation(
            summary = "Dropdown \"Sous-article\" d'un article",
            description = "Sous-articles (StkItem.ParentId) de l'article `articleId` (obligatoire), "
                    + "filtrés par disponibilité (défaut: DISPONIBLE) et optionnellement par "
                    + "catégorie/type comme `GET /api/referentiels/articles` — ex. sélectionner une "
                    + "place de parking annexée au lot déjà choisi. Chaque entrée expose son "
                    + "`description` (son nom), pas seulement son `code`."
    )
    public List<ArticleListItemDto> getSousArticles(
            @Parameter(description = "Id de l'article parent (StkItem.id). Obligatoire.")
            @RequestParam UUID articleId,
            @Parameter(description = "Id(s) de StkItemCategory. Répétable, correspondance OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> categorieId,
            @Parameter(description = "Id(s) de StkItemType. Répétable, correspondance OR. Absent = tous.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "Filtre sur la disponibilité. Absent = DISPONIBLE.")
            @RequestParam(required = false) StatutArticle statut) {
        return referentielService.sousArticles(articleId, statut, categorieId, typeId);
    }

    @GetMapping("/colonnes/prospections")
    @Operation(
            summary = "Catalogue des colonnes de la liste des prospections",
            description = "Alimente le **sélecteur de colonnes** de `GET /api/prospections`. "
                    + "Chaque entrée donne le nom technique à passer dans `?colonnes=`, son "
                    + "libellé français, son groupe d'affichage, son type (pour aligner et "
                    + "formater), et si elle fait partie de la sélection par défaut.\n\n"
                    + "Le front n'a ainsi aucune liste de colonnes à coder en dur, et le "
                    + "catalogue est la même source que la liste blanche qui valide `?colonnes=` "
                    + "— les deux ne peuvent pas diverger.\n\n"
                    + "Les colonnes du groupe `ACTIVITE` (et `CONVERSION`) proviennent d'une vue "
                    + "de la base légataire : les demander déclenche une requête supplémentaire, "
                    + "mesurée à 7 ms pour une page de 20. Ne pas les demander n'en déclenche "
                    + "aucune.\n\n"
                    + "Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de "
                    + "groupement et les bandes « Total : » de `/groupes` — le front n'en code "
                    + "donc aucune liste en dur. Toutes les colonnes ne sont pas groupables : "
                    + "voir instructions/FEATURE-groupement-listes.md §2.\n\n"
                    + "Référentiel statique, sans paramètre : cacheable côté client."
    )
    public List<ColonneDto> getColonnesProspections() {
        return Arrays.stream(ColonneProspection.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/prospects")
    @Operation(
            summary = "Catalogue des colonnes de la liste des prospects",
            description = "Alimente le **sélecteur de colonnes** de `GET /api/prospects`, sur le "
                    + "même contrat que celui des prospections : nom technique à passer dans "
                    + "`?colonnes=`, libellé français, groupe, type d'affichage, et appartenance "
                    + "à la sélection par défaut.\n\n"
                    + "La sélection par défaut reproduit **exactement** les 10 colonnes servies "
                    + "avant l'introduction du paramètre — une intégration qui ignore `colonnes` "
                    + "ne voit aucun changement.\n\n"
                    + "Deux réserves utiles au moment de bâtir le sélecteur : `profession` est "
                    + "alimentée par une colonne **mesurée à 0 ligne sur 13 664** et sera donc "
                    + "toujours vide ; `kind` l'est sur 14 339 des 14 516 tiers réels. Les "
                    + "**tranches d'intérêt sont absentes du catalogue** : elles exigent une "
                    + "requête par prospect et restent réservées à la fiche.\n\n"
                    + "Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de "
                    + "groupement et les bandes « Total : » de `/groupes` — le front n'en code "
                    + "donc aucune liste en dur. Toutes les colonnes ne sont pas groupables : "
                    + "voir instructions/FEATURE-groupement-listes.md §2.\n\n"
                    + "Référentiel statique, sans paramètre : cacheable côté client."
    )
    public List<ColonneDto> getColonnesProspects() {
        return Arrays.stream(ColonneProspect.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/dossiers")
    @Operation(
            summary = "Catalogue des colonnes de la liste des dossiers de vente",
            description = "Alimente le **sélecteur de colonnes** de `GET /api/dossiers`, sur le "
                    + "même contrat que les deux précédents.\n\n"
                    + "La sélection par défaut reproduit **exactement** les 13 colonnes servies "
                    + "avant l'introduction du paramètre.\n\n"
                    + "Les colonnes du groupe `PATRIMOINE` traversent la chaîne article → tranche "
                    + "→ projet → société : les demander coûte trois jointures paresseuses par "
                    + "ligne, ne pas les demander n'en coûte aucune. Même règle pour le groupe "
                    + "`ORIGINE`, qui déréférence la prospection.\n\n"
                    + "Ne figurent pas au catalogue, et c'est délibéré : les **articles** (une "
                    + "requête par dossier), la **ventilation financière du légataire** (5 postes "
                    + "vivants sur 12, et un bloc `Agreement*` qui diverge du bloc principal sur "
                    + "20 % des lignes sans qu'on sache lequel fait foi) et **`notes`** (mesurée à "
                    + "0 ligne sur 358).\n\n"
                    + "Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de "
                    + "groupement et les bandes « Total : » de `/groupes` — le front n'en code "
                    + "donc aucune liste en dur. Toutes les colonnes ne sont pas groupables : "
                    + "voir instructions/FEATURE-groupement-listes.md §2.\n\n"
                    + "Référentiel statique, sans paramètre : cacheable côté client."
    )
    public List<ColonneDto> getColonnesDossiers() {
        return Arrays.stream(ColonneDossier.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/encaissements")
    @Operation(
            summary = "Catalogue des colonnes de la liste des encaissements",
            description = "Alimente le **sélecteur de colonnes** de `GET /api/encaissements`, sur "
                    + "le même contrat que les trois précédents.\n\n"
                    + "La sélection par défaut reproduit **exactement** les 27 colonnes servies "
                    + "avant l'introduction du paramètre.\n\n"
                    + "**Le catalogue a été bâti sur mesure, pas sur la trace.** L'écran légataire "
                    + "fait un `SELECT StlSettlementView.*` — toute la vue, 107 colonnes, sans en "
                    + "nommer aucune — et ne stocke pas la disposition de sa grille en base. Les "
                    + "colonnes retenues sont donc celles qui portent réellement une information "
                    + "sur le périmètre servi (643 règlements réels).\n\n"
                    + "Trois réserves utiles au moment de bâtir le sélecteur. `depotId`/`depotCode` "
                    + "sont mesurés à **0 ligne sur 643**, `compteId`/`compteCode` à 12 (1,9 %) et "
                    + "`dateReglement` à 10 (1,6 %) : servis parce que le contrat les publie, mais "
                    + "ils ne doivent pas être proposés comme des filtres utiles. À l'inverse "
                    + "`description` est renseignée sur les 643 lignes avec 530 valeurs distinctes "
                    + "— c'est la colonne la plus informative du lot.\n\n"
                    + "Ne figurent pas au catalogue, et c'est mesuré : `Group1Label`/`Group2Label`/"
                    + "`Group3Label` (les colonnes de groupement du légataire lui-même, **0 sur "
                    + "643**), `Category`/`ItemNumber`/`ItemDescription` (renseignées partout mais "
                    + "**une seule valeur distincte** : des placeholders), et `AmountRS`/"
                    + "`AmountStamp` (non nulles mais **toujours à zéro**).\n\n"
                    + "Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de "
                    + "groupement et les bandes « Total : » de `/groupes` — le front n'en code donc "
                    + "aucune liste en dur. **`dossierId`, `dossierCode`, `montantBrutRestant`, "
                    + "`montantNet` et `notes` ne sont pas groupables** : les deux premiers "
                    + "traverseraient une association to-many (8 règlements portent sur plusieurs "
                    + "dossiers, la somme des effectifs ne retomberait plus sur le total), les deux "
                    + "suivants sont calculés en Java, et `notes` est un `nvarchar(max)` que le "
                    + "`COUNT(DISTINCT)` refuse. Piloter le sélecteur sur le booléen, pas sur une "
                    + "liste en dur.\n\n"
                    + "Référentiel statique, sans paramètre : cacheable côté client."
    )
    public List<ColonneDto> getColonnesEncaissements() {
        return Arrays.stream(ColonneEncaissement.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }

    @GetMapping("/colonnes/suivi-actions")
    @Operation(
            summary = "Catalogue des colonnes du suivi des actions",
            description = "Colonnes de `GET /api/suivi-actions` : nom (à passer dans `?colonnes=`), "
                    + "libellé, groupe d'affichage, type, sélection par défaut, et les booléens "
                    + "`groupable` / `agregeable` qui pilotent `/groupes`.\n\n"
                    + "**Établi sur l'écran OMRAN** : les colonnes de la vue `ComActionMessageView` "
                    + "que lit l'écran « Suivi des actions » et que liste sa documentation, "
                    + "**filtrées par la mesure** sur les 48 213 actions réelles. Désistement, "
                    + "transfert client, transfert produit (0 ligne), SAV (2), location, offre et "
                    + "syndic (0) n'y figurent pas.\n\n"
                    + "**Non groupables** : `rang`, `alerteActive`, `etat`, `personnel` — calculées "
                    + "pour la page servie seulement — et `message`, un `nvarchar(max)`. **Aucune "
                    + "colonne agrégeable** : la mesure d'un groupe est son effectif, le nombre "
                    + "d'actions.\n\n"
                    + "Référentiel statique, sans paramètre : cacheable côté client."
    )
    public List<ColonneDto> getColonnesSuiviActions() {
        return Arrays.stream(ColonneSuiviAction.values())
                .map(c -> ColonneDto.de(c.nom(), c.libelle(), c.groupe().name(), c.type().name(),
                        c.parDefaut(), c.chemin()))
                .toList();
    }
}
