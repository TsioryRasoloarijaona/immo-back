# Migration des pièces jointes binaires vers S3 / MinIO

Outil **ponctuel**, exécuté deux fois : une fois sur la base de développement pour
valider, une fois sur la restauration de production. Volontairement en Python et hors
du build Maven — il ne doit jamais partir dans le jar.

## Ce qu'il fait, et ce qu'il ne fait pas

| | |
|---|---|
| Lit | `dbo.AdmAttachedFile.AttachedFile` — **lecture seule**, aucun `UPDATE` sur le schéma legacy |
| Écrit | le bucket S3, `dbo.AppFileObject`, `dbo.AppFileMigrationFailure` |
| Ne fait **pas** | vider `AdmAttachedFile.AttachedFile` — c'est `purge_attached_file.sql`, séparé et explicitement confirmé |

## Vérification et purge

`--verify` compare, pour **chaque** objet, sa taille et son empreinte SHA-256 entre la
base et le stockage. MinIO renvoyant le checksum enregistré à l'écriture, la couverture
est totale par simples requêtes `HEAD` — **sans retélécharger les 8 Go**.

Trois conditions doivent être réunies pour que la purge soit autorisée : aucun fichier
restant à migrer, aucun échec journalisé, aucun écart de taille ou d'empreinte.

```bash
$PY migrate_blobs_to_s3.py --prefix prod --verify --workers 8     # doit sortir en 0

sqlcmd -S "<hote>" -U <user> -P <pwd> -d yesRemsProd -C -b \
       -v CONFIRMER=OUI -i purge_attached_file.sql
```

`purge_attached_file.sql` est **volontairement hors de
`src/main/resources/db/migration/`** : ce répertoire suit la convention Flyway, et un
script de purge qui s'y trouverait serait exécuté automatiquement au démarrage du
backend le jour où Flyway serait adopté.

Il refuse de s'exécuter sans `-v CONFIRMER=OUI`, et recontrôle les trois compteurs
avant d'écrire quoi que ce soit.

**Après la purge, le stockage objet est la seule copie de ces fichiers.** Ne la lancer
que lorsqu'il existe une autre copie : sauvegarde du bucket, ou `.bak` d'origine
conservé à part.

## Prérequis

1. Le schéma applicatif appliqué sur la base visée — voir `../src/main/resources/db/migration/`.
   **Nécessite un compte disposant du droit `CREATE TABLE`** ; `tsix` ne l'a pas sur
   `yesRems`.
2. `ODBC Driver 17 for SQL Server` (déjà présent sur le poste de dev).
3. Un environnement Python :

```bash
cd migration
python -m venv .venv
./.venv/Scripts/python.exe -m pip install -r requirements.txt   # Windows
```

## Où mettre les identifiants

**Jamais en argument de ligne de commande** — ils resteraient dans l'historique du
shell et dans la liste des processus. Jamais non plus dans un fichier suivi par git.

### En développement — `migration/.env`

```bash
cp .env.example .env     # puis renseigner les valeurs
```

Le script le charge automatiquement au démarrage et annonce le nombre de variables
lues. `.env` est ignoré par git (`.gitignore`), `.env.example` ne contient que des
clés vides et sert de modèle.

| Variable | Rôle |
|---|---|
| `DB_PWD` | mot de passe SQL Server |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | compte de service MinIO |

### En production — variables d'environnement

**Toute variable déjà présente dans l'environnement prime sur le fichier.** Sur le
serveur, exporter les secrets suffit ; aucun `.env` à déposer :

```bash
export DB_PWD=...  S3_ACCESS_KEY=...  S3_SECRET_KEY=...
```

Un autre emplacement se donne avec `--env-file /chemin/vers/secrets`.

### Le compte MinIO doit être dédié

Ne pas utiliser `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`. Créer un compte de service
limité au seul bucket de la migration :

```bash
mc admin user svcacct add local <utilisateur> --name migration-immo-crm
```

Un identifiant qui ne peut qu'écrire dans un bucket ne peut pas effacer les autres —
et ces clés vont transiter par un `.env`, un historique shell et des journaux.

### Côté backend Java, plus tard

Mêmes variables, **jamais dans `application.properties`** (le fichier est suivi par
git). Le fichier ne porte que le nom du bucket et l'endpoint ; les clés viennent de
l'environnement du processus, comme `DB_PWD` et `JWT_SECRET` aujourd'hui.

## Déroulé recommandé

Avec un `.env` complet, tout vient de là — plus aucun paramètre de connexion à
répéter :

```bash
PY=./.venv/Scripts/python.exe

# 0. accès au stockage : aller-retour d'un objet témoin, checksum compris.
#    Ne touche pas à la base. À lancer en premier, ça coûte une seconde.
$PY migrate_blobs_to_s3.py --prefix test --check

# 1. inventaire — aucun envoi, fonctionne même sans AppFileObject
$PY migrate_blobs_to_s3.py --prefix test --dry-run

# 2. échantillon
$PY migrate_blobs_to_s3.py --prefix test --limit 50

# 3. passe complète
$PY migrate_blobs_to_s3.py --prefix test --workers 6 --log-file migration.log

# 4. contrôle exhaustif avant toute purge
$PY migrate_blobs_to_s3.py --prefix test --verify --workers 8
```

Les fichiers sont traités **du plus petit au plus grand** : une erreur de
configuration se voit en quelques secondes, pas après un envoi de 35 Mo.

`--check` distingue les causes de refus les plus fréquentes (clés invalides, bucket
inexistant, droits insuffisants, endpoint injoignable) au lieu d'une trace boto3 brute.

### Débit — exécuter la passe de production au bon endroit

Mesuré depuis le poste de développement : **0,6 Mo/s**, soit environ **3 h 30** pour
les 8 Go. C'est la liaison montante du poste, pas le script. La passe de production a
tout intérêt à être lancée **depuis le serveur, à côté de la base**, où les mêmes
8 Go passent en quelques minutes.

Une coupure n'est de toute façon pas grave : voir « Reprise après coupure ».

## Reprise après coupure

Relancer la même commande. La liste de travail exclut par anti-jointure tout id déjà
présent dans `AppFileObject`, et une ligne n'y est écrite qu'après un envoi vérifié
par S3. Il n'y a donc **aucun état intermédiaire à nettoyer** et aucun risque de
double envoi.

Pour voir ce qui reste :

```sql
SELECT COUNT(*) FROM dbo.AdmAttachedFile a
WHERE a.AttachedFile IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.AppFileObject o WHERE o.AdmAttachedFileId = a.PKey);
```

Diagnostic des échecs :

```sql
SELECT TOP 50 * FROM dbo.AppFileMigrationFailure ORDER BY FailedAt DESC;
```

## `--prefix` n'est pas cosmétique

La base de production sera une **restauration du même jeu de données** : les `PKey`
y sont identiques à ceux de la base de dev. Sans préfixe distinct (`test` / `prod`),
la seconde exécution écraserait les objets de la première, aux mêmes clés. Des buckets
séparés sont encore plus sûrs.

## Inventaire relevé sur `yesRems` (3 134 fichiers, 8 239 Mo)

| Porteur | Fichiers | Volume |
|---|---:|---:|
| `facture` | 1622 | 5 528,9 Mo |
| `dossier` | 486 | 1 182,0 Mo |
| `article` | 476 | 779,9 Mo |
| `reglement` | 460 | 681,1 Mo |
| `grh-conge` | 10 | 26,5 Mo |
| `orphelin` | 11 | 15,5 Mo |
| `email` | 39 | 8,1 Mo |
| `echeance` | 11 | 8,1 Mo |
| autres (9 porteurs) | 19 | 9,3 Mo |

Aucune catégorie non cartographiée, aucune collision de clé, aucune extension
inexploitable. `facture` est de loin le plus gros poste (67 % du volume) et **n'a
aucun endpoint côté backend** — migré quand même, puisque c'est l'essentiel du poids
de la base.

Les 11 `orphelin` ont `CategoryId IS NULL` : ils ne sont rattachés à rien (10 `.jpg`
décrits « A » à « J », plus `ENTETE AYAD DEVELOPPEMENT.png`). Migrés sous
`{prefix}/orphelin/{fileId}{ext}` pour ne rien perdre sans inventer de parent.

## Format des clés

```
{prefix}/pieces-jointes/{porteur}/{porteurId}/{fileId}{ext}
{prefix}/orphelin/{fileId}{ext}
```

Exemple réel :
`test/pieces-jointes/facture/b7cfe9ae-…/e3f9b400-….pdf`

`{fileId}` est `AdmAttachedFile.PKey` : immuable, donc renommer une description ne
déplace jamais l'objet. Le nom de fichier d'origine n'entre **jamais** dans la clé
(accents, espaces, données personnelles) — il est servi en `Content-Disposition` et
conservé en métadonnée d'objet.

Chaque objet porte aussi `x-amz-meta-source-{pkey,category,category-id,description}`,
ce qui rend le bucket auto-descriptif si la base venait à être perdue.

## Intégrité

Le SHA-256 est calculé avant l'envoi et transmis en `ChecksumSHA256` : MinIO le
revérifie côté serveur et rejette un transfert altéré. Confirmé disponible sur la
version cible (`RELEASE.2025-09-07T16-13-09Z`). L'empreinte est stockée dans
`AppFileObject.Sha256` pour un contrôle ultérieur.
