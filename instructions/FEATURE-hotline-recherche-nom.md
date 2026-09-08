# FEATURE — Recherche hotline par nom

> Statut : spécifié le 08/09/2026. Porte sur `immo-crm` uniquement.
> **Contient des champs ajoutés à la réponse — voir §7.**

## 1. Pourquoi

`GET /api/hotline/recherche?telephone=…` ne sait chercher que par numéro. Un agent qui reçoit
un appel depuis un numéro masqué, ou à qui l'interlocuteur donne son nom avant son numéro, n'a
aucun moyen de retrouver la fiche sans quitter l'écran.

## 2. Mesures (`yesRemsProd`, 08/09/2026)

| Mesure | Valeur |
|---|---:|
| `CfgTier` total | 14 514 |
| `CfgTier.Description` renseignée | **14 514 (100 %)**, format « Ines ALAMI » |
| `FirstName` / `LastName` renseignés | 14 091 (97 %) |
| Pré-prospections sans fiche | 3 574 |
| dont `TempDescription` renseignée | **3 569 (99,9 %)** |
| dont `TempTel` renseigné | 3 556 |

`TempDescription` est le pendant exact de `TempTel` et il est **mieux rempli que lui** : la
recherche par nom couvre donc le bloc « appels sans fiche » au moins aussi bien que celle par
téléphone. La symétrie est justifiée par les chiffres, pas par principe.

### Le problème de volumétrie, mesuré

| Terme | Fiches correspondantes |
|---|---:|
| `MOHAMED` | **555** |
| `ALAMI` | 86 |

Un numéro identifie une personne ; un nom en identifie des centaines. Or **chaque fiche coûte
cinq requêtes** (prospections, dossiers, règlements, interactions, historique), toutes
plafonnées à 50 lignes. Une recherche sur « Mohamed » déclencherait donc ~2 775 requêtes.

Le code actuel n'a aucune protection : `tiers.stream().map(t -> toFiche(t.getId()))` construit
tout ce qui remonte. C'est acceptable pour un téléphone, jamais pour un nom.

### La collation, qui change le SQL

`yesRemsProd` est en **`French_CI_AS`** : insensible à la casse, **sensible aux accents**. Les
données contiennent de vrais noms accentués (`Héjér F'H` dans l'échantillon). Sans intervention,
taper « Hejer » ne trouverait rien — un défaut invisible en test avec des noms ASCII, et
systématique en production.

**Chaque comparaison de nom porte donc un `COLLATE French_CI_AI` explicite.** Conséquence
technique : ces requêtes sont **natives**, JPQL ne connaissant pas `COLLATE`.

## 3. Contrat

```
GET /api/hotline/recherche?telephone={numéro}     (inchangé)
GET /api/hotline/recherche?nom={texte}            (nouveau)
```

**R1 — exactement un critère.** Aucun des deux, ou les deux à la fois : `400`. Accepter les
deux imposerait de décider lequel prime, et cette décision serait invisible pour l'agent.

**R2 — la forme de la réponse ne change pas.** `HotlineResultDto` conserve `fiches`,
`appelsSansFiche` et `connu`, avec exactement la même structure. Le front garde son écran ; seul
le champ de saisie change.

**R3 — plafond de 20 fiches complètes**, avec `tronque = true` quand il y a davantage de
correspondances. Le coût est ainsi borné à 100 requêtes au pire. La recherche par téléphone
n'est pas plafonnée : elle ne remonte que 0 à 2 fiches (un couple peut partager un fixe), et lui
imposer un plafond changerait un comportement qui n'a jamais posé problème.

**R4 — sous-chaîne, insensible à la casse et aux accents.** « alami », « ALAMI » et « Alámi »
trouvent la même chose. Recherche sur `Description`, `FirstName` et `LastName` — les trois, pour
que « ALAMI Ines » fonctionne aussi bien que « Ines ALAMI », l'ordre des mots dans
`Description` n'étant pas garanti.

**R5 — deux caractères minimum.** En dessous, `400` : une lettre seule ramènerait la moitié de
la base pour n'afficher que le plafond, en payant le coût complet de la requête.

**R6 — parité de périmètre commercial.** La recherche par nom applique exactement les mêmes
restrictions que celle par téléphone, **dans la requête** et non après coup — y compris la
variante « périmètre » sur les pré-prospections. Sans cela, la nouvelle route deviendrait un
contournement du périmètre : un commercial restreint verrait par le nom ce que le numéro lui
cache.

## 4. Ce qui est refactorisé

Les deux recherches ne diffèrent que par **ce qu'elles trouvent** ; tout ce qui suit —
construction des fiches, exclusion des doublons, application du périmètre, assemblage — est
identique. Le service est donc scindé :

| Étape | Téléphone | Nom |
|---|---|---|
| 1. trouver les `CfgTier` | `findByNormalizedPhone` | `rechercherParNom` (plafonné) |
| 2. trouver les pré-prospections | `findByNormalizedTempTel[DansPerimetre]` | `rechercherPreProspectionsParNom[DansPerimetre]` |
| 3. **assembler** | commun | commun |

L'étape 3 était jusqu'ici mêlée à l'étape 1 dans `rechercherParTelephone`. Elle devient
`assembler(...)`, appelée par les deux chemins — c'est ce qui garantit que la recherche par nom
hérite sans effort des règles déjà éprouvées, notamment l'exclusion des pré-prospections déjà
rattachées à une fiche remontée.

## 5. Détail des correspondances

**Fiches** — `CfgTier.Description` OU `FirstName` OU `LastName` contient le terme.

**Appels sans fiche** — `ComProspection.TempDescription` contient le terme, et la prospection
n'est pas déjà rattachée à une fiche remontée (règle existante, conservée telle quelle).

## 6. Hors périmètre

- La recherche par email ou par code de dossier : le besoin exprimé porte sur le nom.
- Le classement par pertinence : les résultats sortent triés par `Description`, sans score.
- La recherche phonétique (« Alami » / « Allami ») : `COLLATE` traite les accents, pas les
  variantes orthographiques.

## 7. Impact front (`crm-prototype` — autre dépôt, autre PR)

> Ce dépôt ne touche pas le frontend. Cette section et les annotations springdoc sont le canal.

**Nouveau mode de recherche, additif** — `GET /api/hotline/recherche?nom=…`. La réponse a la
même forme qu'avec `telephone`, l'écran existant peut l'afficher sans modification.

**Trois champs ajoutés à `HotlineResultDto`** (additifs, sans rupture) :

| Champ | Type | Sens |
|---|---|---|
| `critere` | `string` | `TELEPHONE` ou `NOM` — quel critère a produit ce résultat. |
| `nomRecherche` | `string` | Le terme saisi, `null` pour une recherche par téléphone. |
| `tronque` | `boolean` | `true` quand plus de 20 fiches correspondent et que la liste a été coupée. **À afficher** : l'agent doit savoir qu'il doit préciser sa recherche, sinon il croira que la personne n'existe pas. |

`telephoneRecherche` et `telephoneNormalise` valent `null` pour une recherche par nom — ils
restent renseignés à l'identique pour une recherche par téléphone.

**Nouveaux cas de `400`** : aucun critère, les deux critères à la fois, ou un `nom` de moins de
deux caractères.

## 8. Tests

| Cas | Attendu |
|---|---|
| Recherche par nom, fiche existante | la fiche remonte, `critere=NOM`, `connu=true` |
| Casse différente | même résultat |
| **Accent différent** (`Hejer` → `Héjér`) | même résultat — c'est le test qui prouve le `COLLATE` |
| Nom présent seulement dans `LastName` | trouvé |
| Pré-prospection sans fiche, par nom | remonte dans `appelsSansFiche` |
| Plus de 20 correspondances | 20 fiches, `tronque=true` |
| Moins de 20 | `tronque=false` |
| Aucun critère · les deux · nom d'un caractère | `400` |
| Recherche par téléphone | inchangée, `critere=TELEPHONE`, `tronque=false` |
| Périmètre restreint (R6) | même filtrage que par téléphone |
