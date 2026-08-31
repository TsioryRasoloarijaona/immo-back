# FEATURE — Périmètre commercial (affectation de projets)

**Branche** : `feat/perimetre-commercial` (les deux dépôts)
**Statut** : spécifié, à implémenter
**Rédigé par** : session manager, à partir d'une investigation `data-explorer` sur `yesRems` (toutes les
mesures ci-dessous sont réelles, requêtées le 2026-08-31 — elles ne sont pas des estimations).

---

## 1. Besoin

Depuis le panneau d'administration, un administrateur affecte un **commercial** à un ou plusieurs
**projets**. Un utilisateur connecté ne voit alors que les **pré-prospections**, **prospections** et
**dossiers** rattachés à ses projets affectés. Le filtrage est **appliqué côté serveur** — le front ne
fait que refléter ce que l'API renvoie.

---

## 2. Découverte déterminante : la table existe déjà

**Ne créez pas de nouvelle table d'affectation.** Le schéma légataire modélise déjà exactement ce
besoin dans `AdmUserHabilitation` (base table, 1071 lignes, 66 utilisateurs distincts sur 75).

```
AdmUserHabilitation
  PKey           uniqueidentifier  PK
  AdmUserId      uniqueidentifier  FK -> AdmUser.PKey
  CfgCompanyId   uniqueidentifier  FK -> CfgCompany.PKey   (nullable)
  CfgProjectId   uniqueidentifier  FK -> CfgProject.PKey   (nullable)
  CfgTrancheId   uniqueidentifier  FK -> CfgTranche.PKey   (nullable)
  PrjProjectId   uniqueidentifier  FK -> PrjProject.PKey   (nullable)
```

La table est **polymorphe : une portée par ligne** (98,6 % des lignes n'ont qu'une seule des quatre
colonnes renseignée). Répartition réelle : 346 lignes tranche, 302 lignes `PrjProject`, 237 lignes
société, 196 lignes projet.

Les données existantes sont **déjà cohérentes avec l'usage visé** : pour 99,50 % des prospections
portant un commercial, ce commercial est déjà habilité sur le projet de la prospection (14 642 / 14 715).
Idem 95,74 % des dossiers (360 / 376). Le périmètre projet ne masque donc **pas** une part
significative du pipeline — c'est le risque principal qui avait été soulevé, il est écarté par la mesure.

Aucune entité JPA ne mappe cette table aujourd'hui (`grep` sur `src/main/java` : 0 occurrence). Elle
doit être mappée **sur la table existante**, jamais recréée.

### 2.1 Grain retenu : projet, pas tranche

`AdmUserHabilitation` sait descendre à la tranche. **Cette feature s'arrête au projet**, conformément à
la demande. Conséquence à assumer et à ne pas présenter comme une équivalence :

- Les 351 lignes tranche ont **toutes** (351/351, 100 %) leur projet parent également habilité — donc
  filtrer sur les seules lignes `CfgProjectId` ne prive personne d'un accès que le légataire accordait.
- **Mais** 81 des 196 habilitations projet (41,3 %) ne couvrent qu'une partie des tranches du projet.
  Le filtrage projet est donc **plus permissif** que le légataire : il donne accès à des tranches que
  l'ancien système refusait. C'est une simplification acceptée, à documenter, pas un bug.

---

## 3. Prérequis bloquant — DDL sur `yesRemsTest`

**`AdmUserHabilitation` n'existe pas dans `yesRemsTest`** (vérifié : `Msg 208, Nom d'objet
'AdmUserHabilitation' non valide`). Dès qu'une entité JPA mappera cette table, **toute la suite
`@SpringBootTest` échouera au démarrage du contexte**, et l'erreur ressemblera à un bug de mapping.

**Première action de l'agent `backend`, avant toute écriture de code** :

1. Relire la définition réelle sur `yesRems` :
   ```
   sqlcmd -S localhost,49687 -U tsix -P securePassword123 -d yesRems -C -h -1 \
     -Q "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='AdmUserHabilitation'"
   ```
2. Créer la table à l'identique sur `yesRemsTest` (qui a les droits DDL). Les FK ne sont pas
   obligatoires sur la base de test ; les colonnes et leur nullabilité, si.
3. Vérifier que la suite complète repart au vert **avant** d'ajouter la logique de filtrage.

---

## 4. Règles de résolution projet ← entité

Chaînes **mesurées**, à respecter littéralement.

### Dossier (`ComFolder`) — 382/383 résolvent (99,74 %)
```
ComFolder.StkItemId -> StkItem.PKey
StkItem.StkHierarchyId -> CfgTranche.PKey
CfgTranche.CfgProjectId -> CfgProject.PKey
```

> **Piège 1** — `StkItem.CfgProjectId` existe et est **mort : 0 ligne sur 1068 (0,00 %)**. C'est le
> raccourci évident et il est vide. Le projet passe obligatoirement par la tranche.
>
> **Piège 2** — la colonne `StkHierarchyId` est une **FK vers `CfgTranche`**, malgré son nom. Il
> n'existe aucune table `StkHierarchy` dans la base (`StkHierarchyPath`/`StkHierarchyPlan` sont sans
> rapport). Confirmé via `sys.foreign_keys`.

### Prospection et pré-prospection (`ComProspection`) — 17 559/17 678 résolvent (99,33 %)
```
ComProspection.StkHierarchyId -> CfgTranche.PKey -> CfgTranche.CfgProjectId
```
Chemin **direct**, sans passer par `StkItem`. Ne pas implémenter de repli via `StkItemId` : cette
colonne n'est renseignée que sur 815 lignes (4,61 %) et n'apporte **0 ligne supplémentaire** par
rapport au chemin tranche (mesuré).

### Encaissement (`StlSettlement`)
Via la ligne de règlement, avec le motif de sous-requête `exists` déjà employé par le filtre
`dossierId` de `EncaissementService` :
```
StlSettlementLine.comDocument.comFolder -> (chaîne dossier ci-dessus)
```

---

## 5. Pré-prospection : contrainte structurante

**Il n'existe aucune colonne `IsPreProspection` dans `yesRems`.** (La seule occurrence est sur
`ComProspectionToUpdateCfgCommercialON20210811`, une table de sauvegarde morte de 2021 — à ignorer.)
Le discriminant réellement utilisé par le code est l'heuristique déjà présente dans
`ComProspectionRepository#countExcludingPreProspections` :

> pré-prospection ⇔ `CfgProspectId IS NULL AND CfgCommercialId IS NULL`

Conséquence à ne pas contourner : **une pré-prospection a, par construction, un commercial NULL**
(0 sur 2800, 0,00 %). Toute règle du type « le commercial voit ses propres pré-prospections » renvoie
l'ensemble vide. Les pré-prospections sont donc filtrables **uniquement par projet** — ce que fait
cette feature (2708/2800 = 96,71 % résolvent un projet).

Volumes : 14 878 prospections (84,16 %), 2 800 pré-prospections (15,84 %).

---

## 6. Règles de périmètre

### 6.1 Résolution du périmètre d'un utilisateur

```
perimetre(admUserId) =
    { h.CfgProjectId | h ∈ AdmUserHabilitation, h.AdmUserId = admUserId, h.CfgProjectId IS NOT NULL }
```

- La jointure se fait sur **`AdmUserId`**, jamais via `CfgEmployeeId`/`CfgTier`.
  > `AdmUser.CfgEmployeeId` n'est **pas unique** — 59 renseignés pour 58 valeurs distinctes, un tier
  > (`1EFF061F-147C-45A5-931A-9F9F9DBF50B9`) porté par 2 utilisateurs (`h.atchany` et `mma`). Une
  > jointure naïve par tier duplique les lignes (constaté : 15 812 prospections retournées pour 14 715
  > réelles). Le principal `AuthenticatedUser` expose déjà `admUserId` — utilisez-le.
- Les lignes `CfgCompanyId` / `CfgTrancheId` / `PrjProjectId` sont **lues mais ignorées** par le filtre
  de cette feature. Elles ne doivent en aucun cas être supprimées ou modifiées (voir §7.2).
- **Résolu par requête, jamais depuis le JWT.** Un token vit au travers d'une modification
  d'affectation par l'admin ; l'y embarquer servirait un périmètre périmé. Pas de nouveau claim.

### 6.2 Qui échappe au filtre

Périmètre **non appliqué** si l'utilisateur porte l'un de ces rôles :

| Code `AdmRole` | Utilisateurs |
|---|---|
| `Administrateur` | 20 |
| `Direction Générale` | 2 |

- Les rôles proviennent de `AdmUserRole` (m2m) — c'est la source que `AuthService` utilise déjà pour
  peupler le JWT. **Ne pas** lire `AdmUser.WebRoleId` : les deux mécanismes coexistent et **divergent**
  (21 lignes seulement, ex. `Commercial` 42 vs 17).
- Comparaison sur le **code exact**, jamais un `LIKE 'Commercial%'` — cela capturerait `Commercial Chef`
  et `Commercial Aide`. Les codes de rôle sont de la saisie libre et contiennent du bruit réel
  (`majid morabit` est un nom de personne utilisé comme rôle, `Niveua remise 2` est une coquille).
- `Commercial Chef` (13 utilisateurs) n'est **pas** exempté : il n'existe aucune table d'équipe ou de
  hiérarchie dans le schéma permettant de résoudre « voit son équipe ». Un chef reçoit simplement
  plusieurs projets, ce que le modèle couvre déjà.
- Réutiliser `@PreAuthorize("hasRole('Administrateur')")`, motif déjà en place dans
  `UtilisateurAdminController`.

### 6.3 Utilisateur sans affectation → aucun filtre

**Décision produit (validée) : un périmètre vide signifie « pas de restriction ».** Le filtre ne
s'active que lorsqu'un administrateur a affecté au moins un projet.

Motif : déploiement opt-in, compte par compte, sans rien casser le jour de la mise en production.
Cela résout aussi un problème de test réel — sur les 5 comptes disposant d'un `AppUserCredential`,
3 (`t.nahoma`, `tsiory`, `z.amed`) n'ont **aucune** ligne d'habilitation, et le seul compte
crédencé réellement habilité (`a.mestour`) est `Disabled = 1`. Avec une règle « deny by default »,
la recette se serait faite sur des écrans vides.

### 6.4 Lignes sans projet résoluble → visibles de tous

**Décision manager, à confirmer si vous n'êtes pas d'accord.** Une ligne dont le projet ne se résout
pas n'est masquée à personne.

Volumes concernés, tous marginaux : 1 dossier (0,26 %), 27 prospections (0,18 %), 92 pré-prospections
(3,29 %). Motif : une pré-prospection sans projet n'appartient à personne ; la masquer signifie que
plus aucun commercial ne la traite — on perdrait des leads pour protéger une donnée qui n'est
rattachée à aucun périmètre. Idem pour un encaissement sans ligne rattachée à un dossier.

En SQL, cela veut dire que le prédicat de périmètre doit être `projet ∈ perimetre OR projet IS NULL`,
et **non** `projet ∈ perimetre` — une jointure interne masquerait silencieusement ces lignes.

### 6.5 Surface d'application

Appliquer le périmètre à :

| Domaine | Endpoints |
|---|---|
| Prospections | liste, **détail** |
| Pré-prospections | liste, **détail** |
| Dossiers | liste, **détail** |
| Encaissements | liste, **détail** |
| Dashboard | tous les agrégats (KPIs, compteurs, graphiques) |

- **Le détail par id est obligatoire**, pas optionnel : sans lui, le périmètre se contourne en
  énumérant les ids. Une ressource hors périmètre renvoie **404** (et non 403) — un 403 confirmerait
  l'existence de la ressource.
- Tout endpoint imbriqué qui charge un dossier par id (échéancier, règlements, pièces jointes,
  générateurs de documents PDF) hérite de la même vérification. Centralisez : un point de contrôle
  unique sur le chargement de `ComFolder` par id, pas une vérification recopiée dans chaque méthode.
- **Écritures** : la création/modification d'une prospection ou d'un dossier sur un projet hors
  périmètre est refusée (**403**, ici la ressource est nommée par l'appelant, il n'y a rien à cacher).
  Sans cela, la frontière est décorative.
- **Hors périmètre de cette feature, ne pas toucher** : patrimoine (projets/tranches/articles),
  calendrier, hotline, clients, prospects. Décision produit explicite.

### 6.6 Le tableau de bord est le point de fuite le plus probable

`DashboardService` n'utilise **pas** de `Specification` — contrairement à `DossierService`,
`ProspectionService` et `EncaissementService`, où le prédicat de périmètre s'ajoute par un `and` sur
le `Specification` existant. Ses agrégats doivent être filtrés explicitement, un par un. Un KPI qui
resterait global afficherait à un commercial restreint un chiffre d'affaires qu'il ne peut pas
détailler — soit exactement la donnée que la feature prétend masquer.

---

## 7. API

### 7.1 Consultation du périmètre courant

`UtilisateurCourantDto` (`POST /api/auth/connexion`, `GET /api/mon-compte`) gagne deux champs :

| Champ | Type | Description |
|---|---|---|
| `perimetreRestreint` | `boolean` | `true` si le filtre s'applique réellement à ce compte (au moins un projet affecté **et** aucun rôle d'exemption). |
| `projetsAutorises` | `List<ProjetPerimetreDto>` | Projets du périmètre — `{ id, code, libelle }`. Vide si `perimetreRestreint = false`. |

Sans cela, un commercial restreint ne peut pas distinguer « ce projet ne me concerne pas » de
« l'application a perdu mes données ». Le front s'en sert pour afficher un bandeau explicite.

### 7.2 Administration des affectations

Base `/api/utilisateurs` (contrôleur existant `UtilisateurAdminController`), **`Administrateur`
uniquement** :

- `GET /api/utilisateurs/{admUserId}/projets` → `List<ProjetPerimetreDto>` — projets actuellement
  affectés. 404 si l'utilisateur n'existe pas.
- `PUT /api/utilisateurs/{admUserId}/projets` — corps `{ "projetIds": [uuid, ...] }`.
  **Remplacement intégral** du jeu d'affectations projet de cet utilisateur (adapté à une UI de cases
  à cocher : le front envoie l'état final, pas un delta). Liste vide = plus aucune restriction (§6.3).
  400 si un `projetId` est inconnu.

> **Contrainte impérative** : le `PUT` ne supprime et ne recrée **que les lignes dont `CfgProjectId`
> est non nul** pour cet utilisateur. Les lignes société (237), tranche (346) et `PrjProject` (302)
> appartiennent à l'application légataire de bureau, qui reste en service — les effacer casserait les
> habilitations de l'ancien système. C'est le point le plus dangereux de cette feature.

- `UtilisateurListItemDto` gagne `nombreProjets` (`int`), pour afficher l'état d'affectation
  directement dans la liste d'administration.

### 7.3 Sélecteur de projets

Réutiliser `GET /api/projets` (contrôleur `ProjetController` existant). **8 projets, 16 tranches** en
tout : une liste de cases à cocher suffit, ni recherche ni pagination.

```
APF   AYAD PRIVATE FERME BRETONE   2 tranches
APO   AYAD PRIVATE OASIS           1
ARS   AYAD RESIDENCE BEL AIR       3     (9754 prospections, 84 dossiers)
ART   AYAD RESIDENCE TRIBECA       1     (3731 / 32)
CITEB CITE BENACEUR                1
LAZ   VILLA LAZRAK                 1
MBR   MANSOURIA BEACH RESORT       6     (2231 / 195)
YES   YESTEST                      1     (67 / 16 — projet de test présent en production)
```

Il n'existe **aucune colonne de statut/actif sur `CfgProject`** (liste complète des colonnes
vérifiée) ; `MarketingStartDate`/`EndDate` sont NULL sur 6 des 8. Ne tentez pas de filtrer les projets
« actifs » : les 8 sont sélectionnables.

Le sélecteur d'**utilisateur** en revanche doit filtrer sur `AdmUser.Disabled` — **60 des 75 comptes
sont désactivés (80 %)**, seuls 15 sont actifs. Sans ce filtre, l'écran d'affectation propose 60
comptes morts.

---

## 8. Tests attendus (agent `backend`, puis durcis par `backend-verifier`)

1. La table est créée sur `yesRemsTest` et la suite complète repart au vert **avant** toute logique.
2. Commercial affecté au projet A : la liste des dossiers/prospections/pré-prospections ne renvoie que
   ceux du projet A ; le détail d'une ressource du projet B renvoie **404**.
3. Commercial **sans** affectation : résultats strictement identiques à ceux d'avant la feature
   (non-régression du chemin par défaut — c'est ce qui protège les comptes existants).
4. `Administrateur` et `Direction Générale` affectés à un seul projet : voient quand même tout.
5. Une ligne sans projet résoluble reste visible pour un commercial restreint (§6.4).
6. `PUT .../projets` : les lignes `CfgTrancheId`/`CfgCompanyId`/`PrjProjectId` préexistantes de
   l'utilisateur sont **toujours présentes** après l'appel. Test non négociable.
7. `PUT`/`GET .../projets` renvoient 403 pour un compte non administrateur.
8. Le tableau de bord : les agrégats d'un commercial restreint correspondent au sous-ensemble de ses
   projets, et non au total global.
9. Création d'un dossier sur un projet hors périmètre → 403.

**Attention `@Transactional`** : les fixtures se construisent via les vrais repositories. Les tests de
périmètre doivent créer leurs propres lignes `AdmUserHabilitation` — les données réelles de `yesRems`
ne sont pas présentes dans `yesRemsTest`.

---

## 9. Front (`crm-prototype`)

### 9.1 Écran d'administration
Section « Projets affectés » dans l'espace administration (`pages/utilisateurs/`, déjà protégé par
`AdminGuard`). Il n'existe pas encore de page de détail utilisateur : le plus simple est une **boîte de
dialogue depuis la ligne de la liste** (8 cases à cocher, enregistrement en un `PUT`). La liste affiche
`nombreProjets` par utilisateur.

### 9.2 Bandeau de périmètre
Lorsque `perimetreRestreint = true`, les listes prospections / pré-prospections / dossiers /
encaissements affichent un indicateur discret mais explicite du type
« Périmètre limité à : ARS, MBR ». Objectif : qu'un commercial ne confonde jamais restriction et perte
de données. Réutiliser les primitives partagées existantes plutôt que d'inventer un composant.

### 9.3 Rappel de découpage
Le filtrage n'est **pas** implémenté côté front : aucune liste ne doit filtrer par projet elle-même.
Le front affiche ce que l'API renvoie. Toute logique de périmètre côté client serait à la fois
redondante et contournable.

---

## 9bis. RÉVISION après passe adverse (`backend-verifier`)

La première implémentation a été cassée par 12 tests adverses
(`src/test/java/.../controller/perimetre/PerimetreCommercialAdversarialTest.java`). **Ces tests font
foi : ils décrivent le comportement attendu d'une frontière correcte. Ils doivent passer au vert par
correction de `src/main/`, jamais être assouplis.** Les points suivants amendent les sections
ci-dessus.

### 9bis.1 Le périmètre ne se lit plus dans l'URL brute (amende §6.5)

`PerimetreInterceptor` lit `request.getRequestURI()`, qui n'est **pas** décodé. Une requête
`GET /api/dossiers/1635ee6c%2Dbcd9%2D4c38%2Db50c%2Df518b00a15ab` échoue au `UUID.fromString`, le
contrôle est **sauté**, et Spring — qui décode, lui — sert le dossier : **200, hors périmètre**. Les
~30 routes imbriquées de dossier héritent de la faille.

Le correctif n'est pas d'ajouter un décodage : c'est de cesser d'analyser une chaîne d'URL comme
frontière d'autorisation. Lire les variables de chemin **déjà résolues par Spring**
(`HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE`), c'est-à-dire exactement la valeur reçue par le
contrôleur.

### 9bis.2 Extension du périmètre à six endpoints supplémentaires (amende §6.5 — **décision produit**)

Le §6.5 excluait clients / prospects / hotline. La mesure montre que l'exclusion vide la feature de
son sens : `GET /api/clients` et `GET /api/prospects` n'étant pas filtrés, il suffit d'énumérer les
tiers pour relire tout le pipeline. **Décision : les six endpoints ci-dessous sont filtrés.**

| Endpoint | Donnée exposée |
|---|---|
| `GET /api/clients/{id}/dossiers` | `DossierListItemDto` |
| `GET /api/prospects/{id}/dossiers` | `DossierListItemDto` |
| `GET /api/prospects/{id}/prospections` | `ProspectionListItemDto` |
| `GET /api/prospects/{id}/reglements` | `ReglementProspectDto` + `dossierCode` |
| `GET /api/devis` | offre, `prospectionId`/`prospectionCode`, tranche, montants |
| `GET /api/hotline/recherche?telephone=` | tier + prospections + dossiers + règlements + interactions |

Le filtre masque les **lignes** hors périmètre ; il ne masque ni le client, ni le prospect, ni la
fiche hotline eux-mêmes (ils restent hors périmètre au sens du §6.5 : on ne filtre pas les tiers).
Points d'entrée : `ProspectSubResourcesService`, `DevisService`, service hotline.

### 9bis.3 Authentification rétablie sur les générateurs de documents (**décision produit**)

`SecurityConfig` porte `permitAll()` sur `/api/dossiers/*/documents/**`,
`/api/prospections/*/documents/**` et `/api/prospections/*/devis/*/document` (trou marqué
`TEMPORAIRE` par une session antérieure). Sans principal, le périmètre est « non restreint » par
construction : **n'importe quel PDF de dossier est téléchargeable sans aucun cookie** — client, prix
et échéancier compris.

**Décision : ce trou est refermé sur cette branche.** Le front envoie déjà `credentials: 'include'`
partout, l'impact UI attendu est nul, mais les endpoints documents doivent être retestés.
Conséquence à acter : `PerimetreCommercialService` ne doit plus traiter « pas de principal » comme
« non restreint » sur ces routes.

### 9bis.4 `PUT .../projets` — lignes à portée mixte (amende §7.2, **le vrai piège**)

`findByAdmUserIdAndCfgProjectIsNotNull` + `deleteAll` supprime toute ligne dont `CfgProjectId` est non
nul, **y compris celles qui portent aussi une autre portée**, et les recrée sans elle. Mesuré sur
`yesRems` : 2 lignes `CfgCompany`+`CfgProject` appartiennent à l'utilisateur `Mkamara`
(`EA72B281-B80B-44AF-8613-48200E8F86C6`) — un seul `PUT` détruit ses habilitations société de
l'application de bureau, toujours en service.

Règle correcte : **mettre `CfgProjectId` à `NULL` sur les lignes mixtes** (la ligne survit avec ses
autres portées) et ne supprimer que les lignes à portée projet exclusive.

Le test initial `putProjets_preserveLesHabilitationsLegataires` ne l'avait pas vu : sa fixture
n'utilisait que des lignes mono-portée. Toute fixture de ce test doit désormais inclure une ligne
mixte.

### 9bis.5 `POST /api/dossiers` ne valide que le premier article (amende §6.5)

`DossierService` ne vérifie que `articles.get(0)`. Avec `articleIds = [lotDuProjetA, annexeDuProjetB]`
la création renvoie **201** et le lot du projet B est marqué vendu (`comFolderId` positionné) — une
**écriture** transverse au périmètre, par un appelant qui ne peut même pas lire ce projet. Tous les
articles doivent être validés, pas le premier.

### 9bis.6 Corps de requête `null` (durcissement)

`{"projetIds": null}` ou `{}` est aujourd'hui traité comme liste vide : 200, toutes les affectations
projet supprimées, le compte redevient silencieusement non restreint. Un corps tronqué depuis l'UI
d'administration lève donc une restriction. Attendu : **400**. Une suppression de restriction doit
être explicite (`{"projetIds": []}`).

### 9bis.7 Descriptions springdoc mensongères (amende §7)

À corriger en même temps que le code — le front n'a que la spec OpenAPI comme source de vérité :
- `PUT .../projets` affirme que les autres portées sont « strictement préservées » : faux avant
  9bis.4, à réécrire une fois corrigé.
- `perimetreRestreint` affirme que les listes **et les détails** sont filtrés : ne devient vrai
  qu'après 9bis.1 et 9bis.2.
- `GET .../projets` : préciser qu'une liste non vide sur un compte exempté (§6.2) ne restreint
  toujours rien.

### 9bis.8 Vérifié sain — ne pas re-traiter

Sondes passées au vert, à ne pas réinstruire : pas de dérive de schéma `yesRems`/`yesRemsTest`
(colonnes, types, nullabilité identiques) ; pas de doublon `(user, projet)` ni d'orphelin sur les 1071
lignes réelles ; paramètres matriciels et UUID majuscules non contournants ; id inconnu et id hors
périmètre renvoient une réponse **identique** (pas d'oracle d'existence) ; aucun fan-out par
`CfgEmployeeId` ; codes de rôle sans espaces parasites, `Direction Générale` octet-identique ; les
quatre agrégats du tableau de bord sont filtrés ; `DocumentLotService#genererLot` est bien le **seul**
chemin d'id porté par le corps de requête. Un utilisateur n'ayant que des portées tranche/société
devient non restreint — confirmé conforme au §6.3 et non atteignable dans les données réelles.

---

## 9ter. RÉVISION 2 — la frontière doit échouer en fermeture

Seconde passe adverse : 7 échecs sur 15 sondes
(`src/test/java/.../controller/perimetre/PerimetreCommercialRevisionTest.java`). Les 12 défauts du
§9bis sont, eux, réellement corrigés (les 22 tests de la première passe restent verts).

### 9ter.1 Même fuite, autre porte : `%20` (amende §9bis.1)

`GET /api/dossiers/%20363c934e-ea46-446a-aad2-e26bd0032f49` renvoie **200 et le dossier complet**
d'un projet hors périmètre. Idem espace après l'id, sur `/api/prospections/{id}`,
`/api/encaissements/{id}` et les routes imbriquées (`/montants` a renvoyé les montants du projet B).

Les deux côtés lisent bien la même chaîne, mais ne la **convertissent** pas pareil :

| | conversion | résultat sur `" 363c934e-…"` |
|---|---|---|
| `PerimetreInterceptor` | `UUID.fromString(valeur)` | exception → `null` → **contrôle sauté** |
| Spring `@PathVariable UUID` | `UUID.fromString(source.trim())` | **liaison réussie** → ressource servie |

L'argument porteur du saut silencieux — « le contrôleur échouera aussi à lier, donc 400 sans que la
ressource soit servie » — est **faux**, démontré par test.

**Correctif imposé — ne pas se contenter de recopier le `trim()`.** Reproduire la conversion de
Spring dans l'intercepteur, c'est reconduire le même contrat fragile : il resterait à parier qu'aucune
autre divergence de conversion n'existe. La règle devient :

> Sur une collection **effectivement soumise au périmètre**, si la variable de chemin d'id est
> présente et qu'aucun UUID ne peut en être résolu, **refuser (404)**. Ne jamais sauter le contrôle
> sur la foi de ce que le contrôleur fera ensuite.

Un id légitime est toujours un UUID valide : refuser ce qui ne l'est pas ne coûte aucun cas d'usage.
Le souci « 500 au lieu de 400 » évoqué dans la javadoc se règle en renvoyant `false` depuis
`preHandle` après avoir positionné le statut, pas en levant une exception. Le saut reste légitime dans
le seul cas où le motif de route **ne comporte pas** de variable d'id (routes à segment littéral :
`/api/dossiers/documents/…`, `/api/dossiers/simulation-financement`) — cas structurel, décidé sur le
motif, jamais sur la valeur.

### 9ter.2 Hotline : le filtre en mémoire évince les lignes **du** périmètre (amende §9bis.2)

`HotlineService.toFiche` charge les 50 interactions les plus récentes
(`findByCfgTierIdOrderByDocDateDesc`, `PageRequest.of(0, 50)`) **puis** applique `projetVisible` en
Java. Un tiers ayant 50 interactions récentes sur le projet B et 1 plus ancienne sur le projet A
renvoie `interactions: []` à un commercial restreint au projet A : sa propre ligne n'était pas dans la
page chargée, et les 50 chargées sont ensuite toutes filtrées. Le filtre supprime les lignes visibles
et n'en garde aucune.

Atteignable en données réelles : 2 tiers de `yesRems` dépassent 50 interactions (max 69).

**Correctif : filtrer dans la requête (`Specification` sur `ComActionMessage`), puis prendre 50.**
Jamais l'inverse. `appelsSansFiche` (`findByNormalizedTempTel(..., PageRequest.of(0, 50))`, sans tri)
souffre exactement du même défaut et doit être corrigé de la même façon, bien qu'aucun test ne puisse
le reproduire de façon déterministe (ordre DB arbitraire).

### 9ter.3 Hotline : les interactions rattachées à un dossier fuient (amende §9bis.2)

`projetVisible` ne lit que `m.getComProspection()` et conclut « projet non résolu ⇒ visible » (§6.4)
dès qu'il est nul. Or `ComActionMessage` porte aussi un `ComFolderId`, d'où le projet se résout
parfaitement. Mesuré : **13 lignes** de `yesRems` ont `ComFolderId NOT NULL AND ComProspectionId IS
NULL`, et les 13 résolvent un projet. La règle §6.4 ne s'applique qu'après avoir **épuisé les deux
chemins** — prospection *puis* dossier.

### 9ter.4 Vérifié sain à cette passe — ne pas re-traiter

`permitAll()` réellement supprimé (18 routes balayées en anonyme, **toutes 401** ; ne restent que
`/api/auth/**` et swagger) ; l'id est au segment 3 et nommé `{id}` sur **toutes** les routes des
quatre préfixes, et aucun `@PathVariable String/Object` n'existe dans `controller/` ;
`StlQuote.CfgTrancheId` est populé **312/312 (100 %)** et résout un projet dans 100 % des cas, donc
`filtreDevis()` est sain ; le tri `docDate DESC` des sous-ressources est préservé ; le comptage des
sous-ressources filtrées est correct (`totalElements` reflète le périmètre, pas de pagination dans le
vide) ; `PUT .../projets` tient sur une ligne à quatre portées et sur quatre cycles répétés (ni
doublon, ni orpheline) ; `{}`/`null` → 400 ; la boucle de `POST /api/dossiers` attrape l'article
fautif en toute position sans rien écrire, et `setComFolderId` n'existe qu'à un seul endroit de tout
`src/main`.

**Risque latent acté** (pas un défaut aujourd'hui) : « pas de principal ⇒ non restreint » dans
`PerimetreCommercialService` est devenu inatteignable uniquement parce que `SecurityConfig` n'a plus
aucun matcher applicatif en `permitAll()`. La sûreté du périmètre dépend donc d'une propriété d'un
autre fichier. Le test de balayage anonyme sert de fil de détente si quelqu'un en réintroduit un.

---

## 10. Contradictions relevées avec la demande initiale (à garder en tête)

1. La table d'affectation **existait déjà** — la feature expose et applique `AdmUserHabilitation`, elle
   ne la crée pas.
2. Les **pré-prospections ne peuvent pas être rattachées à un commercial** (commercial NULL par
   construction) — seul le projet les scope.
3. `StkItem.CfgProjectId` est mort (0 %) ; `StkHierarchyId` pointe vers `CfgTranche` malgré son nom.
4. Le grain projet est **plus permissif** que le grain tranche du légataire (41,3 % des habilitations
   projet sont partielles au niveau tranche).
5. `AdmUser.CfgEmployeeId` n'est pas unique — ne jamais résoudre le périmètre par le tier.
6. Deux mécanismes de rôles coexistent et divergent — `AdmUserRole` fait foi.
