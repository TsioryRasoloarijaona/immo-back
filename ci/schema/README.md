# Schéma de test versionné

Ces fichiers reconstituent, dans un SQL Server vierge, le schéma dont la suite de tests a
besoin. Ils sont consommés par `.github/workflows/ci.yml`, qui monte une base éphémère à
chaque *pull request*.

| Fichier | Contenu | Origine |
|---|---|---|
| `010_tables.sql` | 171 tables : colonnes, clés primaires, contraintes `CHECK`, valeurs par défaut, index | généré (+ 6 ajoutées à la main, voir ci-dessous) |
| `020_foreign_keys.sql` | 577 clés étrangères, dans une passe séparée pour s'affranchir de l'ordre de création | généré (+ 15 ajoutées à la main) |
| `030_triggers.sql` | 4 triggers (`ComFolder`, `RntFolder`) | généré |
| `040_seed_reference.sql` | lignes de configuration indispensables | écrit à la main |
| `050_views.sql` | `ComSaleProspectionView`, les 2 tables qu'elle lit, et son semis | écrit à la main |

Appliquer dans l'ordre numérique. Le CI applique **tous** les `ci/schema/*.sql`, triés par nom
(`LC_ALL=C sort`) : un nouveau fichier n'exige aucune modification de `ci.yml`.

## Convention des lots « Patrimoine à 100 % »

Chaque lot qui crée des tables livre deux fichiers : `0NN_<lot>_tables.sql` (tables, index, types exacts de
`yesRemsProd`) puis `0NN+1_<lot>_fk.sql` (clés étrangères, noms exacts de production). Plages réservées :

| Lot | Fichiers | Lot | Fichiers |
|---|---|---|---|
| P0 | 060-061 | P6 | 074-075 |
| P1a | 062-063 | P3 | 076-077 |
| P1b | 064-065 | P4 | 078-079 |
| P5 | 066-067 | P7a | 080-081 |
| P1c | 068-069 | P8a | 082-083 |
| P2a | 070-071 | P7b | 084-085 |
| P2b | 072-073 | P8b | 086-087 |
| | | P9 | 088-089 |
| | | P10 | 090-091 |

Une FK dont la cible n'est pas dans le schéma de CI est posée sous condition
(`IF OBJECT_ID('dbo.<cible>') IS NOT NULL EXEC('ALTER TABLE …')`) ou omise, et le fichier le dit.

**P0** (`FEATURE-patrimoine-audit-actions.md`) : `060_audit_tables.sql` crée `AdmWebHistorics` (`Details` en
`text`, piège : jamais `@Nationalized` côté entité) et `AdmMessage`. `061_audit_fk.sql` : la seule FK de
production, `AdmWebHistorics__AdmWebSessions_FK1`, n'est **pas** posée en CI (`AdmWebSessions` absente du
schéma) ; `AdmMessage` n'a aucune FK en production. `AuditSchemaTest` échoue en premier si une table manque.

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

## Périmètre : pourquoi 171 tables et pas 462

La graine est l'ensemble des tables mappées par une entité JPA (78), plus
`AppFileMigrationFailure` (table d'outillage de migration, sans entité). On y ajoute la
**fermeture transitive des clés étrangères** : toute table pointée par une table déjà retenue,
sinon les contraintes ne peuvent pas être créées.

**Ajout manuel « Module Chantier, lot 1 »** (`instructions/FEATURE-projet-chantier-lecture.md` §3) : les
6 tables de lignes du module Projet — `PrjMarketLine`, `PrjMarketLineEstimation`, `PrjMarketLineGrouping`,
`PrjWorkingOutLine`, `PrjWorkingOutLineExtra`, `PrjWorkingOutRetainedLine` — avec leurs 15 clés étrangères
(noms exacts de production, y compris les doubles `__`). Leurs cibles (`PrjMarket`, `PrjWorkingOut`,
`BuyItem`, `StlCategory`) étaient déjà dans le périmètre : la fermeture transitive n'ajoute rien. À
conserver lors d'une régénération (elles sont désormais mappées, donc dans la graine).

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

## Base locale

`yesRemsTest` n'a que les tables créées à la main, **sans clé étrangère**. Une table mappée qui
manque se crée avec sa DDL de `010_tables.sql`, **une commande par table** (`-i <fichier>` a déjà
échoué en « Access is denied » : passer la DDL en ligne par `-Q`) :

```bash
sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsTest -C -b -Q "IF OBJECT_ID('dbo.X') IS NULL CREATE TABLE dbo.X (…)"
```

Tables créées ainsi pour le Module Chantier (lot 1, `FEATURE-projet-chantier-lecture.md` §3.2) :
`PrjProject`, `PrjMarketType`, `PrjMarket`, `PrjMarketLine`, `PrjMarketLineGrouping`,
`PrjMarketLineEstimation`, `PrjWorkingOut`, `PrjWorkingOutLine`, `PrjWorkingOutLineExtra`,
`PrjWorkingOutRetainedLine`, `BuyDocument`, `BuyDocumentStatus`, et `StrStatusType` (lue pour le type
des statuts de lot). `StlCategory` y existait déjà. `ChantierSchemaTest` échoue en premier si l'une
manque.

Tables créées ainsi pour le lot P0 (`FEATURE-patrimoine-audit-actions.md`) : `AdmWebHistorics` et `AdmMessage`,
DDL de `060_audit_tables.sql` passée en ligne par `-Q`, une commande par table.

## Reproduire l'environnement du CI en local

```powershell
sqlcmd -S localhost,49687 -U <user> -P <pwd> -C -Q "CREATE DATABASE yesRemsCiCheck"
foreach ($f in (Get-ChildItem ci/schema/*.sql | Sort-Object Name)) {
  sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRemsCiCheck -C -I -b -i $f.FullName
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
