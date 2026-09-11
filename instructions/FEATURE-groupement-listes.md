# FEATURE — Groupement et agrégats sur les listes

> **⚠️ §2 et §4.1 de ce document sont périmés.** Le lot suivant
> (`FEATURE-groupement-toutes-colonnes.md`) a renversé deux décisions prises ici :
> les montants **sont** groupables, et le groupe sans valeur est **écarté par défaut**.
> Le reste du document — §1 (pourquoi c'est un endpoint), §3, §4.2, §5 (l'invariant) — reste exact
> et fait toujours foi.

Reproduit la *group-by band* du logiciel de bureau (OMRAN Estate) sur les trois listes
`Dossiers de vente`, `Prospections vente` et `Pré-Prospections` : l'utilisateur choisit une
colonne, la liste se replie en groupes, et chaque groupe porte son effectif et ses totaux.

Référence : capture de l'onglet « Dossiers de vente » groupé par `Client`, bandes
`Total : 1 350 000 DH` alignées sous la colonne `Prix de Vente`, groupes repliés par défaut avec
leur compteur `(1 item)`.

---

## 1. Pourquoi c'est un endpoint et pas une affaire de front

C'est le point qui décide de toute l'architecture, et il n'est pas évident.

OMRAN est un client lourd : son grid détient **tout le jeu de résultats en mémoire**, donc grouper
est une opération locale et gratuite. Notre API est paginée à 20.

Si le front groupait ce qu'il reçoit, il grouperait **une page** : les groupes seraient tronqués et
les totaux faux. Le `Total : 1 350 000 DH` de la capture est le total du client sur tout le
périmètre filtré, pas sur la page courante. Le groupement est donc une agrégation serveur.

**Corollaire à ne pas contourner** : l'agrégation doit réutiliser la `Specification` qui construit
déjà la liste (`DossierService.findAll`, `ProspectionService.buildSpec`), **périmètre commercial
inclus** (`PerimetreCommercialService.filtreDossiers()` / `filtreProspections()`). Recoder le
filtrage en parallèle ferait diverger les deux au premier filtre ajouté — et, pour le périmètre,
ferait fuiter dans un total des dossiers que l'appelant n'a pas le droit de lire.

---

## 2. Quelles colonnes sont groupables — mesuré, pas supposé

La demande initiale est « ça s'applique à toutes les colonnes ». Mesuré sur `yesRemsProd`, ce
serait une promesse creuse sur une partie d'entre elles :

| Colonne (dossiers, 358 lignes) | Valeurs distinctes | Décision |
|---|---:|---|
| `statut` | 9 | **groupable** |
| `clientNom` | 337 | **groupable** (dégénéré mais c'est ce que fait l'écran légataire) |
| `dateDocument` exacte | 214 | **non** — granularité obligatoire |
| `dateDocument` par mois | 96 | **groupable** via `granularite=MOIS` |
| `prix` | 190 | **non groupable** — agrégeable |

Prospections : 16 588 `DocDate` distinctes pour 17 939 lignes, 13 810 prospects distincts.

**La règle, dérivée du `TypeColonne` que les catalogues portent déjà :**

- `TEXTE`, `BOOLEEN` → groupables.
- `DATE` → groupables **uniquement** avec `granularite` (`JOUR` | `MOIS` | `ANNEE`), défaut `MOIS`.
- `MONTANT`, `NOMBRE` → **non groupables**, mais **agrégeables** (ce sont les bandes `Total :`).
- Les colonnes `*Id` (`clientId`, `commercialId`…) ne sont pas exposées au groupement : leur
  jumelle `*Nom` groupe déjà sur l'identifiant et rend un libellé lisible. Les exposer offrirait
  deux fois le même groupement, dont une version affichant des UUID.

Une colonne hors de cette liste demandée en `groupBy` renvoie **400** avec la liste des colonnes
groupables, comme `ColonneInconnueException` le fait déjà pour `?colonnes=`.

---

## 3. Ce qui manque dans le code actuel

Les catalogues `ColonneDossier` / `ColonneProspection` / `ColonneProspect` sont **la liste blanche
de la sérialisation** : ils portent `nom`, `libelle`, `groupe`, `type`, `parDefaut`. Ils ne portent
**aucun chemin JPA** — le mappeur `toListItem` est positionnel.

Le groupement a besoin de ce chemin. C'est l'ajout central de ce lot :

```java
// ColonneDossier
STATUT("statut", "Statut", Groupe.IDENTITE, TypeColonne.TEXTE, true,
       Chemin.groupable("strStatus.code")),
CLIENT_NOM("client", "Client", Groupe.ACTEURS, TypeColonne.TEXTE, true,
       // clé = l'id ; libellé = TierNomFormatter, qui lit socialReason > description
       // > firstName+lastName — donc les quatre chemins doivent être sélectionnés.
       Chemin.tier("cfgCustomer")),
PRIX("prix", "Prix", Groupe.FINANCIER, TypeColonne.MONTANT, true,
       Chemin.agregeable("price")),
```

`Chemin` est un record du package `domain/` (sans JPA, comme le reste) portant :
`cheminCle`, `cheminsLibelle`, `groupable`, `agregeable`.

**Le `null` reste la valeur par défaut** : une colonne sans `Chemin` déclaré n'est ni groupable ni
agrégeable. Les ~60 colonnes existantes ne sont donc pas à annoter d'un coup — seules celles qu'on
déclare explicitement entrent au périmètre, et le catalogue reste la source de vérité du front.

---

## 4. Les endpoints

### 4.1 Lister les groupes

```
GET /api/dossiers/groupes?groupBy=statut&agregats=prix,totalAPayer
                          &<tous les filtres de GET /api/dossiers>
GET /api/prospections/groupes?groupBy=…
GET /api/pre-prospections/groupes?groupBy=…
```

| Paramètre | Rôle |
|---|---|
| `groupBy` | **requis**, une seule colonne (voir §7 sur l'imbriqué) |
| `agregats` | colonnes `MONTANT`/`NOMBRE` à sommer. Absent → seul l'effectif est renvoyé |
| `granularite` | `JOUR`\|`MOIS`\|`ANNEE`, uniquement si `groupBy` est une `DATE`. Défaut `MOIS` |
| `tri` | `libelle` (défaut, ascendant) \| `nombre` (descendant) |
| *filtres* | strictement ceux de la liste correspondante |

Réponse — `PageDto<GroupeDto>`, l'enveloppe de pagination déjà en place :

```json
{
  "content": [
    { "cle": "9f3c…", "libelle": "Adnane ES-SAIYDY", "nombre": 1,
      "agregats": { "prix": 950000.00, "totalAPayer": 950000.00 } },
    { "cle": null, "libelle": null, "nombre": 4, "agregats": { "prix": 0.00 } }
  ],
  "page": 0, "size": 20, "totalElements": 337, "totalPages": 17
}
```

`cle: null` est le groupe des lignes sans valeur. Il est **renvoyé**, pas masqué : l'écran légataire
l'affiche, et le masquer ferait que la somme des effectifs ne retomberait plus sur le total de la
liste — exactement le genre d'écart silencieux qu'on veut pouvoir tester.

### 4.2 Déplier un groupe

C'est la moitié du lot qu'on peut oublier : sans elle, un groupe s'affiche mais ne s'ouvre jamais.
Les filtres actuels de `GET /api/dossiers` ne couvrent que `statut`, `commercial`, `projet`,
`trancheId`, `champDate`+dates, `search` — grouper par `vocationCode` ou `societeLibelle`
produirait des groupes inouvrables.

```
GET /api/dossiers?groupeColonne=clientNom&groupeValeur=9f3c…&<mêmes filtres>
```

**`groupeValeur` reprend telle quelle la `cle` renvoyée par `/groupes`.** Le front n'a aucune
valeur à construire ni à typer : il réémet ce qu'il a reçu. C'est ce qui évite d'inventer un
langage de filtre générique (`colonne:opérateur:valeur`) et sa coercition de types — un chantier
sans rapport avec le besoin.

`groupeColonne` fourni sans `groupeValeur` → le groupe `IS NULL`.
`groupeValeur` sans `groupeColonne` → **400**.

### 4.3 Référentiel

`GET /api/referentiels/colonnes/{dossiers,prospections,prospects}` gagne deux booléens par
colonne : `groupable`, `agregeable`. Le front ne code aucune liste en dur, exactement comme pour le
sélecteur de colonnes.

---

## 5. Invariant testable

Le test qui compte, et qui doit exister pour chacune des trois listes :

> à filtres identiques, **la somme des `nombre` de tous les groupes est égale au `totalElements` de
> la liste non groupée**, et la somme d'un agrégat sur tous les groupes est égale au total de la
> même colonne sur la liste entière.

S'il casse, c'est que le groupement et la liste ne parlent plus des mêmes lignes — une divergence
de `Specification`, un `INNER JOIN` implicite qui écarte les lignes à association nulle (le piège
déjà rencontré sur le filtre `trancheId` de `DossierService`, voir les commentaires L.212-227), ou
un périmètre commercial appliqué d'un côté seulement.

Couverture minimale par endpoint, conformément à la règle 5 de `CLAUDE.md` :
cas nominal · 400 sur colonne non groupable · 400 sur `groupeValeur` sans `groupeColonne` ·
l'invariant ci-dessus · respect du périmètre commercial.

---

## 6. Impact front

**Aucune rupture.** Tout est additif : `groupBy` absent, les listes répondent exactement comme
aujourd'hui. Les deux booléens ajoutés au référentiel des colonnes sont de nouveaux champs, pas des
champs modifiés.

Le front reste à faire dans `crm-prototype` (hors périmètre de ce dépôt) : bande de groupement,
groupes repliables, bandes `Total :`. Le contrat ci-dessus et `/api-docs` sont le seul handoff.

---

## 7. Hors périmètre, assumé

- **Groupement imbriqué** (plusieurs colonnes empilées, ce que la bande d'OMRAN accepte). Arbitré
  avec l'utilisateur : un seul niveau pour ce lot. `groupBy` restant un paramètre unique, le passer
  en liste plus tard n'invalide pas les intégrations existantes.
- **Agrégats autres que `SUM` et `COUNT`** (min/max/moyenne, disponibles dans les `Σ` du grid
  légataire). La capture ne montre que des sommes.
- **`ColonneProspect`** : `GET /api/prospects` n'est pas dans les trois onglets visés. Le catalogue
  reçoit les booléens du référentiel (pour l'uniformité de la réponse) mais aucun chemin, donc
  aucune colonne groupable.
