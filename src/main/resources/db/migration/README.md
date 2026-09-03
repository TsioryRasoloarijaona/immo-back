# Schéma applicatif (`App*`) — scripts DDL

Ce dossier contient **tout le schéma que l'application ajoute** à la base legacy
`yesRems`. Il n'existe pas ailleurs : `spring.jpa.hibernate.ddl-auto=none` des deux
côtés (main et test), donc **démarrer le backend ne crée rien**.

## Pourquoi des scripts et pas `ddl-auto=update`

`ddl-auto=update` créerait bien `AppFileObject`, mais il ne sait pas se limiter aux
tables applicatives : Hibernate compare **toutes** les entités mappées à la base et
émet des `ALTER TABLE` pour chaque écart. Or une quarantaine d'entités de ce projet
recopient un schéma legacy que l'application ne possède pas.

Ce n'est pas théorique ici : `ComFolder` a porté un temps 8 colonnes
(`Advance`, `ExpenseAmount`, `CreditAmount`…) déduites à tort d'une vue Stimulsoft et
absentes de la table de base. Avec `ddl-auto=none`, cela a produit une erreur nette
(`Invalid column name 'Advance'`) et le mapping a été corrigé. Avec `update`,
Hibernate aurait **ajouté ces 8 colonnes parasites à `ComFolder`**, silencieusement.

Même exposition pour chaque `@Size(max = 255)` qui ne correspond pas à la vraie
longueur `nvarchar`, et chaque `precision = 26, scale = 6`. `update` n'enlève ni ne
rétrécit jamais : les erreurs s'accumulent, sans diff, sans ordre, sans rollback.

## Ordre d'exécution

Les scripts sont **idempotents** (`IF OBJECT_ID(...) IS NULL`) : les relancer sur une
base déjà à jour ne fait rien. À exécuter dans l'ordre numérique.

| # | Script | Contenu |
|---|---|---|
| 001 | `001_AppUserCredential.sql` | Authentification (mot de passe, onboarding, révocation JWT) |
| 002 | `002_AppFileObject.sql` | Pointeur S3 d'une pièce jointe — cœur du stockage objet |
| 003 | `003_AppFileMigrationFailure.sql` | Journal d'échecs de la migration des binaires |

## Exécution

Sur une base fraîchement restaurée depuis un `.bak` de production :

```bash
for f in db/migration/0*.sql; do
  sqlcmd -S "<host>,<port>" -U <user> -P <pwd> -d <base> -C -b -i "$f" || break
done
```

`-b` fait sortir `sqlcmd` en erreur au premier problème ; le `|| break` évite
d'enchaîner sur un script suivant après un échec.

## Avant de lancer sur la restauration de production

Faire d'abord le **diff de schéma** entre la base restaurée et la base de dev
courante. `001` couvre la seule table applicative connue, mais rien ne garantit
qu'aucune *colonne* n'a été ajoutée à la main sur une table legacy au fil des
sessions :

```sql
-- à exécuter sur les deux bases, puis comparer les deux sorties
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
ORDER BY TABLE_NAME, COLUMN_NAME;
```

Toute ligne présente en dev et absente de la restauration est un ajout manuel non
scripté : il doit devenir un `004_*.sql` avant d'aller plus loin.

## Note sur les noms de contraintes

`001` nomme explicitement ses contraintes (`PK_AppUserCredential`,
`UQ_AppUserCredential_AdmUserId`, `FK_AppUserCredential_AdmUser`), alors que la table
créée à la main en développement porte des noms auto-générés
(`PK__AppUserC__5E190D182158A26B`…). Sans conséquence — rien ne référence ces noms —
mais cela explique que les deux bases diffèrent sur ce point précis.
