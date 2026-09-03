# FEATURE — Stockage des pièces jointes sur S3 / MinIO

**Branche** : `migration/s3-storage`
**But** : sortir les binaires des pièces jointes de SQL Server ; toute méthode de
dépôt et toute méthode de lecture de fichier passe par S3.

---

## 1. État des lieux (relevé sur `yesRems`, pas supposé)

### Colonnes binaires, tables de base uniquement (vues exclues)

| Table | Colonne | Type | Lignes / avec binaire | Volume | Max | Lue par le backend |
|---|---|---|---:|---:|---:|---|
| **`AdmAttachedFile`** | **`AttachedFile`** | `varbinary(max)` | 3411 / **3134** | **8 239 Mo** | 34,8 Mo | **oui — la seule** |
| `AdmReportData` | `ReportFile` | `image` | 293 / 270 | 43,8 Mo | 8,9 Mo | non — définitions Stimulsoft, poste Windows |
| `CfgImage` | `ImageData` | `image` | 63 / 63 | 0,9 Mo | — | non |
| `AdmParamsImage` | `CodeValue` | `image` | 3 / 2 | 0,02 Mo | — | non |
| `AdmMailModel`, `AdmWebMails`, `CfgProjectZone` | — | — | **0 ligne** | — | — | non |
| `__MigrationHistory`, `sysdiagrams`, `dtproperties` | — | — | tables système EF / SSMS | — | — | non |

**Périmètre : `AdmAttachedFile` seule.** Les 45 Mo restants sont propres à
l'application Windows ; les laisser en base coûte 45 Mo et évite de toucher à ce dont
le poste Windows dépend.

Contenu : `.pdf` 2682 fichiers / 7 872 Mo, images 446 / 366 Mo, `.xlsx`/`.docx` 6.
277 lignes sans binaire = cases de checklist `IsCheckedButNotAttached`, déjà exclues
par `hasFile`.

### Chemins d'accès au binaire dans le code — un seul de chaque

- écriture : `PieceJointeSupport.deposer()` → `setAttachedFile(fichier.getBytes())`
- lecture : `PieceJointeSupport.fichier()` → `.body(fichier.getAttachedFile())`
  (`DossierController:502`, `:637`, `ArticleController:203`)

Trois façades minces délèguent (`PieceJointeService`, `ReglementPieceJointeService`,
`ArticlePieceJointeService`), 8 endpoints, dont 2 dépôts. **Les listes ne touchent
jamais au binaire** (`AdmAttachedFileSummary` l'omet et passe par `@Formula hasFile`).

Le basculement est donc localisé, pas transversal.

---

## 2. Livré sur cette branche

### `db/migration/` — schéma applicatif scripté

`spring.jpa.hibernate.ddl-auto=none` des deux côtés : **démarrer le backend ne crée
rien**. Aucun `.sql` n'existait dans le dépôt ; `AppUserCredential` avait été créée à
la main et n'était nulle part. Sur une restauration de production, l'authentification
serait cassée avant même d'arriver à la migration.

- `001_AppUserCredential.sql` — rétro-ingéniérie de la table live (8 colonnes, PK,
  unicité sur `AdmUserId`, FK vers `AdmUser`)
- `002_AppFileObject.sql` — le pointeur S3, contrat entre le script et le backend
- `003_AppFileMigrationFailure.sql` — journal d'échecs

Idempotents, à exécuter dans l'ordre. Voir `db/migration/README.md` pour l'argumentaire
contre `ddl-auto=update` et pour le diff de schéma à faire avant la production.

### `migration/` — reprise des binaires (Python, ponctuel)

Voir `migration/README.md`. Lecture seule sur le legacy, reprise par anti-jointure,
SHA-256 vérifié côté serveur, traitement du plus petit au plus grand fichier.

**Phase 3 (vidage de `AttachedFile`) volontairement non écrite** : tant qu'il n'y a pas
de sauvegarde du bucket, vider les binaires ferait passer 8 Go de contrats, chèques et
pièces d'identité d'un stockage sauvegardé vers un stockage qui ne l'est pas.
Réplication Dropbox prévue — à faire avant d'écrire cette phase.

---

## 3. Reste à faire — côté Java

### 3.1 Port de stockage

Nouveau paquet `storage/` :

```java
public interface FileStorage {
    StoredObject store(String key, InputStream content, long size, String contentType);
    InputStream open(String key);
    void delete(String key);
}
```

`S3FileStorage` comme implémentation. Configuration `app.storage.{bucket,endpoint,
region,prefix}` ; **`forcePathStyle = true`** (MinIO n'accepte pas l'adressage par
sous-domaine de bucket). Identifiants par variables d'environnement, jamais en
properties.

**Dépendance `pom.xml` : `software.amazon.awssdk:s3`.** Non ajoutée — règle 1 du
CLAUDE.md. `s3-transfer-manager` inutile à ces tailles.

**Tests** : la suite tape une vraie base mais ne doit jamais taper le vrai S3. Prévoir
une implémentation en mémoire / répertoire temporaire, activée par profil.

### 3.2 Lecture avec repli — le cœur du basculement

Dans `PieceJointeSupport.fichier()` : ligne `AppFileObject` présente → flux depuis S3 ;
absente → `AttachedFile` comme aujourd'hui. Cette unique bifurcation rend le
déploiement possible **avant** toute migration, et sert d'interrupteur de bascule.

Passer aussi le contrôleur de `byte[]` à `ResponseEntity<InputStreamResource>` : avec
des fichiers de 35 Mo, tout charger en mémoire est inutile. **Aucun changement de
contrat** — même URL, mêmes en-têtes.

### 3.3 `hasFile` — le risque de casse silencieuse

`AdmAttachedFileSummary.hasFile` vaut aujourd'hui :

```java
@Formula("(CASE WHEN AttachedFile IS NOT NULL THEN 1 ELSE 0 END)")
```

Dès que les nouveaux dépôts laisseront `AttachedFile` à `NULL`, **cette formule
renverra `false` et le fichier disparaîtra de toutes les listes** — sans erreur.
Elle doit couvrir les deux sources :

```sql
CASE WHEN AttachedFile IS NOT NULL
       OR EXISTS (SELECT 1 FROM AppFileObject o
                  WHERE o.AdmAttachedFileId = PKey AND o.DeletedAt IS NULL)
     THEN 1 ELSE 0 END
```

C'est le point le plus susceptible de casser en silence. À couvrir par un test.

### 3.4 Écriture — ordre imposé

S3 n'est pas dans la transaction. **Toujours S3 d'abord, puis la base**, avec un
`TransactionSynchronization` qui supprime l'objet en cas de rollback. Cet ordre fait
que la seule fuite possible est un objet orphelin dans le bucket (coût, sans
conséquence fonctionnelle, balayable). L'ordre inverse laisserait des lignes pointant
vers le vide, c'est-à-dire des 500 au téléchargement.

Suppressions : marquer `DeletedAt`, jamais supprimer l'objet dans la transaction de la
requête.

### 3.5 Clés des nouveaux dépôts

Même format que la migration (voir `migration/README.md`). Le segment porteur doit
venir d'une énumération contrôlée par l'application (`DOSSIER`, `REGLEMENT`,
`ARTICLE`), pas de `Category` qui est du texte libre en données réelles — ce qui
absorbera au passage les littéraux dispersés dans les trois façades
(`ReglementPieceJointeService:42-43`, `PieceJointeService:80`).

Rappel : `AppFileObject.StorageKey` stocke la clé **en entier** et l'application ne la
recalcule jamais. Une divergence de forme entre les clés de la migration et celles des
nouveaux dépôts est donc sans conséquence fonctionnelle.

### 3.6 Table des types MIME

`web.ContentTypeParExtension` ignore `.jfif` et `.heic`, qui existent en données réelles
(4 fichiers). Le script Python les traite ; ajouter les deux entrées côté Java pour que
les nouveaux dépôts soient cohérents.

---

## 4. Impact front

**Aucun à ce stade.** Le basculement décrit ci-dessus conserve URL, méthode, en-têtes
et corps de réponse à l'identique.

Une évolution ultérieure vers des **URL présignées** (le fichier transitant directement
entre le navigateur et MinIO, sans passer par le backend) changerait, elle, le contrat
de `GET …/pieces-jointes/{pieceId}/fichier` : corps binaire → redirection ou URL. Si
elle est retenue, elle doit être un **endpoint distinct**, pas une redéfinition de
l'existant — un endpoint re-scopé a déjà provoqué des 404 silencieux sur ce projet.
Même raisonnement pour un dépôt direct en `PUT` présigné.

---

## 5. Décisions arrêtées sur données réelles

- **Table à part plutôt que colonnes sur `AdmAttachedFile`** — le poste Windows garde
  sa structure sur son propre serveur ; le concern S3 reste dans du schéma applicatif.
  Précédent : `AppUserCredential`.
- **Pas de FK `AppFileObject` → `AdmAttachedFile`** — le schéma legacy n'en déclare
  aucune vers cette table, et cela garde le script indépendant de l'ordre d'insertion.
- **Les 11 lignes non cartographiables ont `CategoryId IS NULL`** : rien à sonder, ce
  sont de vrais orphelins. Rangés sous `orphelin/`, migrés quand même (16 Mo).
- **`facture` (1622 fichiers, 5,5 Go, 67 % du volume) est migré** bien qu'aucun
  endpoint ne l'expose — c'est l'essentiel du poids de la base.
- **MinIO `RELEASE.2025-09-07T16-13-09Z`** : `ChecksumSHA256` disponible, pas de repli
  nécessaire.

## 6. Points ouverts

- **Droit `CREATE TABLE` sur `yesRems`** — `tsix` ne l'a pas. Nécessaire pour appliquer
  `002`/`003` avant la passe réelle.
- **TLS sur l'endpoint MinIO** — 8 Go de données personnelles clients traversent
  l'internet public.
- **Sauvegarde du bucket** (réplication Dropbox) — prérequis de la phase 3.
- **Diff de schéma** entre la restauration de production et la base de dev, pour
  détecter d'éventuelles colonnes ajoutées à la main non scriptées.
