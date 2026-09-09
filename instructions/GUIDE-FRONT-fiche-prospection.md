# Guide d'intégration front — fiche prospection, journal, et sélecteur de colonnes

> Destiné au dépôt `crm-prototype`. Écrit depuis le backend, qui ne touche pas au front : ce
> fichier et `/api-docs` sont le canal. Il décrit **ce qui existe aujourd'hui**, ce qui est
> volontairement absent, et le contrat du sélecteur de colonnes à venir — pour que l'écran soit
> structuré dès maintenant de façon à l'accueillir sans réécriture.
>
> Référence d'implémentation : `instructions/FEATURE-vues-legacy-prospection.md`.

## 0. État réel du backend — à lire avant de planifier

| | Aujourd'hui |
|---|---|
| `GET /api/prospections/{id}` | **enrichi** — nouveau bloc `activite` |
| `GET /api/prospections/{id}/interactions` | **enrichi** — catégorie + projet/tranche |
| `GET /api/prospections` (liste) | **enrichi** — 14 colonnes d'activité, sur demande |
| `?colonnes=` (sélection serveur) | **livré** |
| `GET /api/referentiels/colonnes/prospections` | **livré** — le catalogue du sélecteur |

Tout est en place : le sélecteur de colonnes peut être branché sur le serveur. La §5 donne le
contrat exact.

Pourquoi ce découpage : le journal d'usage du logiciel de bureau (311 923 ouvertures d'écran)
montre que la **fiche** prospection est ouverte **7 372 fois sur 12 mois** contre **270** pour sa
liste — un facteur 27. L'enrichissement est donc parti du détail.

---

## 1. La fiche — ordre d'affichage recommandé

Tous les champs ci-dessous sont sous `activite`, sauf mention contraire. Les pourcentages sont
des taux de population **mesurés sur les 14 118 prospections de vente réelles** : ils indiquent
à quelle fréquence le champ aura une valeur, donc s'il mérite une place fixe ou un affichage
conditionnel.

### Rang 1 — l'en-tête, toujours visible

| Champ | Peuplé | Pourquoi en premier |
|---|---:|---|
| `code`, `prospectNom`, `etape`, `commercialNom` | — | identité, déjà servis |
| **`activite.joursInactivite`** | **100 %** | **Le champ le plus utile de ce lot.** C'est le tri de relance du logiciel de bureau. Toujours présent, donc pas d'affichage conditionnel. |
| `activite.nbInteractions` | 97 % | volume d'échanges, une seule valeur qui résume le fil |

Suggestion : `joursInactivite` en pastille colorée (vert < 30 j, orange < 90 j, rouge au-delà) —
les seuils sont à valider avec le métier, le backend n'en impose aucun.

### Rang 2 — le bloc « relance »

| Champ | Peuplé |
|---|---:|
| `activite.derniereCommunicationDate` | 90 % |
| `activite.derniereCommunicationResultat` | 90 % |
| `activite.derniereCommunicationMessage` | 90 % |
| `activite.derniereActionDate` | 97 % |
| `activite.alerteActiveDate` | 25 % |
| `activite.alerteActiveRaison` | 25 % |

Les deux derniers ne concernent qu'un quart des fiches : bloc entier masqué quand
`alerteActiveDate` est `null`, plutôt qu'une ligne « Aucune relance ».

### Rang 3 — le parcours

| Champ | Peuplé | Note |
|---|---:|---|
| `activite.nbVisites`, `premiereVisiteDate`, `derniereVisiteDate` | 20 % | correspond aux prospections ayant atteint l'étape visite |
| **`activite.dossierId` / `dossierCode` / `statutDossier`** | **2 %** | rare, mais **c'est le fait le plus important quand il est là** : la prospection a été convertie |

`dossierCode` mérite un traitement visuel fort (bandeau, lien cliquable vers
`/dossiers/{dossierId}`) précisément parce qu'il est rare. Voir le piège §2.2.

### Rang 4 — contexte et identité

`activite.projetCode` / `projetLibelle` (**99 %**, la fiche n'exposait que la tranche jusqu'ici),
puis les champs déjà servis (`trancheCode`, `articleCode`, `prix`, `surface`, `avance`,
financement), puis `activite.cin` (99 %).

---

## 2. Trois pièges à ne pas reproduire

### 2.1 `activite` vaut `null` en entier sur une pré-prospection

`GET /api/prospections/{id}` sert **aussi** les pré-prospections (il n'y a pas de route de détail
séparée). Or les indicateurs viennent d'une vue qui **exclut les 3 817 pré-prospections** par
construction.

```ts
if (!prospection.activite) {
  // pré-prospection : afficher « non disponible », JAMAIS « 0 interaction »
}
```

Ce n'est pas une erreur et l'API répond bien **200**. La pré-prospection est le 4ᵉ écran le plus
ouvert du logiciel de bureau (5 420 ouvertures/an) : le cas est fréquent, pas marginal.

### 2.2 `null` ne veut jamais dire « zéro »

`dossierId`, `clientId` et `statutDossier` sont `null` sur **98 %** des prospections. C'est le
**taux de conversion réel** (358 dossiers pour 14 118 prospections), pas une donnée manquante.
Afficher « — » ou masquer la ligne ; ne pas afficher « 0 » ni « Aucun dossier » en rouge.

Même logique pour `nbVisites` : `null` (bloc activité absent) ≠ `0` (aucune visite).

### 2.3 Les chaînes vides sont déjà normalisées — ne pas les re-tester

Cinq colonnes de la base légataire renvoient `""` au lieu de `NULL`. Le backend les normalise en
`null` (verrouillé par un test). Côté front, un simple test de nullité suffit : inutile
d'ajouter `=== ''`.

---

## 3. Le journal d'interactions

`GET /api/prospections/{id}/interactions` — `PageDto<InteractionDto>`. Trois nouveaux champs
utiles, six au total :

| Champ | Peuplé | Note |
|---|---:|---|
| `categorie` | 100 % | `Prospection` (46 666), `Dossier de vente (Réservation)` (1 325), `(Livraison)` (167), `Réclamation` (2) |
| `trancheId` / `trancheCode` / `trancheLibelle` | 96 % | contexte jusqu'ici absent du journal |
| `projetId` / `projetCode` / `projetLibelle` | 96 % | déduit de la tranche |

**Ne pas confondre `categorie` et `resultat`.** `resultat` est la *sous*-catégorie (« Rendez vous
pris », « Hors Budget ») ; `categorie` est le niveau au-dessus et sert de discriminant. Elles ne
sont pas redondantes, contrairement à `description`, qui l'est — la doc OpenAPI d'`InteractionDto`
explique pourquoi il ne faut afficher que `note`.

`categorie` est un bon candidat pour un filtre ou un groupement du fil, puisqu'elle est toujours
renseignée.

---

## 4. La liste — pourquoi elle n'a pas bougé

`GET /api/prospections` renvoie toujours les 20 champs de `ProspectionListItemDto` : `id`,
`code`, `dateDocument`, `tempDescription`, `prospectId`, `prospectNom`, `commercialId`,
`commercialNom`, `origine`, `etape`, `fermee`, `prix`, `surface`, `trancheId`, `trancheCode`,
`trancheDescription`, `telephone`, `email`, `derniereAlerteDate`, `derniereAlerteMessage`.

Filtres serveur inchangés : `etape`, `origine`, `commercial`, `trancheId` (répétables),
`dateDebut`, `dateFin`, `search`, `ouvertes`, plus la pagination `Pageable`.

**14 colonnes d'activité s'y ajoutent désormais**, mais uniquement sur demande via `?colonnes=` :
`joursInactivite`, `nbInteractions`, `derniereActionDate`, `derniereCommunicationDate`,
`derniereCommunicationResultat`, `nbVisites`, `derniereVisiteDate`, `projetId`/`projetCode`/
`projetLibelle`, `cin`, `dossierId`/`dossierCode`/`statutDossier`.

Elles ne sont pas servies par défaut, et ce n'est pas une précaution de performance : la liste
est servie par la table indexée, puis enrichie par lot d'identifiants sur la page déjà chargée —
**7 ms**, qu'on demande 4 colonnes ou 20. C'est simplement le seul moyen de garantir que la
réponse par défaut reste identique à celle d'avant.

> Une version antérieure de ce guide justifiait `?colonnes=` par la pagination profonde
> (5 942 ms → 123 ms). **Cette justification est caduque** : elle valait pour une architecture
> où la liste était servie *depuis* la vue, écartée à la mesure. La sélection économise la charge
> utile et, quand aucune colonne d'activité n'est demandée, une requête — pas des secondes.

---

## 5. Le sélecteur de colonnes — contrat livré

> ✅ **Implémenté.** Voir `FEATURE-selection-colonnes-listes.md` pour les mesures et les choix
> d'implémentation.

### 5.1 La requête

```
GET /api/prospections?colonnes=code,prospectNom,joursInactivite,nbInteractions&page=0&size=25
```

- **`colonnes` absent ⇒ comportement actuel à l'identique.** Aucune intégration existante ne
  casse ; c'est la règle qui rend le lot additif.
- Liste blanche stricte côté serveur. Un nom inconnu renvoie **400** avec la liste des noms
  valides dans le `ProblemDetail` — pas un 500, pas un silence.
- L'ordre du paramètre ne porte pas l'ordre d'affichage : c'est au front de le gérer. Le serveur
  renvoie un objet JSON, pas un tableau positionnel.
- `id` est toujours renvoyé, qu'il soit demandé ou non — sans lui la ligne n'est pas cliquable.
- Le paramètre est **répétable ou séparé par des virgules**, indifféremment.
- **Le tri sur une colonne d'activité n'est pas supporté** : `?sort=joursInactivite` renvoie 400.
  Le tri est appliqué sur la table, qui ne porte pas ces colonnes. Ne pas proposer d'en-tête
  cliquable sur les colonnes du groupe `ACTIVITE` ou `CONVERSION`.

### 5.2 La réponse

Le `PageDto` ne change pas de forme (`content`, `page`, `size`, `totalElements`, `totalPages`).
Seules les **clés non demandées sont absentes** de chaque élément de `content` — le DTO reste
entièrement typé et documenté dans `/api-docs`.

Conséquence pour `types.ts` : les champs de `ProspectionListItemDto` deviennent **optionnels**.
C'est le seul changement de typage à prévoir, et il peut être fait dès maintenant — un champ
optionnel toujours présent ne gêne pas.

### 5.3 Le référentiel des colonnes

Pour que le sélecteur ne code pas en dur une liste qui dérivera :

```
GET /api/referentiels/colonnes/prospections
→ [ { nom, libelle, groupe, type, parDefaut } ]
```

`groupe` vaut `IDENTITE` | `CONTACT` | `CONTEXTE` | `FINANCIER` | `ACTIVITE` | `CONVERSION` et
permet de présenter le sélecteur par sections. `parDefaut` donne la sélection initiale — l'union
des colonnes `parDefaut` reproduit exactement la réponse sans paramètre. `type` vaut `TEXTE` |
`NOMBRE` | `MONTANT` | `DATE` | `BOOLEEN` et permet d'aligner et de formater sans table de
correspondance côté front.

C'est **la même source que la liste blanche** qui valide `?colonnes=` côté serveur : les deux ne
peuvent pas diverger, et une colonne ajoutée plus tard apparaît dans le sélecteur sans
redéploiement du front.

### 5.4 Recommandations d'intégration

1. **Construire l'UI du sélecteur à partir du référentiel**, jamais d'une liste codée en dur.
2. **Mémoriser le choix en `localStorage`.** Aucune table légataire ne stocke de disposition de
   colonnes — vérifié, le logiciel de bureau les garde sur le poste client. Une table applicative
   côté backend n'a de sens que si le métier veut retrouver son affichage d'un poste à l'autre :
   c'est une décision distincte, à demander via `from_front.md`.
3. **Ne pas** filtrer côté client après avoir chargé un gros lot. Le contournement du même genre
   existe déjà pour le filtre `statut` des dossiers (demande #10 de `from_front.md`, lot de 500
   dossiers filtré côté client) et il ne passe pas l'échelle. Ici il serait pire : c'est
   justement le transfert de toutes les colonnes qu'on cherche à éviter.

---

## 6. Ce qu'il ne faut *pas* afficher

Mesuré vide ou quasi vide sur la base réelle — ces champs ne sont pas exposés par l'API, et il ne
faut pas les redemander sans nouvelle mesure :

| Champ légataire | Peuplé |
|---|---:|
| `Categories`, `Types`, `Cities` (agrégats du desktop) | **0 %** |
| Budget, Utilisation, Mode de paiement, Intermédiaire | 0 % |
| Géolocalisation (`MapLatitude` / `MapLongitude`) | 0 % |
| `Delai` | 0,8 % |
| Rang de l'interaction (`ActionRank`) | écarté : 30 s de calcul pour une ligne |

Leur nom est prometteur, leur contenu ne l'est pas. C'est la raison d'être de la colonne
« Peuplé » de ce guide : **demander une colonne vide coûte le même travail qu'une colonne utile.**

---

## 7. Limite connue, côté données

Une prospection créée par `POST /api/prospections` n'a pas d'entité structurelle
(`StrEntityId`), et se retrouve donc **invisible dans le logiciel de bureau** — et sans bloc
`activite` dans l'API. 4 lignes sont dans ce cas en base réelle. Le correctif relève d'une
décision métier et fait l'objet d'un lot séparé (`FEATURE-vues-legacy-prospection.md` §8).

En attendant : une fiche fraîchement créée par l'application peut légitimement afficher
`activite: null`. Le traitement du §2.1 couvre déjà ce cas.
