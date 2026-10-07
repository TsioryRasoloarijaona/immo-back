# ReferentielController

Listes de référence pour peupler les dropdowns/cases à cocher des formulaires (Prospection, Dossier, Patrimoine...).

**Guide d'implémentation front — sélection Article/Sous-article par Catégorie + Type**
Reproduit le formulaire legacy "Tranche → Catégorie → Type → Article" : l'utilisateur coche une catégorie (ex. "APPARTEMENT") et un type (ex. "2 CHAMBRES + SALON"), et voit apparaître les codes d'articles disponibles pour cette combinaison — pas l'inverse (on ne choisit jamais un article directement par son code dans une longue liste).
1. `GET /api/referentiels/tranches?projetId=` → dropdown Tranche (ou `GET /api/referentiels/projets` d'abord si le Projet n'est pas encore choisi).
2. `GET /api/referentiels/categories-bien` et `GET /api/referentiels/types-bien` → cases à cocher "Catégorie"/"Type", indépendantes de la tranche (affichées une fois pour toutes, pas rechargées à chaque tranche).
3. `GET /api/referentiels/articles?trancheId=&categorieId=&typeId=` (les deux derniers répétables si plusieurs cases cochées) → la liste d'articles qui en résulte, chaque entrée exposant `code`, `description` (le nom), `statut` (DISPONIBLE/OCCUPE) et déjà sa catégorie/son type (`categorieLibelle`/`typeLibelle`) pour affichage sans appel supplémentaire. Sans filtre `categorieId`, les **annexes** (`IsUnderItem` — parkings, terrasses, jardins, mêmes racines sans parent) sont écartées, comme dans l'écran Articles du logiciel de bureau ; cocher explicitement leur catégorie les fait réapparaître. L'utilisateur choisit l'article dans **cette** liste filtrée, pas dans un dropdown de tous les articles de la tranche.
4. Une fois l'article choisi, `GET /api/articles/{id}` donne le détail complet (tranche, catégorie/type, prix, `sousLots`). Si l'article a des sous-articles et que l'utilisateur doit en choisir un, `GET /api/referentiels/sous-articles?articleId=` (mêmes filtres `categorieId`/`typeId` optionnels) rejoue exactement la même mécanique à l'étage du dessous — chaque sous-article y est identifié par son `description` (son nom, ex. "Place de parking sous-sol n°3"), jamais par son seul `code`.
Tous ces appels par défaut ne renvoient que les éléments DISPONIBLE — passer `statut=OCCUPE` explicitement pour voir les indisponibles.

## getRoles

Rôles applicatifs existants (AdmRole), tels qu'ils sont assignés aux comptes dans Omran — Arpent ne
crée ni n'assigne de rôle. Chaque code (ex. "Commercial", "Administrateur") pilote les autorisations
d'accès (hasRole(...)) sur les endpoints réservés et apparaît dans `roles` de GET /api/utilisateurs
et GET /api/mon-compte.

## getResultatsInteraction

Liste les résultats utilisables comme `resultatId` dans `POST /api/prospections/{id}/interactions`
(ex: "Rendez vous pris", "Hors Budget", "Client injoignable ou Boite vocale"...). Chaque entrée
indique via `necessiteDateRdv`/`alerteRecommandee` comment adapter le formulaire, sans avoir à coder
les libellés en dur côté front.

## getNatures

Alimente `natureId` sur `GET /api/articles`/`GET /api/tranches/{id}/articles`
(instructions/from_front.md #4) — `StkKind`, référentiel de la nature physique du bien (ex. "TER"
pour terrain), distinct de Catégorie/Type.

## getEtages

Alimente `etageId` sur `GET /api/articles`/`GET /api/tranches/{id}/articles`
(instructions/from_front.md #4) — `StkFloor` (ex. code "RDC").

## getTypesEmploye

Types d'employé existants (ex. "COM" pour commercial), tels qu'ils qualifient les fiches employé
saisies dans Omran — c'est le `typeEmployeCode` / `typeEmployeLibelle` de GET /api/utilisateurs.
Équivalent de `GET /api/referentiels/tier-types/{typeId}/kinds` déjà filtré sur le CfgTierType
"EMP", pour ne pas obliger le front à connaître son id.

## getTypesPieceJointe

Liste le référentiel `AdmAttachedFileType` (ex. "PIECE D'IDENTITE", "CONTRAT", "FICHE PERMISSION
REMISE DES CLES"...), triée par description. À utiliser pour peupler le sélecteur de type lors du
dépôt d'une pièce jointe de dossier (`POST /api/dossiers/{id}/pieces-jointes`, paramètre `typeId`).

## getTypesReglement

Alimente `CreerReglementRequest.typeId` (`POST .../echeancier/{echeanceId}/reglements`) — table
`StlSettlementType`, **distincte** de `GET /api/referentiels/modes-paiement` (qui alimente
`CreerDossierRequest.modePaieId`, un champ différent utilisé uniquement à la création du dossier).
Chaque entrée porte `banqueObligatoire` : quand vrai (ex. CHÈQUE, VIREMENT), le formulaire doit
aussi demander une banque via `GET /api/referentiels/banques` ; quand faux (ex. ESPÈCES), ce champ
n'a pas de sens et doit être masqué. Porte aussi `referenceObligatoire` : quand vrai (ex. CHÈQUE,
EFFET), le formulaire doit demander une référence (`CreerReglementRequest.reference`) — **enforcé
côté serveur** (400 si absente à la création), contrairement à `banqueObligatoire` qui reste
indicatif.

## getBanques

Alimente `CreerReglementRequest.banqueId` — `CfgTier` de type "BAN". N'a de sens que lorsque le type
de règlement choisi (`GET /api/referentiels/types-reglement`) a `banqueObligatoire=true`.

## getEntites

Entités juridiques actives (CfgCompany, ADMOD/GHRIZA) — mêmes valeurs que le filtre "Entité" de
l'écran legacy "Encaissements Vente".

## getProjets

Tous les projets (CfgProject), pour peupler un sélecteur par nom/code plutôt que par id — premier
maillon de la cascade Projet → Tranche → Article.

## getTranches

Toutes les tranches (CfgTranche), ou uniquement celles du projet `projetId` si fourni — deuxième
maillon de la cascade Projet → Tranche → Article. Contrairement à `GET /api/tranches`, cette liste
n'est pas paginée : elle est prévue pour peupler un `<select>` directement.

## getArticles

Articles racine (hors sous-articles) de la tranche `trancheId` (obligatoire), filtrés par
disponibilité (défaut: DISPONIBLE) et optionnellement par catégorie (`categorieId`, ex.
"APPARTEMENT") et type (`typeId`, ex. "1 CHAMBRES + SALON") — reproduit la cascade de cases à cocher
"Catégorie"/"Type" du formulaire de qualification legacy avant de choisir l'article. Chaque entrée
expose son `code` ET sa `description` (le nom de l'article), plus l'id/code/libellé de sa catégorie
et de son type, pour peupler ces cases à cocher sans appel séparé. Dernier maillon de la cascade
Projet → Tranche → Article avant sélection d'un sous-article éventuel via `GET
/api/referentiels/sous-articles`.

## getSousArticles

Sous-articles (StkItem.ParentId) de l'article `articleId` (obligatoire), filtrés par disponibilité
(défaut: DISPONIBLE) et optionnellement par catégorie/type comme `GET /api/referentiels/articles` —
ex. sélectionner une place de parking annexée au lot déjà choisi. Chaque entrée expose son
`description` (son nom), pas seulement son `code`.

## getColonnesProspections

Alimente le **sélecteur de colonnes** de `GET /api/prospections`. Chaque entrée donne le nom
technique à passer dans `?colonnes=`, son libellé français, son groupe d'affichage, son type (pour
aligner et formater), et si elle fait partie de la sélection par défaut.

Le front n'a ainsi aucune liste de colonnes à coder en dur, et le catalogue est la même source que
la liste blanche qui valide `?colonnes=` — les deux ne peuvent pas diverger.

Les colonnes du groupe `ACTIVITE` (et `CONVERSION`) proviennent d'une vue de la base légataire : les
demander déclenche une requête supplémentaire, mesurée à 7 ms pour une page de 20. Ne pas les
demander n'en déclenche aucune.

Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de groupement et les bandes «
Total : » de `/groupes` — le front n'en code donc aucune liste en dur. Toutes les colonnes ne sont
pas groupables : voir instructions/FEATURE-groupement-listes.md §2.

Référentiel statique, sans paramètre : cacheable côté client.

## getColonnesProspects

Alimente le **sélecteur de colonnes** de `GET /api/prospects`, sur le même contrat que celui des
prospections : nom technique à passer dans `?colonnes=`, libellé français, groupe, type d'affichage,
et appartenance à la sélection par défaut.

La sélection par défaut reproduit **exactement** les 10 colonnes servies avant l'introduction du
paramètre — une intégration qui ignore `colonnes` ne voit aucun changement.

Deux réserves utiles au moment de bâtir le sélecteur : `profession` est alimentée par une colonne
**mesurée à 0 ligne sur 13 664** et sera donc toujours vide ; `kind` l'est sur 14 339 des 14 516
tiers réels. Les **tranches d'intérêt sont absentes du catalogue** : elles exigent une requête par
prospect et restent réservées à la fiche.

Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de groupement et les bandes «
Total : » de `/groupes` — le front n'en code donc aucune liste en dur. Toutes les colonnes ne sont
pas groupables : voir instructions/FEATURE-groupement-listes.md §2.

Référentiel statique, sans paramètre : cacheable côté client.

## getColonnesDossiers

Alimente le **sélecteur de colonnes** de `GET /api/dossiers`, sur le même contrat que les deux
précédents.

La sélection par défaut reproduit **exactement** les 13 colonnes servies avant l'introduction du
paramètre.

Les colonnes du groupe `PATRIMOINE` traversent la chaîne article → tranche → projet → société : les
demander coûte trois jointures paresseuses par ligne, ne pas les demander n'en coûte aucune. Même
règle pour le groupe `ORIGINE`, qui déréférence la prospection.

Ne figurent pas au catalogue, et c'est délibéré : les **articles** (une requête par dossier), la
**ventilation financière du légataire** (5 postes vivants sur 12, et un bloc `Agreement*` qui
diverge du bloc principal sur 20 % des lignes sans qu'on sache lequel fait foi) et **`notes`**
(mesurée à 0 ligne sur 358).

Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de groupement et les bandes «
Total : » de `/groupes` — le front n'en code donc aucune liste en dur. Toutes les colonnes ne sont
pas groupables : voir instructions/FEATURE-groupement-listes.md §2.

Référentiel statique, sans paramètre : cacheable côté client.

## getColonnesEncaissements

Alimente le **sélecteur de colonnes** de `GET /api/encaissements`, sur le même contrat que les trois
précédents.

La sélection par défaut reproduit **exactement** les 27 colonnes servies avant l'introduction du
paramètre.

**Le catalogue a été bâti sur mesure, pas sur la trace.** L'écran légataire fait un `SELECT
StlSettlementView.*` — toute la vue, 107 colonnes, sans en nommer aucune — et ne stocke pas la
disposition de sa grille en base. Les colonnes retenues sont donc celles qui portent réellement une
information sur le périmètre servi (643 règlements réels).

Trois réserves utiles au moment de bâtir le sélecteur. `depotId`/`depotCode` sont mesurés à **0
ligne sur 643**, `compteId`/`compteCode` à 12 (1,9 %) et `dateReglement` à 10 (1,6 %) : servis parce
que le contrat les publie, mais ils ne doivent pas être proposés comme des filtres utiles. À
l'inverse `description` est renseignée sur les 643 lignes avec 530 valeurs distinctes — c'est la
colonne la plus informative du lot.

Ne figurent pas au catalogue, et c'est mesuré : `Group1Label`/`Group2Label`/`Group3Label` (les
colonnes de groupement du légataire lui-même, **0 sur 643**),
`Category`/`ItemNumber`/`ItemDescription` (renseignées partout mais **une seule valeur distincte** :
des placeholders), et `AmountRS`/`AmountStamp` (non nulles mais **toujours à zéro**).

Porte aussi **`groupable`** et **`agregeable`**, qui pilotent la bande de groupement et les bandes «
Total : » de `/groupes` — le front n'en code donc aucune liste en dur. **`dossierId`, `dossierCode`,
`montantBrutRestant`, `montantNet` et `notes` ne sont pas groupables** : les deux premiers
traverseraient une association to-many (8 règlements portent sur plusieurs dossiers, la somme des
effectifs ne retomberait plus sur le total), les deux suivants sont calculés en Java, et `notes` est
un `nvarchar(max)` que le `COUNT(DISTINCT)` refuse. Piloter le sélecteur sur le booléen, pas sur une
liste en dur.

Référentiel statique, sans paramètre : cacheable côté client.

## getColonnesSuiviActions

Colonnes de `GET /api/suivi-actions` : nom (à passer dans `?colonnes=`), libellé, groupe
d'affichage, type, sélection par défaut, et les booléens `groupable` / `agregeable` qui pilotent
`/groupes`.

**Établi sur l'écran OMRAN** : les colonnes de la vue `ComActionMessageView` que lit l'écran « Suivi
des actions » et que liste sa documentation, **filtrées par la mesure** sur les 48 213 actions
réelles. Désistement, transfert client, transfert produit (0 ligne), SAV (2), location, offre et
syndic (0) n'y figurent pas.

**Non groupables** : `rang`, `alerteActive`, `etat`, `personnel` — calculées pour la page servie
seulement — et `message`, un `nvarchar(max)`. **Aucune colonne agrégeable** : la mesure d'un groupe
est son effectif, le nombre d'actions.

Référentiel statique, sans paramètre : cacheable côté client.
