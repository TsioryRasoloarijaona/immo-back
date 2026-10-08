# Source de vérité unique : séparer les lectures des écritures

## Pourquoi

Le CRM cohabite avec **OMRAN Estate**, le logiciel de bureau qu'une dizaine de personnes
utilisent tous les jours sur `192.168.11.115`. Les deux écrivent dans le même schéma.

Une synchronisation **bidirectionnelle** a été étudiée puis écartée, sur mesure :

- les deux bases frappent leurs numéros depuis **leur propre compteur `AdmSequence`**, qui
  avance séparément (`Tier` 14 431 d'un côté, 14 503 de l'autre — écart de 72) ;
- il n'existe **aucun index unique sur `Code`** : les 8 index trouvés sont des artefacts
  `_dta_index_*`, tous `is_unique = 0` ;
- le dégât est **déjà là**. Neuf codes clients ont été attribués à deux personnes différentes,
  avec des `PKey` distincts :

  | Code | base A | base B |
  |---|---|---|
  | 26/14423 | Karim MAZZI | Chaymae X |
  | 26/14428 | Badr ARH | Marwân CHÂKIR |
  | 26/14431 | Post TEST | Nahoma NAHOMA |

  (…et six autres, contigus de `26/14423` à `26/14431`.)

**Aucune technologie de réplication ne corrige ça** : `AdmSequence` est un compteur applicatif,
pas une identité SQL, donc la gestion de plages d'identité de merge replication ne le couvre pas.
La documentation Microsoft l'écarte de toute façon ici — Express ne peut être **qu'abonné**
(*Subscriber only*), et merge replication **n'existe pas** sur SQL Server pour Linux.

Répliquer le compteur ne marche pas non plus : la collision naît **entre la lecture et
l'écriture** du compteur, pas après. Deux sites qui lisent 14431 au même instant frappent tous
deux 14432, quelle que soit la vitesse de réplication.

Partitionner les plages (un site à 500 000) réglerait le problème sans coordination — mais ces
numéros sont des **numéros de dossier**, une série métier qui doit rester unique et continue.

**D'où la solution retenue : une seule base est écrite.** Le problème n'est pas géré, il n'existe
plus.

## Ce qui est en place

```
ÉCRITURES  →  la source de vérité (OMRAN), seule à frapper les numéros
LECTURES   →  la copie locale du backend
```

| Élément | Rôle |
|---|---|
| `config/SourceDeVeriteProperties` | coordonnées de la source, préfixe `app.source-de-verite` |
| `config/DataSourceConfig` | les deux sources, l'aiguillage, le petit pool `hikari-remontee` du rapatriement |
| `config/CollecteurLignesEcrites` | écouteurs Hibernate `POST_INSERT`/`POST_UPDATE` |
| `config/RemonteeLecture` | recopie immédiate de la ligne écrite, par `MERGE`, **avant la réponse HTTP** |

### Rapatriement synchrone (FIX-rapatriement-synchrone.md)

Le rapatriement de nos propres écritures tourne **dans le fil de la requête** : la transaction
publie `EcrituresValidees` en `afterCommit`, `RemonteeLectureListener` (sans `@Async`) recopie
chaque ligne, puis seulement le contrôleur écrit sa réponse. Invariant : **quand un
POST/PUT/PATCH/DELETE répond 2xx, chaque ligne qu'il a écrite est lisible dans la copie.**

Pourquoi : en asynchrone, la réponse 201 partait avant la recopie, et le front, qui relit
aussitôt, lisait l'ancien état (action ajoutée invisible, fiche restée « pré-prospection »,
`GET /api/dossiers/{id}` en 404 ~10 s après le `POST`). L'écart grandit en production, où la
source est jointe par Internet.

- **Pool à part, `hikari-remontee`** (`SOURCE_DB_POOL_REMONTEE`, défaut 2). Pendant
  `afterCommit`, la requête tient encore sa connexion du pool `ecriture` (Hibernate la garde
  jusqu'au nettoyage de fin de transaction, après les `afterCommit`) ; relire la ligne sur le
  même pool, c'est en emprunter une seconde : avec 5 écritures simultanées et un pool de 5,
  chacune attendrait l'autre jusqu'au timeout. Sur un pool distinct, aucune attente croisée :
  une recopie prend une connexion `remontee` puis une `lecture`, jamais l'inverse. En
  `afterCommit` la synchronisation de transaction est encore active, donc `DataSourceUtils`
  lie ces deux connexions au fil : **une recopie tient sa connexion `remontee` (et sa
  `lecture`) jusqu'à la fin de la transaction de la requête (`afterCompletion`)**, pas
  instruction par instruction — une seule connexion `remontee` pour toute la recopie, et
  **au plus 2 recopies simultanées** avec le pool par défaut ; une troisième attend.
  La relecture des passes `SynchronisationDescendante` (même `RemonteeLecture`) passe aussi
  par ce pool.
- **Un échec de recopie ne fait pas échouer la requête** : l'écriture est validée sur la
  source ; `log.error`, et la synchronisation périodique rattrape.
- **Connexion indisponible = recopie abandonnée d'un coup.** Si la connexion (`remontee` ou
  `lecture`) ne s'obtient pas (pool épuisé, source coupée), le reste de la recopie est
  abandonné avec un seul `log.error` donnant le nombre de lignes non rapatriées : sinon
  chaque ligne attendrait à son tour le timeout de connexion (5 s), soit N × 5 s dans le
  temps de réponse. Une erreur SQL sur **une** ligne (contrainte…) n'arrête pas les suivantes.
- **Ordre parent → enfant** : `AdmEvent` (RDV d'une action « Visite ») est recopié avant
  `ComActionMessage`, qui le référence.
- L'exécuteur `remonteeExecutor` a disparu avec l'`@Async`.

**La clé de routage est le drapeau `readOnly` de la transaction.** Le code le portait déjà :
48 `@Transactional(readOnly = true)` sur 93, en défaut de classe surchargé méthode par méthode.
**Aucun service n'a été modifié.**

## Trois pièges, tous rencontrés

1. **`LazyConnectionDataSourceProxy` n'est pas une optimisation.** Sans lui la connexion est
   empruntée *avant* que la transaction ne pose son drapeau : la clé vaut toujours `false` et
   **tout part sur la source**, lectures comprises. Aucune erreur — seulement une application
   lente et un aiguillage qui n'aiguille rien.
2. **`@ConditionalOnProperty` ne convient pas** pour « propriété non vide » : la propriété est
   toujours présente avec une valeur vide par défaut, et elle est alors considérée comme
   présente. D'où `@ConditionalOnExpression`.
3. **Une écriture marquée `readOnly` écrirait dans la copie**, où sa ligne serait écrasée au
   rapatriement suivant — sans trace. `RoutageEcrituresTest` verrouille ça.

## Mode mono-base

`SOURCE_DB_HOST` vide et **rien de tout ceci n'existe** : autoconfiguration Spring Boot seule,
comportement d'avant la feature. C'est ce qui permet aux 890 tests de passer sans modification —
la suite n'a pas de seconde base.

## Vérifié de bout en bout

Contre la paire locale `yesRems` (source) / `yesRemsProd` (copie), par `POST /api/prospects`
authentifié :

| | |
|---|---|
| ✅ Suite inchangée en mono-base | 890 tests verts |
| ✅ Routage en lecture | `Routage -> LECTURE`, ouverture du pool `hikari-lecture`, sessions sur la copie |
| ✅ **Le numéro vient de la source** | code `26/14434` — suivant du compteur **source** (14433), pas de celui de la copie (14503) |
| ✅ **Le compteur n'avance que d'un côté** | source 14434, copie **14503 inchangée** |
| ✅ **Rapatriement immédiat** | la ligne est dans la copie sans attendre de synchronisation — garanti avant la réponse depuis FIX-rapatriement-synchrone (avant, recopie `@Async` : la réponse pouvait la devancer) |

### Trois défauts que seule cette vérification a révélés

1. **401 sur toute requête authentifiée.** `JwtAuthenticationFilter` interroge la base à chaque
   requête, or un filtre ne porte pas de transaction — et la transactionnalité par défaut de
   Spring Data ne couvre <b>pas</b> les requêtes dérivées. La lecture partait donc sur la source
   pendant que la connexion lisait la copie. Corrigé par `@Transactional(readOnly = true)` sur
   `AppUserCredentialRepository#findByAdmUserId`.
2. **Le rapatriement ne partait jamais.** Le vidage de session d'Hibernate a lieu *dans*
   `doCommit()`, donc après les `beforeCommit` de Spring : la publication y voyait une collecte
   vide. Déplacée dans `afterCommit`, ce qui interdit du même coup un
   `@TransactionalEventListener` — voir le javadoc de `RemonteeLectureListener`.
3. **`AdmSequence` était collectée pour rapatriement.** Recopier le compteur dans la copie y
   réinstallerait exactement la divergence que cette feature supprime. Exclusion explicite dans
   `CollecteurLignesEcrites`.

### Un quatrième défaut, trouvé en testant depuis le front

Une prospection créée par le CRM **n'apparaissait pas dans le logiciel de bureau** — alors que la
donnée était bien arrivée dans les deux bases. Ce n'était donc pas la synchronisation.

La requête réelle de son écran, relevée dans sa trace d'audit pendant la consultation :

```sql
SELECT * FROM (SELECT * FROM ComSaleProspectionView) AS ComProspection
 WHERE (1 = 1)
   AND (StrEntityId = @P1)
   AND (StkHierarchyId IN ('c273dec7-…', '5eee12f0-…'))
   AND ((ComFolderId IS NULL) OR (… StatusIsCancellation=1 OR StatusIsClosing=1))
   AND (IsClosed = 0)
   AND (DocDate >= @P2) AND (DocDate < @P3)
```

Et la vue se termine par `WHERE ComProspection.StrEntityId <> '<pré-prospection>'`.

Nos lignes échouaient sur **deux** conditions, l'une et l'autre parce qu'en SQL un `NULL` ne
satisfait aucune comparaison :

| Colonne | Notre valeur | Écartée par |
|---|---|---|
| `StrEntityId` | `NULL` | le `<>` de la vue **et** le `=` de l'écran |
| `IsClosed` | `NULL` | `IsClosed = 0` |

`StrEntityId` explique **100 %** de l'appartenance à la vue : 14 036 lignes avec un type valide y
sont toutes, les 3 641 pré-prospections et les 5 lignes à `NULL` n'y sont jamais.

**Une fausse piste, pour mémoire** : j'ai d'abord attribué le défaut au seul `IsClosed`, sur une
comparaison à deux lignes. Le croisement à l'échelle a montré que les 938 lignes incriminées
étaient simplement, par coïncidence, celles dont le `StrEntityId` était exclu. `IsClosed` compte
bien — c'est un filtre de l'écran — mais il n'explique pas l'appartenance à la vue.

Corrigé à la source : `ProspectionService` pose `StrEntityId` (« Prospection vente »),
`IsClosed` et `IsOldCustomer` ; `ProspectService` pose les cinq drapeaux hérités de `CfgTier`.
Le manque sur `StrEntityId` était **connu et documenté** sur `ComProspection.strEntityId` — il se
referme ici.

Quatre tests dans `CreationConformiteLegataireTest`, dont un qui asserte la **présence dans la
vue** plutôt que la valeur d'une colonne : c'est le résultat métier qui compte. Vérifié qu'ils
échouent sans le correctif.

**Et une découverte qui ne nous concerne pas** : 936 prospections créées *dans le logiciel de
bureau* entre 2024 et 2026 ont `IsClosed` à `NULL` et sont écartées par le filtre de l'écran.
`db/migration/008_ComProspection_visibilite_legataire.sql` répare les nôtres et laisse celles-là
en commentaire — les faire apparaître est une décision métier.

**Laissé de côté, faute d'évidence** : `UtilisateurAdminService` crée un `CfgTier` de type
EMPLOYÉ sans `IsVisibleInList`, la colonne sur laquelle l'écran employés filtre (`= 1`). Mais
chez OMRAN la répartition est **58 à `0` contre 9 à `1`** : un choix par personne, pas un défaut.

### Données de test à retirer

Trois prospects créés dans `yesRems` par la vérification :

```sql
DELETE FROM dbo.CfgTier WHERE LastName = 'A-SUPPRIMER';
-- 26/14432 VERIF-ROUTAGE · 26/14433 VERIF-REMONTEE · 26/14434 VERIF-REMONTEE-2
```

Le compteur `Tier` de `yesRems` est passé de 14431 à 14434 et ne redescend pas — sans
conséquence, la série tolère les trous.

## Pièces jointes : le seul flux qui déroge au routage automatique

Le cas ne pouvait pas passer tel quel en deux-bases, pour trois raisons mesurées :

1. **le binaire n'est pas mappé** — `AttachedFile` est FILESTREAM côté légataire, supprimée de
   la copie par la migration 004 (FILESTREAM n'existe pas sur SQL Server Linux). Or le logiciel
   de bureau **ne lit que cette colonne** : sans elle, une pièce déposée par le CRM serait pour
   lui une case de checklist vide ;
2. **`AppFileObject` n'a pas de `PKey`** — sa clé est `AdmAttachedFileId`, or le rapatriement
   supposait `PKey` partout ;
3. **le rapatriement exigeait que la copie ait toutes les colonnes de la source** — juste pour
   une dérive de schéma, faux pour une divergence voulue comme `AttachedFile`.

### Le flux retenu (S3 → source → copie, mêmes identifiants partout)

```
1. S3                     l'objet, comme avant — un échec ultérieur ne laisse qu'un orphelin balayable
2. source de vérité       AdmAttachedFile (métadonnées, par JPA routé) + le binaire
                          (UPDATE natif via DepotBinaireVersSource, MÊME transaction, même connexion)
                          + AppFileObject (par JPA routé)
3. copie                  par le rapatriement ordinaire : AdmAttachedFile SANS la colonne binaire,
                          AppFileObject entier
```

Aucun aller-retour d'identifiants : le `PKey` est frappé **côté application**
(`UUID.randomUUID()`, `PieceJointeSupport`) et posé tel quel dans les trois écritures. Pas de
compteur `AdmSequence` sur ce flux — le risque de collision qui a motivé la feature n'existe pas
ici (UUID aléatoire, et une collision improbable serait rejetée bruyamment par la clé primaire).

### Ce que ça a changé dans le code

| Où | Quoi |
|---|---|
| `RemonteeLecture` | clé d'identité par table (`AppFileObject` → `AdmAttachedFileId`) ; colonnes volontairement absentes de la copie, exclues **dès le `SELECT`** pour que le binaire ne repasse jamais sur le réseau |
| `CollecteurLignesEcrites` | `AppFileObject` dans l'ordre des clés étrangères, après son parent `AdmAttachedFile` |
| `DepotBinaireSource` (+ 2 impl.) | l'UPDATE natif du binaire sur la source. `Propagation.MANDATORY` : même transaction, donc même connexion routée que l'INSERT des métadonnées — sur une connexion séparée, l'UPDATE attendrait le verrou de l'INSERT non validé. En mono-base : no-op, comportement d'avant |
| `PieceJointeSupport.deposer` | l'appel au dépôt du binaire, entre les deux `save` |

La colonne est sondée une fois (`COL_LENGTH`) : une source qui ne l'a pas est tolérée avec un
avertissement, pour que le montage reste testable sur une paire locale quelconque.

### À faire à la main sur 115 avant d'activer (côté utilisateur)

- appliquer `001_AppUserCredential.sql`, `002_AppFileObject.sql`, `003_AppFileMigrationFailure.sql`,
  `006_AppFileObject_DeletedBy.sql` — **pas la 004** (elle supprimerait la colonne que le
  légataire lit) ;
- recopier depuis la copie les lignes `AppUserCredential` et `AppFileObject` existantes : en
  deux-bases, les transactions d'écriture **lisent aussi** sur la source (changement de mot de
  passe, suppression d'une pièce), et une ligne absente y ferait un 404 à tort.

Sur la paire locale de vérification (`yesRems` source / `yesRemsProd` copie), tout est déjà en
place : `yesRems` a les trois tables `App*` et sa colonne `AttachedFile` (varbinaire, nullable).

### Vérifié de bout en bout (21/09, banc `yesRems` source / `yesRemsProd` copie)

Campagne menée par HTTP authentifié (compte `Khadija`), vérification SQL des deux côtés après
chaque appel :

| Endpoint | Constat |
|---|---|
| `POST/PATCH /api/prospects`, contacts | ✅ ligne + rapatriement, code `26/14441` frappé sur la source, compteur copie intact |
| `POST /api/prospections` + étape/interaction/ville/tranche-principale | ✅ des deux côtés, **présente dans `ComSaleProspectionView` des deux côtés** |
| `POST /api/dossiers` (`ARS 2026/00409`) | ✅ dossier + lot lié (`StkItem.ComFolderId`) + historique de statut, compteur copie intact |
| `POST échéancier` + `POST règlement` | ✅ des deux côtés |
| `PATCH statut règlement` | 409 attendu (transition refusée) — le garde-fou traverse le montage |
| **`POST pièce jointe`** | ✅ **binaire 72 o dans la source**, métadonnées des deux côtés, `AppFileObject` rapatrié, téléchargement S3 conforme |
| **`DELETE pièce jointe`** | ✅ `DeletedAt`/`DeletedBy` rapatriés |
| `POST /api/mon-compte/changer-mot-de-passe` | ❌ **404 attendu** : `Khadija` absente d'`AppUserCredential` **sur la source** — la preuve du rattrapage à faire (§ ci-dessus) |

**Deux défauts trouvés et corrigés par cette campagne :**

1. **Les `DELETE` ne voyageaient pas** — le collecteur n'écoutait que `POST_INSERT`/`POST_UPDATE` :
   un contact supprimé restait affiché par le CRM (fantôme dans la copie). Corrigé : collecte de
   `POST_DELETE`, et au rapatriement **l'absence sur la source vaut suppression dans la copie**,
   traitée après les fusions et en ordre inverse (enfants d'abord — la copie porte les 1 219 FK).
2. **`yesRems.AppFileObject` n'avait pas `DeletedBy`** (006 non appliquée sur la source du banc) :
   l'INSERT échouait et ressortait en **401 nu** — l'exception d'un filtre part dans le dispatch
   d'erreur, pas dans le `ProblemDetail`. Migration appliquée au banc ; sur 115 c'est le point
   « appliquer 006 » de la check-list. Un 401 inattendu sur un endpoint d'écriture doit faire
   penser à ça.

**Prérequis confirmé par l'échec `ELKORRI`** : toute ligne référencée par une création doit
exister **sur la source** (la validation lit dans la transaction d'écriture). Sur le banc local
la copie est plus récente que la source, d'où l'échec ; face à 115, la source est le
sur-ensemble et le cas ne se produit pas — sauf pour les lignes créées par le CRM seul
(`AppUserCredential`, employés créés par l'API) : c'est le rattrapage documenté plus haut.

### Données de test à retirer (campagne du 21/09)

Sur **les deux bases** (`yesRems` et `yesRemsProd`), dans cet ordre :

```sql
DELETE FROM dbo.AppFileObject   WHERE AdmAttachedFileId IN ('98243728-F16B-486F-843A-02BC65FCF468','A509C1DC-909A-4503-854F-99E03259DE1F');
DELETE FROM dbo.AdmAttachedFile WHERE PKey IN ('98243728-F16B-486F-843A-02BC65FCF468','A509C1DC-909A-4503-854F-99E03259DE1F');
DELETE FROM dbo.StlSettlementRelation WHERE StlSettlementId = '616D7CA1-F2B4-4A5A-8E2F-41D3AC7EB8E6';
DELETE FROM dbo.StlSettlementLine     WHERE StlSettlementId = '616D7CA1-F2B4-4A5A-8E2F-41D3AC7EB8E6';
DELETE FROM dbo.StlSettlement   WHERE PKey = '616D7CA1-F2B4-4A5A-8E2F-41D3AC7EB8E6';
DELETE FROM dbo.ComDocument     WHERE PKey = '14EBE3ED-2943-4EE9-B970-76266A49B57C';
UPDATE dbo.StkItem SET ComFolderId = NULL WHERE PKey = 'FAC8B2CA-1E78-4EF5-AE9C-18194C09C0E4'; -- libérer ARS0934
DELETE FROM dbo.ComFolderItem   WHERE ComFolderId = '620C0A88-EDA3-4AE7-91E8-A4E489ACBADE';
DELETE FROM dbo.StrFolderStatusHistory WHERE ComFolderId = '620C0A88-EDA3-4AE7-91E8-A4E489ACBADE';
DELETE FROM dbo.ComFolder       WHERE PKey = '620C0A88-EDA3-4AE7-91E8-A4E489ACBADE';
DELETE FROM dbo.ComActionMessage WHERE ComProspectionId = 'AE957C63-99CD-477C-B93B-CE7FC47D5938';
DELETE FROM dbo.ComProspectionCity WHERE ComProspectionId = 'AE957C63-99CD-477C-B93B-CE7FC47D5938';
DELETE FROM dbo.ComProspection  WHERE PKey = 'AE957C63-99CD-477C-B93B-CE7FC47D5938';
DELETE FROM dbo.CfgTierContact  WHERE PKey = '3CF302C9-B403-4F59-AAF1-A5559895AB91'; -- fantôme pré-correctif, copie seulement
DELETE FROM dbo.CfgTier         WHERE LastName = 'A-SUPPRIMER';
```

Les deux objets S3 (`pieces-jointes/dossier/620c0a88-…/*.txt`) relèvent du balayage habituel.
Compteurs source avancés (`Tier` 14442, `Prospection` 5942, `Dossier de vente` 409) — la série
tolère les trous, rien à faire.

### Décision laissée ouverte : la suppression côté légataire

Notre suppression est logique (`AppFileObject.DeletedAt`), rapatriée dans la copie : la pièce
disparaît du CRM. Sur la source, la ligne `AdmAttachedFile` et son binaire restent — **le
logiciel de bureau continue donc d'afficher une pièce supprimée dans le CRM.** Aligner les deux
(supprimer la ligne côté légataire ?) est une décision métier, pas prise ici.

## Synchronisation descendante : ce qu'OMRAN écrit rejoint la copie

Le rapatriement immédiat ne couvre que **nos** écritures. Ce que le logiciel de bureau saisit
sur la source n'atteignait jamais la copie — un dossier créé au bureau restait invisible du
CRM. `SynchronisationDescendante` ferme ce trou : un cycle **toutes les 5 minutes**
(`SOURCE_DB_SYNC_MINUTES`, défaut 5), porté par le **Change Tracking** de SQL Server (activé
sur la source par `prototype/ct-activer-yesRems.sql`, rétention 7 jours ; disponible sur
Express, aucune colonne ni déclencheur ajouté — OMRAN ne voit rien).

La conception reprend `prototype/sync-ct-descendant.sql` (la version T-SQL parquée, qui exigeait
un seul serveur) :

- **deux régimes par table** — `BASELINE` (filigrane absent ou périmé : fusion de toutes les
  lignes par lots de 500, **sans jamais supprimer** — la copie porte des lignes que la source
  n'a jamais eues) et `DELTA` (`CHANGETABLE` donne les clés touchées ; l'opération I/U/D
  rapportée n'est pas consultée, **la relecture fait foi** : présent → fusion, absent →
  suppression) ;
- **filigrane par table**, côté copie (`AppSyncEtat`, migration `009`) : une table en échec
  rejoue depuis son propre point sans bloquer les autres ; le filigrane n'avance que si la
  passe a réussi ;
- fusions dans l'ordre des FK, suppressions après et en ordre inverse — même logique que le
  rapatriement immédiat, **même `MERGE`** (le code est partagé avec `RemonteeLecture`) ;
- la liste des tables est lue sur la source (`sys.change_tracking_tables`) à chaque cycle —
  suivre une table de plus ne demande aucun redéploiement ;
- nos propres écritures reviennent par ce canal : refusionnées à l'identique, no-op idempotent.

**Vérifié sur le banc** (cycle réglé à 1 min) : INSERT, UPDATE et DELETE faits par `sqlcmd`
directement dans la source (simulant OMRAN) arrivent chacun dans la copie au cycle suivant ;
baseline exercée sur `ComProspectionType` (24 lignes) ; un échec de table (le cast UUID du
pilote, corrigé) a bien laissé son filigrane en arrière et rejoué au cycle suivant. Un cycle
vide coûte ~2 s sur le banc, l'essentiel en allers-retours `CHANGETABLE`.

### Cadence 2 min + déclenchement manuel

- **Défaut ramené à 2 minutes** (`SOURCE_DB_SYNC_MINUTES`, était 5). Un cycle vide reste
  quasi gratuit ; le seul coût récurrent est un balayage des ~23 tables (une lecture de
  filigrane + `CHANGE_TRACKING_MIN_VALID_VERSION` + `CHANGETABLE` par table). Ce coût fixe
  **croît avec le nombre de tables × le RTT** — sur 115 et sa queue de latence, un cycle vide
  peut prendre quelques secondes ; c'est acceptable à 2 min, à surveiller si on descend plus bas.
- **`POST /api/synchronisation`** — « synchroniser maintenant », réservé au rôle
  Administrateur. Synchrone, renvoie le bilan (`fusions`, `suppressions`, `tablesTraitees`,
  `tablesEnEchec`, `dureeMs`). **Un seul cycle à la fois** : un `ReentrantLock.tryLock`
  partagé avec le cycle planifié — le second arrivant reçoit **409** au lieu de doubler le
  travail. En **mono-base**, le service n'existe pas : le contrôleur reste présent (injection
  par `ObjectProvider`) et répond **409** avec un message explicite, plutôt que de faire
  échouer le démarrage.

  Vérifié sur le banc : une ligne insérée dans la source apparaît dans la copie **au
  déclenchement** (200, `fusions:1`) sans attendre le cycle ; deux appels concurrents → un 200,
  un 409 ; en mono-base et sans le rôle, 409 et 403 (tests `SynchronisationControllerTest`).

**Au déploiement (côté utilisateur)** : appliquer `009_AppSyncEtat.sql` sur les **copies**
(`yesRemsDev` VPS — jamais sur 115), activer le Change Tracking sur 115 (`ct-activer-yesRems.sql`
adapté), et si la copie vient d'une restauration fraîche de la source, **pré-remplir les
filigranes à la version courante** (script en tête de la migration 009) pour éviter une
baseline intégrale inutile. En recette, `SOURCE_DB_SYNC_MINUTES=1` raccourcit l'attente.

## Impact front

**Aucun changement de contrat.** Ni les chemins d'URL, ni les DTO, ni les codes de statut ne
changent. Le front peut **relire aussitôt** après une écriture 2xx : la recopie dans la copie
est faite avant la réponse (section « Rapatriement synchrone »). Contrepartie : la latence
d'une écriture inclut cette recopie — une relecture sur la source et un `MERGE` sur la copie
par ligne écrite, en plus d'un aller-retour réseau par requête SQL d'écriture.

## Hors périmètre

- ~~La synchronisation descendante périodique~~ — **faite**, voir la section dédiée ci-dessus.
- La **reprise du stock existant de binaires** entre 115 et S3 dans les deux sens — le flux du
  dépôt est traité ci-dessus (section Pièces jointes), pas le rattrapage de l'historique.
