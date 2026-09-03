# Migration des pièces jointes binaires vers S3 / MinIO

Outil **ponctuel**, exécuté deux fois : une fois sur la base de développement pour
valider, une fois sur la restauration de production. Volontairement en Python et hors
du build Maven — il ne doit jamais partir dans le jar.

## Ce qu'il fait, et ce qu'il ne fait pas

| | |
|---|---|
| Lit | `dbo.AdmAttachedFile.AttachedFile` — **lecture seule**, aucun `UPDATE` sur le schéma legacy |
| Écrit | le bucket S3, `dbo.AppFileObject`, `dbo.AppFileMigrationFailure` |
| Ne fait **pas** | vider `AdmAttachedFile.AttachedFile` (phase 3, script séparé, non écrit à ce stade) |

La phase 3 n'existe pas encore délibérément : tant qu'il n'y a pas de sauvegarde du
bucket, vider les binaires ferait passer 8 Go de contrats, chèques et pièces
d'identité d'un stockage sauvegardé (la base) à un stockage qui ne l'est pas.

## Prérequis

1. Le schéma applicatif appliqué sur la base visée — voir `../db/migration/`.
   **Nécessite un compte disposant du droit `CREATE TABLE`** ; `tsix` ne l'a pas sur
   `yesRems`.
2. `ODBC Driver 17 for SQL Server` (déjà présent sur le poste de dev).
3. Un environnement Python :

```bash
cd migration
python -m venv .venv
./.venv/Scripts/python.exe -m pip install -r requirements.txt   # Windows
```

## Secrets

Jamais en argument de ligne de commande (ils resteraient dans l'historique du shell) :

```bash
export DB_PWD=...
export S3_ACCESS_KEY=...
export S3_SECRET_KEY=...
```

## Déroulé recommandé

```bash
PY=./.venv/Scripts/python.exe
COMMUN="--db-host localhost,49687 --db-name yesRems --db-user tsix \
        --bucket immo-crm --endpoint https://minio.example.com"

# 0. inventaire — aucun envoi, fonctionne même sans AppFileObject
$PY migrate_blobs_to_s3.py $COMMUN --prefix test --dry-run

# 1. échantillon : valide identifiants, bucket, TLS et checksum en quelques secondes
$PY migrate_blobs_to_s3.py $COMMUN --prefix test --limit 50

# 2. passe complète
$PY migrate_blobs_to_s3.py $COMMUN --prefix test --workers 6 --log-file migration.log
```

Les fichiers sont traités **du plus petit au plus grand** : une erreur de
configuration se voit en quelques secondes, pas après un envoi de 35 Mo.

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
