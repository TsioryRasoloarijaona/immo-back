# Schéma de test versionné

Ces fichiers reconstituent, dans un SQL Server vierge, le schéma dont la suite de tests a
besoin. Ils sont consommés par `.github/workflows/ci.yml`, qui monte une base éphémère à
chaque *pull request*.

| Fichier | Contenu | Origine |
|---|---|---|
| `010_tables.sql` | 165 tables : colonnes, clés primaires, contraintes `CHECK`, valeurs par défaut, index | généré |
| `020_foreign_keys.sql` | 562 clés étrangères, dans une passe séparée pour s'affranchir de l'ordre de création | généré |
| `030_triggers.sql` | 4 triggers (`ComFolder`, `RntFolder`) | généré |
| `040_seed_reference.sql` | lignes de configuration indispensables | écrit à la main |
| `050_views.sql` | `ComSaleProspectionView`, les 2 tables qu'elle lit, et son semis | écrit à la main |
| `060_interop_vente.sql` | `AdmParamsGuid`, `StlDocumentLine`, `ComLoanCompanyDecision` (lues par la vente hors entités JPA) et la devise par défaut ; idempotent, applicable aussi à `yesRemsTest` | écrit à la main |
| `070_workflow_vente.sql` | tables de paramétrage du workflow (`StrWorkFlow`, intervenants, champs et pièces obligatoires), **vides** ; idempotent, applicable aussi à `yesRemsTest` | écrit à la main |
| `070_vente_sous_listes.sql` | `ComModification`, `ComGift`, `ComGiftReason`, `ComGiftQuantity`, `ComFolderGift`, `StlPaymentAuthorizationLine`, `CfgTrancheSettlementCategory` (fiche dossier, FEATURE-vente-echeancier.md) ; idempotent, applicable aussi à `yesRemsTest` | écrit à la main |
| `080_offres_schema_financement.sql` | `StlQuoteDocument` (schéma de financement saisi sur une offre, FEATURE-encaissements-offres-lot2.md) ; idempotent, applicable aussi à `yesRemsTest` | écrit à la main |
| `100_audit_tables.sql` | `AdmWebHistorics` (journal d'audit OMRAN, `Details` en `text`) et `AdmMessage` (notes), FEATURE-patrimoine-audit-actions.md ; idempotent, déjà présentes dans `yesRemsTest` | écrit à la main |
| `101_audit_fk.sql` | aucune clé étrangère : `AdmWebSessions`, cible de `AdmWebHistorics.AdmWebSessionsId` en production, est absente du CI | écrit à la main |

Le CI applique **tous** les `ci/schema/*.sql` triés par nom (ordre `C`) : un nouveau fichier n'a pas à être
ajouté à `ci.yml`. Les deux `070_*` n'ont pas de dépendance entre eux.

### Plages réservées aux lots du plan Patrimoine

Chaque lot livre `0NN_<lot>_tables.sql` puis `0NN+1_<lot>_fk.sql` (même s'il n'a aucune clé étrangère), dans sa
plage :

| Lot | Plage | Lot | Plage | Lot | Plage | Lot | Plage |
|---|---|---|---|---|---|---|---|
| P0 | 100-101 | P1a | 102-103 | P1b | 104-105 | P5 | 106-107 |
| P1c | 108-109 | P2a | 110-111 | P2b | 112-113 | P6 | 114-115 |
| P3 | 116-117 | P4 | 118-119 | P7a | 120-121 | P8a | 122-123 |
| P7b | 124-125 | P8b | 126-127 | P9 | 128-129 | P10 | 130-131 |

## Pourquoi le schéma de production plutôt que `yesRemsTest`

`yesRemsTest`, la base locale contre laquelle la suite tourne historiquement, **n'est pas une
copie de production** : elle a été bâtie à la main et ne contient que les tables mappées. Sur
ce même sous-ensemble de tables, l'écart mesuré était le suivant :

| | `yesRemsTest` | production |
|---|---|---|
| Tables | 79 | 462 (165 dans le périmètre ci-dessous) |
| Clés étrangères | 116 | 295, dont 97 sortent des 79 tables |
| Triggers | 0 | 2 sur `ComFolder` |

Cette dérive est un mode de panne récurrent du projet : un test passe en local et le
comportement diffère en production. Reconstituer le schéma de production en CI la supprime,
au prix d'un CI plus strict que la base locale — c'est l'objectif, pas un effet de bord.

## Périmètre : pourquoi 165 tables et pas 462

La graine est l'ensemble des tables mappées par une entité JPA (78), plus
`AppFileMigrationFailure` (table d'outillage de migration, sans entité). On y ajoute la
**fermeture transitive des clés étrangères** : toute table pointée par une table déjà retenue,
sinon les contraintes ne peuvent pas être créées.

Sont volontairement exclus :

- **215 des 216 vues.** L'exception est `ComSaleProspectionView`, mappée depuis la feature
  « vues legacy — fiche prospection » par une entité JPA `@Immutable` en lecture seule ; elle
  vit dans `050_views.sql` avec les deux tables qu'elle lit et qui n'entrent dans aucune
  fermeture de clés étrangères (`AdmAlertData`, `AdmParamsInteger`). Toutes les autres restent
  hors périmètre — et `ComActionMessageView` a été explicitement écartée après mesure : 30 s
  pour une seule ligne. Voir `instructions/FEATURE-vues-legacy-prospection.md` ;
- **les procédures stockées, fonctions et le type table `WorkingOutType`** — jamais appelés ;
- **les données**, hormis le semis documenté ci-dessous ;
- **les utilisateurs, rôles et filegroups** — propres à l'installation.

## Le semis, et sa limite

`040_seed_reference.sql` ne contient qu'**une ligne** : la `StrEntity` « ComFolder », la
définition de workflow des dossiers de vente. Les fixtures créent des `StrStatus` en
renseignant `StrConfigEntityId` avec la constante `StatutDossier.CONFIG_ENTITY_ID` ; sans sa
ligne parente, la contrainte `StrStatus__StrConfigEntity_FK1` rejette chacun de ces `INSERT`.

Tout le reste du référentiel est construit par les tests eux-mêmes. En particulier la nature
physique exigée par `StkItemType`/`StkItemCategory` est fournie par
`FixtureReferentielStk.natureParDefaut()`, qui **crée la ligne si elle est absente** au lieu de
la supposer semée — c'est ce qui permet à la suite de tourner indifféremment contre cette base
reconstituée et contre `yesRemsTest`.

## Régénérer

Depuis une base au schéma de référence (ici `yesRemsProd`), sur un poste où le module
PowerShell `SqlServer` est installé. Le script calcule la fermeture, puis produit les trois
fichiers générés en trois passes SMO ; `040_seed_reference.sql` n'est jamais régénéré.

```powershell
Import-Module SqlServer
$conn = New-Object Microsoft.SqlServer.Management.Common.ServerConnection
$conn.ServerInstance = "localhost,49687"
$conn.LoginSecure = $false; $conn.Login = "<user>"; $conn.Password = "<pwd>"
$conn.TrustServerCertificate = $true
$db = (New-Object Microsoft.SqlServer.Management.Smo.Server($conn)).Databases["yesRemsProd"]
# … voir l'historique Git de ce fichier pour le script complet (calcul de fermeture + 3 passes)
```

Points à ne pas perdre en régénérant :

- `NoFileGroup`, `Permissions = $false`, `ExtendedProperties = $false` ;
- **les clés étrangères dans un fichier séparé** — sinon l'ordre de création des tables
  devient contraignant et le script casse au premier cycle (`StkItem` s'auto-référence) ;
- un `GO` après chaque objet : `CREATE TRIGGER` doit ouvrir son propre lot.

Après régénération, valider **avant de committer** : appliquer les quatre fichiers sur une base
neuve, puis lancer `./mvnw test` avec `SPRING_DATASOURCE_URL` pointant dessus.

## Base locale `yesRemsTest`

Tables lues en SQL natif par la vente et absentes de `yesRemsTest` (une commande par table, idempotentes) :

```powershell
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -I -b -Q "IF OBJECT_ID('dbo.SynFolder', 'U') IS NULL CREATE TABLE dbo.SynFolder (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL, ComFolderId uniqueidentifier NULL, StrStatusId uniqueidentifier NULL)"
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -I -b -Q "IF OBJECT_ID('dbo.StlDocument', 'U') IS NULL CREATE TABLE dbo.StlDocument (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL, DocType nvarchar(255) NULL, DocDate datetime NULL)"
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -I -b -i "ci/schema/060_interop_vente.sql"
```

## Reproduire l'environnement du CI en local

```powershell
sqlcmd -S localhost,49687 -U <user> -P <pwd> -C -Q "CREATE DATABASE yesRemsCiCheck"
Get-ChildItem ci/schema/*.sql | Sort-Object Name | ForEach-Object {
  sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsCiCheck -C -I -b -i $_.FullName
}
$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
.\mvnw.cmd test
```

Les variables d'environnement priment sur `src/test/resources/application.properties` dans
Spring Boot : rediriger la suite ne demande **aucune modification de fichier**.
