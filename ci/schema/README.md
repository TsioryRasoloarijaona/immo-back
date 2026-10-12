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

Appliquer dans l'ordre numérique.

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

S'y ajoutent les références qu'OMRAN écrit sur chaque dossier, échéance et règlement
(`FEATURE-interop-omran-ecritures.md` §2.4) : `StrEntity` `StlSettlementIn`/`StlSettlementOut`,
`AdmModule` `COM`, `StlCurrency` `MAD`, et la table `AdmParamsGuid` (hors fermeture des clés
étrangères, donc créée ici) avec son paramètre `DefaultCurrency`. PKey de production, reprises
dans `ReferentielSeme`.

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

## Reproduire l'environnement du CI en local

```powershell
sqlcmd -S localhost,49687 -U <user> -P <pwd> -C -Q "CREATE DATABASE yesRemsCiCheck"
foreach ($f in @("010_tables","020_foreign_keys","030_triggers","040_seed_reference","050_views")) {
  sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsCiCheck -C -I -b -i "ci/schema/$f.sql"
}
$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
.\mvnw.cmd test
```

Les variables d'environnement priment sur `src/test/resources/application.properties` dans
Spring Boot : rediriger la suite ne demande **aucune modification de fichier**.

## Base locale

`yesRemsTest` n'a pas les tables `AdmModule`, `StlCurrency` et `AdmParamsGuid`, que le code lit
pour résoudre ses références (sans ligne, il se replie sur les PKey de production). Les créer une
fois, une commande par table (DDL de `010_tables.sql`, et de `040_seed_reference.sql` pour
`AdmParamsGuid`) :

```powershell
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -b -Q "IF OBJECT_ID('dbo.AdmModule') IS NULL CREATE TABLE dbo.AdmModule (PKey uniqueidentifier NOT NULL CONSTRAINT AdmModule_PK PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL, Sorting int NULL, ObjectForm nvarchar(255) NULL, IsActive bit NULL)"
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -b -Q "IF OBJECT_ID('dbo.StlCurrency') IS NULL CREATE TABLE dbo.StlCurrency (PKey uniqueidentifier NOT NULL CONSTRAINT StlCurrency_PK PRIMARY KEY, Code nvarchar(255) NULL, Description nvarchar(255) NULL, CodeIso nvarchar(255) NULL, Notes nvarchar(500) NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, DescriptionLong nvarchar(500) NULL, CurrencyFormat nvarchar(255) NULL, CurrencySymbol nvarchar(255) NULL, CurrencyDecimalDigits int NULL)"
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -b -Q "IF OBJECT_ID('dbo.AdmParamsGuid') IS NULL CREATE TABLE dbo.AdmParamsGuid (Code nvarchar(255) NOT NULL CONSTRAINT AdmParamsGuid_PK PRIMARY KEY, CodeValue uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL, PKey uniqueidentifier NULL, StrConfigEntityId uniqueidentifier NULL)"
```
