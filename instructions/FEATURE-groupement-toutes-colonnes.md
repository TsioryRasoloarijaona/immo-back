# FEATURE — Grouper sur toutes les colonnes, et n'afficher que les valeurs qui existent

Deuxième lot du groupement. Le premier (`FEATURE-groupement-listes.md`) a livré le moteur sur un
sous-ensemble **choisi** de colonnes. Celui-ci lève la restriction et change un défaut.

Les deux demandes, telles qu'elles ont été formulées : *« dans toutes les colonnes on doit pouvoir
faire ça »* et *« juste rendre les lignes qui existent »*.

---

## 1. Ce que ce lot renverse, et pourquoi c'était discutable

Le lot précédent refusait de grouper les montants sur une mesure : `Price` porte 190 valeurs
distinctes sur 358 dossiers, donc le groupement y rend presque autant de groupes que de lignes.
La mesure est juste ; **la conclusion ne l'était pas**.

Un portefeuille filtré sur une tranche peut n'avoir que six prix distincts, et le groupement y est
alors la bonne lecture. Décider à la place du client qu'un groupement « ne sert à rien » revient à
lui retirer une fonction que le logiciel de bureau lui donne — sa bande accepte n'importe quel
en-tête de colonne, sans jugement.

**La règle devient donc : une colonne est groupable dès qu'elle est atteignable à un coût
acceptable.** Ce qui reste exclu l'est sur une mesure de *coût*, plus jamais sur une opinion
d'utilité. Et chaque exclusion porte son chiffre (§4).

---

## 2. `?inclureSansValeur=` — défaut `false`

Le lot précédent renvoyait toujours le groupe des lignes sans valeur, pour que la somme des
effectifs retombe sur le `totalElements` de la liste. C'est un bon invariant de test, c'est un
mauvais défaut d'écran : sur `derniereVisiteDate`, **11 186 prospections sur 14 118 n'ont pas de
valeur** — le groupe vide écrase les 1 108 autres.

```
GET /api/{dossiers,prospections,pre-prospections}/groupes?groupBy=…&inclureSansValeur=false
```

- **`false` (défaut)** — seules les valeurs renseignées font groupe.
- **`true`** — le groupe `cle: null` revient, et l'invariant du §5 du lot précédent redevient
  vérifiable. C'est ce que font les tests d'invariant, explicitement.

Le prédicat porte sur l'**expression de clé**, pas sur la colonne brute : pour une date groupée au
mois la clé est `YEAR()*100 + MONTH()`, nulle exactement quand la date l'est. Tester la colonne
séparément marcherait ici mais divergerait de la requête de comptage, qui compte des clés.

### Impact front

Le front consomme déjà cet endpoint (`src/lib/groupement.ts`) et **gère le groupe nul**
(`LIBELLE_GROUPE_VIDE`, clé React `' vide'`). Rien ne casse : il recevra simplement moins de
groupes, et son traitement du nul devient inerte. S'il veut le récupérer, il passe
`inclureSansValeur=true`.

---

## 3. Couverture atteinte

| Catalogue | Avant | Après |
|---|---:|---:|
| `ColonneDossier` | 28 / 40 | **40 / 40** |
| `ColonneProspection` | 19 / 56 | **44 / 56** |

Quatre mécanismes, du moins cher au plus cher :

**a. Chemin d'entité direct** — les colonnes simplement jamais déclarées (`code`,
`tempDescription`, `prospectCode`, `mobileProspect`…) et **les identifiants** (`clientId`,
`trancheId`, `societeId`…). Ceux-ci étaient refusés parce que leur jumelle `*Nom` donne la même
partition avec un libellé lisible — mais c'est au client de choisir laquelle il affiche.

**b. `Chemin.groupableEtAgregeable`** — les `MONTANT`/`NOMBRE`, désormais clé **et** total. Le
record `Chemin` séparait déjà les deux drapeaux ; seule la fabrique manquait.

**c. `Chemin.repli`** — `telephone` et `email` de la prospection ne sont pas des colonnes mais des
**cascades** : la fiche d'abord, le champ saisi au premier contact ensuite. Mesuré sur les 8 613
prospections ouvertes — 5 034 par la fiche, 7 596 par la saisie, **8 591 par l'une ou l'autre**.
La clé rejoue la cascade en SQL, avec `NULLIF(TRIM(…), '')` à chaque terme : le mappeur Java teste
`StringUtils.hasText`, qui rejette aussi les chaînes blanches, là où un `COALESCE` nu ne neutralise
que `NULL`. Sans ça, une fiche au mobile blanc formerait son propre groupe alors que la ligne
affiche le numéro temporaire.

**d. Reroutage des colonnes `Source.VUE` atteignables** — `projetId/Code/Libelle` et `cin` étaient
étiquetées « vue » **par héritage, pas par nécessité** : la chaîne `stkHierarchy → cfgProject` est
dans le graphe d'entités, et le lot précédent l'empruntait déjà pour `societeCode`. Aucune
machinerie, et le groupement ne touche pas la vue.

**e. `ComProspectionVisites`, entité `@Subselect`** — voir §5.

---

## 4. Ce qui reste exclu, avec son chiffre

| Colonne(s) | Raison | Mesure |
|---|---|---|
| `joursInactivite`, `nbInteractions`, `derniereActionDate`, `derniereCommunicationDate`, `derniereCommunicationResultat` | agrègent **toute** la catégorie « Prospection » de `ComActionMessage` | 46 667 lignes → **~1 s à chaud** |
| `derniereAlerteDate`, `derniereAlerteMessage` | « dernière ligne par partition » (`ROW_NUMBER`), pas un agrégat — forme différente | `OUTER APPLY` mesuré à **0,7–4,7 s** |
| `dossierId`, `dossierCode`, `statutDossier` | `ComFolder → ComProspection` est mappé dans ce sens ; l'inverse exigerait un `@OneToOne` optionnel, non proxifiable → N+1 sur tout le domaine | — |
| `typeProspectionCode`, `typeProspection` | `StrEntityId` est un `UUID` brut, délibérément non associé (voir `ComProspection`) | renseignée sur **4 lignes / 17 939** |

`typeProspectionId` **est** groupable : la colonne brute suffit et donne la même partition.

Ces exclusions rendent **400** avec la liste des colonnes acceptées, comme un nom inconnu — du
point de vue du client, demander une colonne qui n'existe pas et une qui ne se groupe pas sont la
même erreur.

---

## 5. `ComProspectionVisites` — pourquoi une entité de plus

`derniereVisiteDate` était l'exemple qui a déclenché ce lot. Sa valeur vient de
`ComSaleProspectionView`, donc elle n'était pas groupable. Mais **la vue la calcule elle-même**
comme un `MAX(ComActionMessage.DocDate)` filtré sur un type et une catégorie : la donnée n'a jamais
eu besoin de la vue.

Mesuré sur `yesRemsProd`, groupement par jour de dernière visite :

| Route | Temps |
|---|---:|
| `GROUP BY` sur `ComSaleProspectionView` | **5 496 ms** |
| pré-agrégation **large** (toute la catégorie, 6 colonnes d'un coup) | ~1 000 ms |
| pré-agrégation **étroite** (visites seules) — retenue | **6 ms** à chaud |

La leçon vaut pour la suite : **une pré-agrégation large est le mauvais réflexe**. Ne lire que les
4 292 lignes de type « visite » plutôt que les 46 667 de la catégorie vaut deux ordres de grandeur.
C'est aussi pourquoi les quatre colonnes du §4 ne sont pas livrées avec celle-ci : elles exigeraient
leur propre sous-select étroit, avec leur propre mesure.

**L'association est un `@ManyToOne` paresseux, jamais un `@OneToOne`.** La relation est bien un-à-un
(même clé), mais un `@OneToOne` *optionnel* ne se proxifie pas : Hibernate doit interroger la cible
pour savoir si elle existe, donc il émet un select par prospection chargée — un N+1 sur **toutes**
les requêtes du domaine, pas seulement sur le groupement.

### L'écart assumé avec la vue

2 957 prospections ont une visite d'après `ComActionMessage`, 2 932 d'après la vue. Les 25 de
différence sont perdues par les jointures internes de la vue ; **4 d'entre elles sont visibles par
`GET /api/prospections`**. Pour ces quatre, le groupe portera une date alors que la colonne
affichée — servie par la vue — reste nulle.

C'est la vue qui perd la donnée, pas nous. Anomalie légataire préexistante, **signalée plutôt que
recopiée**. La corriger voudrait dire servir aussi la colonne depuis ce sous-select : un autre lot,
qui changerait des valeurs affichées.

---

## 6. Couverture de test

Aux tests du lot précédent s'ajoutent : le groupe sans valeur écarté puis récupéré, un montant
comme clé **et** total, un identifiant, un `code`, la dernière visite (deux visites sur une même
prospection ne font qu'un groupe, à la date de la plus récente), son dépliage, `nbVisites`, le
repli `telephone` **avec un mobile blanc** qui ne doit pas faire groupe, et le projet atteint par
la tranche.

Les deux tests d'invariant passent désormais `inclureSansValeur=true` explicitement — c'est la
condition sous laquelle la somme des effectifs égale le `totalElements` de la liste, et l'écrire
dans le test dit exactement pourquoi.
