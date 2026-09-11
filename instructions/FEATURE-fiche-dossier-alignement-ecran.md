# FEATURE — aligner la fiche dossier sur l'écran du logiciel de bureau

> Spécifié à partir de **deux captures de l'écran « Dossier de vente »** du légataire (dossier
> `ARS 2025/00357`), confrontées case par case à `ComSaleFolderView` et à nos endpoints. Ce
> document ne décrit que les écarts réels, chacun mesuré sur les **358 dossiers** de `yesRemsProd`.

## 1. La vue est écartée, et c'est mesuré

`ComSaleFolderView` est la vue la plus dense du légataire — 217 colonnes, 39 142 caractères,
**77 sous-requêtes corrélées**. Elle reproduit l'écran exactement : chaque case de la capture a été
retrouvée dans une colonne.

**Elle n'est pourtant pas utilisable**, et la raison diffère de celle du lot prospection :

| Sélection | Médiane sur 12 lignes distinctes |
|---|---:|
| 15 colonnes plates | **363 ms** |
| 16 colonnes financières | 347 ms |
| Les 217 colonnes | 551 ms |

**Aucun élagage.** Demander 15 colonnes coûte autant que 217. La technique du lot 1 — ne mapper
que l'utile pour déclencher l'élagage des sous-requêtes — **ne fonctionne pas ici**. Et le prix
est absurde au regard des volumes : 358 dossiers, 921 échéances, 3 243 règlements.

**Aucune entité de vue n'est donc créée.** Tout ce qui suit se lit sur `ComFolder` / `ComDocument`
/ `StkItem` — colonnes déjà mappées, zéro ajout d'entité — ou se calcule depuis l'échéancier,
comme `montants` le fait déjà (règle 10 du `CLAUDE.md` : toujours calculé, jamais stocké).

## 2. Deux pièges que seule la confrontation à l'écran révèle

**`PriceWithModification` n'est pas le « Prix contractuel ».** C'est un **booléen** — il vaut
`False` sur le dossier de la capture. Le « Prix contractuel : 931 500,00 DH » affiché, c'est
`Price`. Un mapping fait au nom se serait trompé silencieusement.

**Les ratios sont stockés en fraction, pas en pourcentage :**

| Affiché | Colonne | Valeur réelle |
|---|---|---|
| `100.00 %` (quote-part) | `PartPercent` | **1.00** |
| `100.00 %` (probabilité) | `ProbabilityOfFinalisation` | **1.00** |
| `17.18 %` (avance) | `Advance` | **0.17** |
| `-3.85 %` (remise) | `Discount` | **-0.04** |

`probabiliteFinalisation` est **déjà exposé** par notre API, et déjà en fraction. Le contrat ne
change pas — mais son `@Schema` le dit désormais, ce qu'il ne faisait pas.

## 3. Lot (a) — l'échéancier : deux champs

`GET /api/dossiers/{id}/echeancier` couvre déjà Date, Catégorie, Montant, Réglé, Reste et Notes,
et sert même davantage que l'écran (`code`, HT/TTC, TVA, `exclureDuPrix`, `statutCalcule`).
`GET .../reglements` couvre **intégralement** les lignes filles dépliées (payeur, montant, mode,
banque, date) — vérifié sur données réelles : `VIREMENT / CIH` et `CHÈQUE / CRÉDIT AGRICOLE DU
MAROC`.

Manquent deux colonnes :

| Champ | Source | Peuplé |
|---|---|---:|
| `decompte` | `ComDocument.QuittanceCode` | **16,5 %** (152/921) |
| `creeLe` | `ComDocument.UserCreateDateTime` | **100 %** |

⚠️ **`QuittanceCode` n'est jamais `NULL`** — 0 ligne sur 921. C'est une **chaîne vide** quand il
n'y a pas de décompte, et le logiciel de bureau affiche « N.A. ». L'API normalise en `null`. C'est
le quatrième champ de ce projet à porter ce piège ; il est systématique dans ce schéma.

**La colonne « Facture » de l'écran n'est pas reprise** : aucune table de facturation vente
n'existe. `BuyOrderInvoice` (8 lignes) appartient au module achat. Le bouton « Facturer la vente »
n'est pas utilisé, et le champ resterait vide par construction.

### Un correctif entraîné par l'exposition de `creeLe`

`POST /api/dossiers/{id}/echeancier` **ne posait pas `UserCreateDateTime`**. Les 921 échéances
créées par le logiciel de bureau la portent toutes ; les nôtres seraient sorties avec un `creeLe`
nul. Exposer une date que notre propre chemin d'écriture laisse vide, c'est livrer un champ cassé
— `addEcheance` la renseigne donc désormais.

**L'auteur (`UserCreate`) reste non posé**, délibérément : il faudrait faire descendre le
principal jusqu'au service, et aucun champ ne l'expose. C'est la même famille de défaut que le
`UserCreate` manquant de `creerProspection` ; à traiter ensemble le jour où l'auteur sera exposé.

## 4. Lot (b) — l'en-tête

| Champ DTO | Source | Peuplé |
|---|---|---:|
| `prixM2` | `ComFolder.UnitCost` | **98,3 %** |
| `prixHT` | `ComFolder.PriceHT` | **98,3 %** |
| `prixAvantRemiseHT` | `ComFolder.PriceBeforeDiscountHT` | **98,3 %** |
| `articles[].titreFoncier` | `StkItem.TitleNumber` | **69,6 %** |
| `tauxTVA` | `ComFolder.VATRatio` | 42,7 % |
| `articles[].presentationCode` / `presentationLibelle` | `StkItem.StkPresentationType` | 23,2 % |
| `dateValiditeStatut` | `ComFolder.StatusValidityDate` | 11,5 % |
| `quotePart` | `ComFolder.PartPercent` | 100 %, mais **95 % valent 1,00** |

**`UnderItemsPrice` (« S. articles ») est écarté : 2,2 %** — 8 dossiers sur 358. C'est le cas
`prix`/`surface` de la prospection qui se répète : le champ afficherait « 0,00 DH » sur 350
dossiers. Ne pas l'ajouter tant qu'un besoin réel ne le justifie pas.

`quotePart` est conservé malgré sa faible information (95 % à 100 %) parce qu'il est **gratuit**
— colonne déjà mappée — et que les 5 % restants sont précisément les dossiers en indivision, où
l'information compte.

## 5. Lot (c) — l'onglet « Montants », et les formules vérifiées

C'est l'onglet **ouvert par défaut**, donc le cœur de l'écran. Il affiche une grille
Montant / Réglé / Reste à postes fixes. Nos `parCategorie` couvrent déjà les postes issus de
l'échéancier (`AVANCE`, `AUTO-FINANCEMENT`, `FRAIS SYNDIC`…), avec les trois colonnes.

Manquent les **deux lignes de synthèse** et le ratio, qui ne sont pas des catégories d'échéancier.
Les formules ont été **vérifiées sur la totalité des 358 dossiers**, pas déduites d'un exemple :

| Règle | Vérifiée |
|---|---|
| `netAPayer.montant = prix + frais exclus du prix` (dossiers actifs) | **328/328** |
| `netAPayer.montant = netAPayer.regle` (dossiers **sortis**) | **30/30** |
| `reste = montant − regle` | **358/358** |
| `prixVente.regle` = somme des règlements des échéances **non exclues du prix** | **357/358** |

### Le ratio d'avance est écarté, faute de formule vérifiée

L'écran affiche `17.18 %` à gauche de la ligne « Avance ». La relation
`ratio = AdvanceAmount / Price` tient bien **328/328**, mais elle suppose de savoir reconstituer
`AdvanceAmount` depuis notre échéancier — et là, l'égalité avec le total de la catégorie `AVANCE`
ne tient qu'à **350/358**. Les 8 écarts ne sont pas expliqués ; la catégorie
`PRÉ-RÉSERVATION` (8 lignes en base) est le suspect le plus probable, mais ce n'est pas vérifié.

**97,8 % n'est pas une formule, c'est une coïncidence partielle.** Le champ n'est donc pas livré.
Le rétablir demande d'abord de comprendre les 8 dossiers.

**« Sorti » = `StatutDossier.isSortie()`** — `ANNULATION` ou `DESISTEMENT`. La correspondance avec
le `IsCancellation` de la vue est **exacte : 30/30**, sans exception (22 annulations + 8
désistements). Aucun mapping nouveau n'est nécessaire, l'enum existait déjà.

Le sens métier de la règle des sortis : un dossier annulé ne « reste à payer » rien ; son net à
payer se réduit à ce qui a déjà été encaissé, et le reste tombe à zéro.

### Deux anomalies de données, signalées et non corrigées

**`ART 2026/00365` — le legacy double-compte une contre-passation.** Ce dossier porte une paire
de règlements **−470 000 / +470 000** (écriture de correction). Notre calcul les compense et rend
**330 000** ; la vue annonce **1 270 000**, soit 330 000 + 2 × 470 000 : elle additionne la
contre-passation au lieu de l'annuler. **Notre chiffre est l'économiquement juste** ; c'est le seul
des 358 dossiers où les deux divergent. Ne pas « corriger » l'API pour s'aligner sur la vue.

**Un dossier dont l'échéancier ne reconstitue pas le prix stocké.** `ComFolder.Price` est égal à la
somme des échéances non exclues sur 327 des 328 dossiers actifs. L'implémentation prend donc
**`ComFolder.Price` comme valeur faisant foi** plutôt que de la re-dériver — c'est ce que fait la
vue, et c'est cohérent 328/328 avec elle.

## 6. Hors périmètre

- **Les onglets « Dates » et « Dates autres »** — 32 dates de chronologie, dont **12 strictement à
  0 %** et 9 seulement au-dessus de 10 %. `ReservationDate` en particulier est à **0,6 %** (2
  dossiers sur 358) malgré son nom : la réservation se lit ailleurs. À spécifier séparément, une
  fois les captures de ces onglets disponibles.
- **Les onglets « Autres montants », « Annulation / Résiliation », « Autres »** — non vus.
- **Les onglets du bas autres que « Schéma de financement »** (Sous articles, Acquéreurs,
  Modifications, Cadeaux, Dépenses, Factures, Avoirs, Décomptes, Réclamations).
- **La liste des dossiers** — `DossierListItemDto` ne bouge pas.

## 7. Impact front

**Additif, aucune rupture.** Aucun champ retiré, aucun type changé, aucun endpoint re-scopé.

Quatre points à relayer :

1. **Les ratios sont des fractions** : `quotePart`, `probabiliteFinalisation`, `ratioAvance`,
   `remise` valent `1.00` pour 100 %. Multiplier par 100 à l'affichage.
2. **`decompte` vaut `null`, pas `""`** — le legacy affiche « N.A. », c'est un choix
   d'affichage à refaire côté front.
3. **`netAPayer` d'un dossier sorti** (`ANNULATION` / `DESISTEMENT`) vaut ce qui a été encaissé,
   et son `reste` est nul. Ce n'est pas une donnée manquante mais la règle métier.
4. **Ne pas afficher un « S. articles »** : la donnée existe mais est morte (2,2 %).
