# Session Memory
_Last updated: 2026-09-04T15:10:00+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective
Sortir les 8 Go de pièces jointes binaires de SQL Server vers un stockage objet MinIO,
puis basculer le backend dessus (dépôt → S3 + pointeur en base ; lecture → URL présignée),
pour permettre un déploiement sur un VPS Linux. **Migration terminée et backend intégré ;
reste à pousser la branche et à configurer l'environnement d'exécution.**

---

## ✅ What Worked
- **Inventaire avant tout** : `sys.columns ... is_filestream/varbinary/image` sur les tables de
  base (vues exclues) → une seule table exploitée par le backend, `AdmAttachedFile`
  (3 138 fichiers, 8 245 Mo). Les 45 Mo restants (`AdmReportData`, `CfgImage`…) sont propres au
  poste Windows et laissés en place.
- **Migration Python ponctuelle** (`migration/migrate_blobs_to_s3.py`), hors build Maven :
  lecture seule sur le legacy, reprise par anti-jointure sur `AppFileObject`, SHA-256 transmis
  en `ChecksumSHA256` (vérifié serveur), traitement du plus petit au plus grand fichier.
  **3 138/3 138 en 14 min, 0 échec, 9,7 Mo/s.**
- **Trois niveaux de vérification** : checksum serveur à l'écriture, `--verify` (HEAD sur les
  3 138 objets, taille + empreinte vs base, sans retélécharger), et téléchargement réel
  octet-pour-octet sur les 6 plus gros fichiers (34,8 → 24,4 Mo). Tous conformes.
- **`--check`** (aller-retour d'un objet témoin) pour valider endpoint/TLS/identifiants/checksum
  en une seconde avant toute passe.
- **Purge + ramasse-miettes FILESTREAM** : base passée de ~10 Go à 1,8 Go.
- **Suite complète verte : 463/463.**

## ❌ What Failed
- `ddl-auto=update` **écarté** — Hibernate compare *toutes* les entités et émettrait des `ALTER`
  sur le schéma legacy. Précédent réel : les 8 colonnes fictives de `ComFolder` auraient été
  créées au lieu de lever `Invalid column name`.
- **Confirmation de purge via `$(CONFIRMER)`** → substitution propre à sqlcmd ; DataGrip passe la
  chaîne telle quelle, la confirmation ne pouvait jamais aboutir. Remplacé par un drapeau
  `@confirmation` édité dans le fichier.
- **Garde `IF COL_LENGTH(...) IS NULL RETURN;`** → inopérant : SQL Server résout les noms de
  colonnes à la **compilation du lot entier**. Toutes les requêtes nommant `AttachedFile` sont
  passées en `sp_executesql`.
- **`S3Client.Builder`** n'existe pas dans le SDK AWS v2 → `S3ClientBuilder` (type racine).
- **Suite lancée contre `yesRemsProd`** → 4 échecs de dérive de schéma (`StkItemType.StkKindId`
  `NOT NULL` là-bas, nullable dans `yesRemsTest`). Abandonné au profit de l'application des
  scripts DDL sur `yesRemsTest`.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | `Autorisation CREATE TABLE refusée` sur `yesRems` | Fixed | L'utilisateur a ajouté `tsix` à `db_ddladmin` |
| 2 | Purge : `$(CONFIRMER)` inutilisable hors sqlcmd | Fixed | Drapeau `@confirmation` en clair dans le fichier |
| 3 | `Le fichier 'YesRems_FILESTREAM' n'est pas vide` malgré GC à 0 | Fixed | La **table** gardait sa clause `FILESTREAM_ON` → `ALTER TABLE ... SET (FILESTREAM_ON = "NULL")` |
| 4 | `Nom de colonne non valide : AttachedFile` après le drop | Fixed | `sp_executesql` (résolution à la compilation du lot) |
| 5 | `cannot find symbol: class Builder` (S3Client) | Fixed | `S3ClientBuilder` |
| 6 | 25 erreurs « Unable to find @SpringBootConfiguration » | Fixed | Cascade d'un **seul** échec (302 vs 200) : le seuil d'échec de contexte franchi fait renoncer Spring à le recharger |
| 7 | `sqlcmd -i /tmp/x.sql` → `opening file C: Access denied` | Fixed | Conversion de chemin MSYS — utiliser un chemin relatif |
| 8 | Connexion `localhost,1433` en timeout | Fixed | C'est un **tunnel SSH** vers le VPS — ajouter `-l 60` |

---

## 🔑 Key Points to Remember
- **`AdmAttachedFile.AttachedFile` était une colonne FILESTREAM**, pas un `varbinary(max)`
  ordinaire : contenu dans un groupe de fichiers séparé, embarqué par `BACKUP DATABASE`, et un
  `NULL` ne fait que *marquer* les fichiers. Sans `CHECKPOINT` +
  `sp_filestream_force_garbage_collection`, les 8 Go restent sur disque **et dans le .bak**.
- **SQL Server pour Linux refuse toute base DÉCLARANT un groupe FILESTREAM**, même vide — c'est
  la déclaration qui est rejetée. D'où `004`, sans quoi le RESTORE sur le VPS échoue.
- **Supprimer la colonne ne détache pas la table** du groupe de fichiers (`FILESTREAM_ON`
  subsiste dans `sys.tables.filestream_data_space_id`) ; le message d'erreur « n'est pas vide »
  est trompeur.
- **`AppFileObject.StorageKey` stocke la clé EN ENTIER** et n'est jamais recalculée pour un
  fichier existant. Une divergence de forme entre clés de migration et clés de nouveaux dépôts
  est donc sans conséquence.
- **`hasFile` était le point de rupture silencieuse** : sa `@Formula` nommait `AttachedFile` et
  est évaluée à *chaque* chargement d'`AdmAttachedFileSummary` — elle aurait cassé **toutes les
  listes**, pas seulement les téléchargements. Elle interroge désormais `AppFileObject`.
- **Ordre d'écriture imposé : stockage d'abord, base ensuite**, + `TransactionSynchronization`
  qui supprime l'objet au rollback. Seule fuite possible : un objet orphelin (coût, sans
  conséquence). L'ordre inverse donnerait des 500 au téléchargement.
- **`Category` est inexploitable comme discriminant** (texte partiellement libre : ~110 libellés
  « Dossier de vente N° … », 11 lignes vides) → énumération `PorteurPieceJointe`.
- **Les 11 lignes non cartographiables ont `CategoryId IS NULL`** — de vrais orphelins, migrés
  sous `orphelin/`.
- **`facture` = 1 622 fichiers / 5,5 Go (67 % du volume) et n'a aucun endpoint backend** —
  migré quand même.
- **Autorités = `ROLE_` + `AdmRole.Code`** (pas `Description`). `hasRole('Administrateur')`
  exige donc `Code = 'Administrateur'`.
- **RUPTURE DE CONTRAT FRONT** : les 3 endpoints `/fichier` renvoient `302` + `Location` au lieu
  du binaire. `<a href>`, `window.open`, `fetch` continuent de marcher, **mais MinIO doit
  autoriser l'origine du front en CORS** — sinon échec CORS après une redirection réussie.
  Documenté dans `instructions/FEATURE-s3-storage.md` §4. Frontend **non touché** (frontière du
  dépôt).
- Reporté volontairement : **pas de `PUT` présigné** pour le dépôt (chantier séparé, endpoint
  distinct) ; **pas d'upload d'article** (lecture seule, non demandé).

---

## 📁 Important Files & Paths

**Schéma applicatif** — `src/main/resources/db/migration/` (chemin Flyway ; idempotents) :
`001_AppUserCredential.sql` (rétro-ingéniérie, n'existait dans aucun fichier du dépôt),
`002_AppFileObject.sql`, `003_AppFileMigrationFailure.sql`,
`004_drop_attachedfile_filestream.sql`, `README.md`.

**Migration** — `migration/` : `migrate_blobs_to_s3.py` (`--check`/`--dry-run`/`--limit`/
`--verify`), `purge_attached_file.sql` (**délibérément hors de `db/migration/`** : Flyway
l'exécuterait au démarrage), `requirements.txt`, `.env.example`, `README.md`.
`.env` et `.venv` sont gitignorés.

**Stockage (backend)** — `src/main/java/com/tsix/immocrm/storage/` : `StockageFichiers` (port),
`StockageS3`, `StockageConfig` (`pathStyleAccessEnabled(true)` **obligatoire** pour MinIO, sur le
client *et* le signataire), `StockageProperties`, `CleStockage`, `PorteurPieceJointe`,
`ObjetStocke`. + `entities/app/AppFileObject`, `repositories/app/AppFileObjectRepository`.

**Modifiés** : `services/piecejointe/PieceJointeSupport` (seul chemin de lecture et d'écriture),
`entities/adm/AdmAttachedFile` (colonne démappée), `AdmAttachedFileSummary` (`@Formula`),
les 3 façades (`PieceJointeService`, `ReglementPieceJointeService`, `ArticlePieceJointeService`),
`DossierController`, `ArticleController`, `application.properties`.

**Tests** : `src/test/java/com/tsix/immocrm/storage/StockageMemoire.java` (`@Primary`, la suite ne
touche jamais le réseau) et `FixturePieceJointe.java` (crée métadonnées **+** pointeur ; l'oubli
du pointeur rend un fixture invisible via `hasFile`).

**Spec** : `instructions/FEATURE-s3-storage.md` (inventaire, décisions, §4 impact front,
§6 dépendance et variables d'environnement).

---

## 🔧 Environment & Config
- **Deux serveurs SQL** : local `localhost,49687` (`yesRems`, `yesRemsProd`, `yesRemsTest`) et
  **`localhost,1433` = tunnel SSH vers le VPS** (`yesRemsDev`). Le tunnel exige `-l 60` sur
  `sqlcmd`, sinon timeout à la première connexion.
- **États des bases** : `yesRemsProd` (local) migrée + purgée + colonne supprimée, prête pour le
  `.bak` → VPS. `yesRemsDev` (VPS) déjà migrée (3 138 pointeurs, colonne supprimée).
  `yesRemsTest` a reçu 002/003/004 et sert la suite.
- **MinIO** `RELEASE.2025-09-07`, endpoint TLS ; bucket **`immo-back`** (objets sous `prod/`).
  Buckets dev/prod séparés → `S3_PREFIX` devenu **facultatif** (il n'évitait que la collision
  dans un bucket partagé, les `PKey` étant identiques entre restaurations).
- **Secrets** : `migration/.env` (gitignoré, modèle dans `.env.example`) ; toute variable déjà
  présente dans l'environnement prime. **À faire tourner** : la clé de service MinIO a transité
  par la conversation.
- **`pom.xml`** : `software.amazon.awssdk:s3:2.32.14` ajoutée par l'utilisateur.
- **Variables d'exécution du backend** (en plus de `DB_*`/`JWT_SECRET`) : `S3_BUCKET`,
  `S3_ENDPOINT`, `S3_REGION`, `S3_PREFIX`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`.
- **Compte admin créé sur `yesRemsDev`** : `t.admin`, rôle `Administrateur`,
  `MustChangePassword=0`, `ProfileCompleted=1`. Hash BCrypt produit avec l'encodeur du projet
  puis relu depuis la base et vérifié. `AppUserCredential` y était **vide** (0 ligne pour
  74 `AdmUser`) : c'est le seul compte connectable. Mots de passe non consignés ici.
- **Git** : branche `migration/s3-storage`, arbre propre, **10 commits non poussés**
  (`c378841` → `0597f56`).

---

## ⏭️ Next Step
> Pousser la branche (`node .claude/scripts/git-flow.mjs push back`), ouvrir la PR sur
> `TsioryRasoloarijaona/immo-back` et attendre la fusion par l'utilisateur avant tout autre
> travail. La demande de push a été posée trois fois sans réponse — la reposer.

### Backlog
- [ ] **CORS sur MinIO** pour l'origine du front — c'est ce qui cassera en premier après le
      déploiement, avec un symptôme trompeur (échec CORS *après* une redirection réussie).
- [ ] **Sauvegarde du bucket** (réplication Dropbox évoquée) : MinIO est aujourd'hui la seule
      copie des 8 Go. Filets restants : le `.bak` d'origine et `yesRems` local (3 134 fichiers,
      instantané plus ancien).
- [ ] **Faire tourner la clé de service MinIO** (passée en clair dans la conversation) et
      vérifier qu'elle est bien limitée au bucket, pas root.
- [ ] Restaurer le `.bak` de `yesRemsProd` sur le VPS et lancer le **diff de schéma** contre la
      base de dev pour détecter d'éventuelles colonnes ajoutées à la main non scriptées.
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic` (4 fichiers réels) — le script Python les
      traite, pas le Java.
- [ ] Endpoint d'upload pour les pièces jointes d'Article (lecture seule aujourd'hui).
- [ ] `UtilisateurAdminService` : bug `Description` jamais renseignée à la création du `CfgTier`
      employé, déjà corrigé dans `ProspectService` — signalé, non corrigé.
- [ ] Réactiver l'auth sur les générateurs de documents (3 `permitAll()` dans `SecurityConfig`,
      trou temporaire assumé) avant la prod.
- [ ] Anciens items encore ouverts : bug cookie 401 (`app.jwt.cookie-secure` sans profil actif),
      `sommePrixVenteTTC`, `PATCH` devis, source de `GET /api/commerciaux`, DESISTEMENT.
