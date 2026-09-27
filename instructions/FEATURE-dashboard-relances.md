# FEATURE — Tableau de bord : les relances du jour et les nouveaux pré-prospects

Trois besoins exprimés par l'utilisateur, par ordre d'importance décroissante :

1. **les relances à faire aujourd'hui** (prospection *et* pré-prospection) ;
2. **les relances en retard** ;
3. **les nouveaux pré-prospects du jour** ;
4. et une exigence transverse : **une relance doit être rattachée à un commercial**, en vérifiant
   d'abord si la prospection en a déjà un par ses actions.

Tout est mesuré sur `yesRemsProd` (copie, 17 939 prospections) et recoupé sur `yesRems` (base
vivante qu'Omran alimente, 17 679 prospections). **Backend seul** — voir §7.

---

## 1. Où vit réellement une relance

Elle est portée par l'**interaction**, `ComActionMessage`, jamais par la prospection :

| Colonne | Rôle |
|---|---|
| `AlertValidityDate` | l'échéance de la relance |
| `AlertMessage` | le motif saisi par le commercial |
| `WithoutRevival = 1` | la relance est **désarmée** |
| `ComDocumentId IS NOT NULL` | ce n'est pas une relance commerciale mais un **rappel de paiement** d'échéance |

`ComProspection.AlertValidityDate` existe mais est **morte** — 0 ligne. Ne pas la mapper.

Le prédicat d'une relance vivante est donc, mot pour mot :

```sql
AlertValidityDate IS NOT NULL
AND (WithoutRevival IS NULL OR WithoutRevival = 0)
AND ComDocumentId IS NULL
AND ComProspectionId IS NOT NULL
```

`WithoutRevival` est le même drapeau que pose `ComActionMessageRepository.desarmerRelancesDeLaProspection`
à la transformation en vente : une prospection vendue ne relance plus. Le prédicat est cohérent
avec celui relevé dans la trace d'audit du logiciel de bureau.

### Ce que le filtrage retire

| | Lignes |
|---|---:|
| Interactions totales | 48 162 |
| … portant une `AlertValidityDate` | 39 335 (82 %) |
| … **dont désarmées** (`WithoutRevival=1`) | **33 941 (86 %)** |
| … armées | 5 394 |
| … armées, hors rappels d'échéance, sur une prospection | **3 765** |

**86 % des alertes sont désarmées.** Servir `AlertValidityDate IS NOT NULL` sans le reste du
prédicat multiplierait le volume par dix et ressortirait des prospections déjà vendues.

Vérifié aussi : les 3 765 relances armées portent sur **3 765 prospections distinctes** — une
chacune, jamais deux. L'hypothèse « une interaction plus récente périme la relance précédente » a
été testée et **elle est fausse** : 3 701 des 3 765 sont justement portées par la dernière
interaction de leur prospection. Il n'y a donc pas de déduplication à faire, contrairement à
`ComActionMessageRepository.dernieresAlertes` qui, lui, en a besoin parce qu'il ne filtre pas sur
`WithoutRevival`.

---

## 2. L'échéance est une date, pas un instant

Les 3 765 `AlertValidityDate` armées sont **toutes à minuit**. La colonne est un `datetime` mais
son heure ne porte aucune information. Toute comparaison se fait donc sur la date :
`CAST(AlertValidityDate AS date)`, jamais sur l'instant — sinon « aujourd'hui » ne remonterait que
ce qui est daté à 00:00:00 exactement, ce qui est vrai ici par accident et cesserait de l'être dès
la première relance posée par notre API.

---

## 3. Le mur des retards, et la fenêtre qui le rend exploitable

| Échéance | Lignes |
|---|---:|
| À faire **aujourd'hui** | 0 (copie) / **1** (base vivante) |
| **En retard** | **3 710** |
| À venir | 55 |

Le retard, ventilé par ancienneté :

| Retard | Lignes |
|---|---:|
| 1 à 7 jours | **12** |
| 8 à 30 jours | 63 |
| 31 à 365 jours | 1 615 |
| plus d'un an | **2 020** |

**98 % des retards ont plus d'un mois, et 2 020 ont plus d'un an.** Ce n'est pas une file de
travail, c'est un arriéré abandonné. Servi tel quel, l'endpoint afficherait en permanence un
nombre décourageant que personne ne traitera.

> **Décision produit (utilisateur, 15/09/2026) — fenêtre glissante de 30 jours.**
> `echeance=RETARD` ne sert que `today-30 <= AlertValidityDate < today`, soit **75 lignes**
> aujourd'hui. L'arriéré antérieur reste atteignable par les filtres de `GET /api/prospections`
> (`avecRelanceActive`), pas par le tableau de bord.
>
> **Remplacée le 28/09/2026 — voir §3 bis.**

### 3 bis. Les retards « pour ce mois » : les prospections du mois (28/09/2026)

Branche : `feat/dashboard-relances-du-mois`.

**Le constat.** L'utilisateur compare deux écrans pour la même commerciale : la liste des
prospections montre environ 46 alertes en retard, le tableau de bord 81. Appel de la production
(`api.ayaddeveloppement.com`, compte de la commerciale) et rapprochement ligne à ligne :

| Les 81 relances du tableau de bord | Lignes | Dans la liste du mois |
|---|---:|---|
| sur des prospections créées en septembre | 43 | oui — les 43 en retard de la liste, exactement |
| sur des prospections créées en août | 33 | non : la liste est filtrée sur le mois |
| sur des pré-prospections | 5 | non : une liste de prospections les exclut |

Les deux écrans ne comptaient pas la même chose : la liste filtre sur la **date de la
prospection**, le tableau de bord filtrait sur la **date de l'alerte**.

> **Décision produit (utilisateur, 28/09/2026) — les relances en retard pour ce mois.**
> `echeance=RETARD` sert les relances échues (`AlertValidityDate < today`) des prospections
> **créées dans le mois civil en cours** (`ComProspection.DocDate`), **qualifiées** (hors
> pré-prospections, discriminant `StrEntity`) et **ouvertes** (`IsClosed` nul ou 0). Le tableau de
> bord affiche ainsi le même nombre que la liste des prospections filtrée sur le mois.

Chiffres au 28/09/2026 sur la source, pour cette commerciale : 45 avec la nouvelle règle, contre
76 (78 avec les pré-prospections) avec la fenêtre de 30 jours.

Ce que la règle change, et ce qu'elle laisse :

- **plus de borne sur l'ancienneté de l'alerte** : c'est la date de la prospection qui borne. Une
  date d'alerte saisie dans le passé (4 cas réels) reste servie, `joursDeRetard` peut dépasser 30 ;
- **les relances des prospections des mois précédents sortent du tableau de bord** dès le 1er du
  mois — 33 lignes pour cette commerciale. Elles restent dans la liste des prospections en
  élargissant la période. C'est le prix de l'accord entre les deux écrans, assumé ;
- **prospection ouverte** : protection ajoutée au passage. OMRAN laisse poser une alerte sur une
  prospection archivée sans jamais l'éteindre (460 cas nettoyés le 27/09,
  `FEATURE-alerte-admalertdata.md`) ;
- **`AUJOURDHUI` est inchangé** : toute relance armée due aujourd'hui, pré-prospections et
  prospections anciennes comprises. La demande portait sur les retards.

Le rythme normal justifie la tuile « aujourd'hui » : sur les 21 derniers jours peuplés, il échoit
**1 à 11 relances par jour** (médiane ≈ 3). La tuile aura de quoi s'afficher un jour ouvré normal ;
un 0 y signifie « rien à faire », pas une panne.

---

## 4. Le commercial d'une relance

Deux sources possibles, mesurées sur les 3 765 relances armées :

| Source | Couverture |
|---|---:|
| `ComProspection.CfgCommercialId` — l'affectation de la prospection | **3 656 (97,1 %)** |
| `ComActionMessage.CfgEmployeeId` — qui a saisi l'interaction | 2 661 (70,7 %) |

Quand les deux existent (2 660 cas), **elles concordent à 98,8 %** — 31 désaccords seulement.

| Cas | Lignes |
|---|---:|
| Les deux, d'accord | 2 629 |
| Les deux, en désaccord | 31 |
| Commercial de la prospection seul | 996 |
| Employé de l'interaction seul | **1** |
| **Ni l'un ni l'autre** | **108 (2,9 %)** |

**Règle de résolution** — `COALESCE(prospection.CfgCommercialId, interaction.CfgEmployeeId)`.

L'affectation de la prospection prime, et ce n'est pas arbitraire : c'est l'attribution *de droit*,
tandis que l'auteur de l'interaction n'est que celui qui a décroché ce jour-là. Le repli n'ajoute
qu'**un seul** cas sur 3 765 — il est conservé parce qu'il est gratuit et qu'il est la réponse
littérale à la question posée (« vérifier si la prospection est déjà rattachée à un commercial par
une action »), mais il ne faut pas en attendre de volume.

### Les 108 non affectées, et pourquoi les pré-prospections en font toutes partie

Une **pré-prospection se définit** par `CfgProspectId IS NULL AND CfgCommercialId IS NULL` — c'est
le discriminant déjà utilisé par `GET /api/pre-prospections`. Elle n'a donc **jamais** de
commercial, par construction. Et les **21** relances posées sur des pré-prospections n'ont pas non
plus d'employé sur leur interaction : `preprosp_avec_employe = 0`, `preprosp_sans_personne = 21`.

Une relance de pré-prospection est donc **structurellement orpheline**. Ce n'est pas une donnée
manquante à corriger.

> **Décision produit (utilisateur, 15/09/2026) — visibles de tous, marquées.**
> `commercialId`/`commercialNom` à `null` et **`nonAffectee: true`**. Ces 108 lignes sont servies
> **quelle que soit la portée demandée**, y compris `portee=MOI` : elles n'appartiennent à
> personne, les réserver au périmètre de quelqu'un reviendrait à les enterrer — or c'est justement
> le canal des leads injectés automatiquement.

---

## 5. Les nouveaux pré-prospects du jour

Même discriminant que `GET /api/pre-prospections` : `CfgProspectId IS NULL AND CfgCommercialId IS
NULL`, date de création `UserCreateDateTime` ramenée au jour.

**L'arrivée est par rafales, pas en continu.** Sur les 21 derniers jours : 0 la plupart du temps,
puis 11, 20, **34 un dimanche**, 31 un samedi. C'est cohérent avec l'injection automatique des
leads depuis le serveur legacy. Deux conséquences pour le contrat :

- une tuile à **0 est le cas courant** et doit se lire « aucun lead aujourd'hui », jamais « erreur » ;
- **le week-end n'est pas creux** ici, contrairement aux interactions. Ne pas filtrer sur les jours
  ouvrés.

`portee` **ne s'applique pas** à cet endpoint : une pré-prospection n'ayant pas de commercial,
`MOI` n'aurait aucun sens. Seul le périmètre commercial par projet s'applique, comme partout.

---

## 6. Contrat

### `GET /api/dashboard/relances`

| Paramètre | Valeurs | Défaut | Rôle |
|---|---|---|---|
| `echeance` | `AUJOURDHUI` \| `RETARD` | `AUJOURDHUI` | `AUJOURDHUI` : `AlertValidityDate = today`. `RETARD` : `AlertValidityDate < today` **et** prospection qualifiée, ouverte, créée dans le mois civil en cours (§3 bis). |
| `portee` | `MOI` \| `EQUIPE` | `MOI` | `MOI` : les relances dont le commercial résolu est l'appelant. `EQUIPE` : tout son périmètre visible. Les non affectées sont servies dans les deux cas (§4). |
| `page`, `size` | | `0`, `20` | Pagination standard du dépôt. |

Trié par `dateRelance` croissante — le plus ancien retard d'abord — puis par `interactionId`.
Le second critère n'est pas cosmétique : toutes les échéances légataires étant à minuit, les
ex æquo sont nombreux, et sans départage stable une même relance pourrait apparaître sur deux
pages ou sur aucune. Le tri est **imposé par le service**, pas laissé au paramètre `sort`.

`RelanceProspectionDto` :

| Champ | Type | Contenu |
|---|---|---|
| `interactionId` | UUID | `ComActionMessage.PKey` — la relance elle-même, pour la pointer |
| `prospectionId` | UUID | `ComProspectionId` |
| `preProspection` | boolean | vrai si `CfgProspectId` et `CfgCommercialId` sont nuls (§4) |
| `prospectNom` | String | nom du tiers, via `TierNomFormatter.nom` ; `null` sur pré-prospection sans prospect |
| `telephone` | String | pour appeler depuis la tuile sans ouvrir la fiche |
| `dateRelance` | LocalDate | `AlertValidityDate` ramenée au jour (§2) |
| `joursDeRetard` | int | 0 pour `AUJOURDHUI`, ≥ 1 sinon |
| `motif` | String | `AlertMessage` ; `null` fréquent |
| `commercialId` | UUID | résolu par `COALESCE` (§4) ; `null` si aucun |
| `commercialNom` | String | `null` si aucun |
| `nonAffectee` | boolean | vrai pour les 108 (§4) |
| `derniereInteractionDate` | LocalDateTime | `DocDate` de l'interaction porteuse |

### `GET /api/dashboard/nouveaux-pre-prospects`

Pas de `portee` (§5). Pagination identique. Renvoie le compte du jour **et** la liste.

### Périmètre

Les deux endpoints passent par `PerimetreCommercialService` comme le reste. Une prospection dont
le projet ne se résout pas reste comptée pour tout le monde — c'est la règle déjà appliquée par
`GET /api/dashboard`, et la changer ici créerait deux comptages contradictoires sur la même page.

### Tests attendus

Minimum du dépôt (`CLAUDE.md` règle 5) plus, spécifiquement :

- une relance **désarmée** (`WithoutRevival=1`) n'apparaît pas — c'est le filtre qui porte 86 % du volume ;
- une relance **sur échéance** (`ComDocumentId` non nul) n'apparaît pas ;
- `RETARD` (§3 bis) : une prospection créée le **1er du mois à 00:00** est dedans, la **veille à
  23:59** dehors ; une alerte vieille de **45 jours** sur une prospection du mois est servie ; une
  **pré-prospection** et une prospection **fermée** ne le sont pas ; `AUJOURDHUI` continue de
  servir prospections anciennes et pré-prospections ;
- une relance **sans commercial des deux côtés** est servie avec `nonAffectee: true`, *y compris*
  sous `portee=MOI` ;
- une relance dont la prospection et l'interaction **désignent deux commerciaux différents** est
  attribuée à celui de la prospection (les 31 désaccords de §4) ;
- `AlertValidityDate` à 23:59 le jour même compte comme « aujourd'hui » (§2).

---

## 7. Impact front

**Aucune rupture : deux endpoints neufs, aucun existant modifié.** Ce dépôt est backend seul ; la
consommation côté `crm-prototype` n'entre pas dans ce lot et n'est pas à engager ici.

**Changement de sens du 28/09/2026 (§3 bis), sans changement de forme.**
`GET /api/dashboard/relances?echeance=RETARD` garde ses paramètres et son DTO, mais ne renvoie
plus les mêmes lignes :

- le compteur baisse (81 → 45 pour la commerciale mesurée) et égale désormais le nombre d'alertes
  en retard de la liste des prospections filtrée sur le mois en cours ;
- `preProspection` est toujours `false` sous `RETARD` — les pré-prospections n'y sont plus
  servies ; il reste significatif sous `AUJOURDHUI` ;
- `joursDeRetard` n'est plus borné à 30 ;
- tout libellé du type « en retard depuis moins de 30 jours » est à remplacer par « en retard ce
  mois ».

Un point mérite d'être relayé tel quel, parce qu'il est contre-intuitif :

> **`DashboardDto.relancesEnRetard` (existant) ne parle pas des mêmes relances et renvoie toujours
> une liste vide.** Il est construit sur `ComFolder.Revival1`/`Revival2`, qui sont **mortes en base
> réelle — 0 ligne sur 358** (une seule ligne porte `Revival1ValidityDate`). Le champ existe, il est
> documenté, et il est structurellement vide.

Les relances de cette feature sont donc les **seules réelles**. Le champ existant n'est
volontairement **pas** touché ici : le retirer ou le redéfinir serait une rupture du contrat servi
par `/api-docs`, à décider séparément. Signalé, pas corrigé.
