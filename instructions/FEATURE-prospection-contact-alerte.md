# FEATURE — Contact et dernière alerte dans la liste des prospections

> Statut : spécifié le 08/09/2026. Porte sur `immo-crm` uniquement.
> **Champs ajoutés à la réponse — voir §6.**

## 1. Pourquoi

`GET /api/prospections?ouvertes=true` renvoie de quoi identifier une prospection, mais pas de
quoi **agir** dessus : ni téléphone, ni email, ni relance en attente. Un commercial qui veut
appeler ses leads doit ouvrir chaque fiche une par une.

## 2. Mesures (`yesRemsProd`, 08/09/2026, 8 613 prospections ouvertes)

### Le contact a deux sources, et aucune ne suffit

| Source | Téléphone | Email |
|---|---:|---:|
| Fiche prospect (`CfgTier`) | 5 034 | 2 499 |
| Premier contact (`TempTel` / `TempEMail`) | 7 596 | 4 558 |
| **L'une ou l'autre** | **8 591 (99,7 %)** | **4 752 (55 %)** |

5 034 et 7 596 pour 8 591 combinés : les deux ensembles se recouvrent largement mais ni l'un ni
l'autre ne couvre seul. **Le repli n'est pas une commodité, c'est la condition pour que le champ
serve à quelque chose.**

### L'alerte ne vient pas d'où on croit

| Source d'alerte | Prospections ouvertes concernées |
|---|---:|
| `ComProspection.AlertValidityDate` / `AlertMessage` | **0** |
| Interaction (`ComActionMessage.AlertValidityDate`) | **3 236** |

La prospection porte ses propres colonnes d'alerte — **elles sont mortes**, pas une ligne sur
8 613. Les relances vivent exclusivement sur les interactions. Mapper la colonne « évidente »
aurait produit un champ toujours nul, et un bug invisible faute de test sur données réelles.

## 3. Contrat

Quatre champs ajoutés à `ProspectionListItemDto`, servis par `GET /api/prospections` et
`GET /api/pre-prospections` :

| Champ | Règle |
|---|---|
| `telephone` | Mobile de la fiche → fixe de la fiche → `TempTel`. `null` si rien. |
| `email` | Email de la fiche → `TempEMail`. `null` si rien (45 % des cas). |
| `derniereAlerteDate` | `AlertValidityDate` de la dernière interaction en portant une. |
| `derniereAlerteMessage` | `AlertMessage` de cette même interaction. |

**R1 — « dernière » = l'interaction la plus récente** (`DocDate DESC`), pas celle dont
l'échéance est la plus lointaine. C'est la dernière décision du commercial qui fait foi : une
relance à 90 jours posée il y a un mois ne prime pas sur une relance à 2 jours posée hier.

**R2 — une seule requête pour toute la page.** Les alertes sont résolues par un
`ROW_NUMBER() OVER (PARTITION BY ComProspectionId)` sur l'ensemble des identifiants de la page.
Les résoudre ligne par ligne ferait 20 requêtes pour une page de 20 — un N+1 invisible sur
trois lignes de test et coûteux sur un écran réel.

**R3 — jamais d'appel avec une page vide** : `IN ()` est une erreur de syntaxe SQL.

**R4 — les champs d'alerte ne sont renseignés que par les listes de prospections.** Les autres
consommateurs de `ProspectionListItemDto` (hotline, sous-ressources d'un prospect) les laissent
nuls : leur ajouter la requête coûterait une requête de plus par écran pour un besoin qui n'a
pas été exprimé. C'est documenté sur le DTO plutôt que caché.

## 4. Refactor : trois copies du même mappeur

`ProspectionListItemDto` était construit **à l'identique, au caractère près, dans trois
services** — `ProspectionService`, `HotlineService`, `ProspectSubResourcesService`. C'est cette
duplication qui les a fait diverger dès l'ajout des quatre champs : la compilation a cassé aux
deux copies.

Les trois sont désormais un seul `ProspectionService.toListItem(...)`, les deux autres y
déléguant. Le mappeur devient public pour cette raison, et pour elle seule.

## 5. Deux pièges rencontrés à l'exécution

**Une requête native n'entre pas dans les espaces de requête d'Hibernate.** Il ne déclenche donc
pas l'auto-flush avant de l'exécuter : une entité `save()` dans un test n'existe pas encore en
base quand la requête part. `saveAndFlush` dans les fixtures. Même piège que celui rencontré sur
les `@Formula` des pièces jointes.

**`/api/prospections` exclut les pré-prospections** (`CfgProspectId` ET `CfgCommercialId` nuls).
Une fixture sans prospect n'est donc jamais renvoyée — et une assertion `doesNotExist` sur une
liste vide **passe trivialement**. Trois tests étaient de faux verts avant qu'un
`content.length() == 1` ne soit exigé au début de chacun. À reproduire dans tout test de liste :
**affirmer d'abord que la ligne est là**.

## 6. Impact front (`crm-prototype` — autre dépôt, autre PR)

> Ce dépôt ne touche pas le frontend. Cette section et les annotations springdoc sont le canal.

**Quatre champs ajoutés, additifs, sans rupture** : `telephone`, `email`,
`derniereAlerteDate`, `derniereAlerteMessage`.

Ce qu'ils permettent, et qui est le but : **appeler un lead depuis la liste**, sans ouvrir sa
fiche. `null` sur `email` est un cas courant (45 %), pas une anomalie à signaler à
l'utilisateur ; `null` sur `telephone` est rare (0,3 %).

`derniereAlerteDate` reste `null` sur les interactions embarquées par la hotline et par les
sous-ressources d'un prospect — voir R4.

## 7. Tests

| Cas | Attendu |
|---|---|
| Fiche renseignée | mobile de la fiche, pas le champ temporaire |
| Fiche sans mobile | repli sur le fixe |
| Fiche muette | repli sur `TempTel` / `TempEMail` |
| Rien nulle part | `null`, sans plantage |
| Aucune interaction | alerte `null` |
| Une interaction avec alerte | date et message remontés |
| **Plusieurs interactions** | la plus récente gagne, même si l'ancienne a une échéance plus lointaine (R1) |
| Interaction sans date d'alerte | ignorée |
| Page vide | pas d'erreur SQL (R3) |
