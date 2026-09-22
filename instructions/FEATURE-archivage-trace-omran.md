# FEATURE — Archivage de la trace d'audit OMRAN avant rotation

## Problème

L'audit SQL Server `Audit_Global` (fichiers `C:\backup\*.sqlaudit`) capte chaque requête
d'OMRAN Estate — **la** source de vérité sur les colonnes réellement lues/écrites par chaque
écran du légataire, celle qui a cadré les champs obligatoires de la prospection et les lots de
fiches. Mais il est configuré en rotation : `MAX_ROLLOVER_FILES = 20`, `MAXSIZE = 100 MB`.
Au-delà de 20 fichiers, SQL Server **supprime les plus anciens**.

Mesuré le 22/09/2026 sur le banc local :

- 4 fichiers, 129,6 Mo — un fichier a déjà atteint 100 Mo et tourné (le 21/09) ;
- la spécification (`Audit_DML_DDL_Spec` sur `yesRems`) capte **tout** le DML, donc le bruit
  consomme la fenêtre : 23 943 lignes JDBC (la synchro du backend), 10 517 MySQL Workbench,
  3 104 SQLCMD… pour **42 793 lignes OMRAN** (depuis le 11/09) ;
- à ce rythme (~100 Mo / 4 jours avec la synchro active), la fenêtre complète représente
  quelques semaines — après quoi la trace OMRAN la plus ancienne est détruite.

## Solution

Un cycle planifié dans le backend, `config/ArchivageTraceOmran`, dans le moule de
`SynchronisationDescendante` :

- **lit la source** (`sys.fn_get_audit_file(chemin, DEFAULT, DEFAULT)` — la fonction répond
  pour l'instance où OMRAN écrit et exige `CONTROL SERVER`), filtre
  `application_name = 'OMRAN estate'` **côté serveur** — seules les lignes OMRAN traversent ;
- **écrit la copie** (`jdbcLecture`) : table `AppTraceOmran`
  (`db/migration/010_AppTraceOmran.sql`), à appliquer sur les copies uniquement
  (yesRemsProd, yesRemsDev), jamais sur la source ;
- **reprise incrémentale par purge-et-rejeu de la borne** — le point le plus travaillé de la
  feature, parce que **l'audit n'offre aucune clé naturelle, et c'est mesuré** : une requête
  sur une vue produit un enregistrement par objet référencé (même `audit_file_offset`, même
  instant, même statement — seule `object_name` change), et **18 848 lignes du fichier brut
  sont strictement identiques sur toutes les colonnes** (relevé du 22/09/2026). Dédupliquer
  par clé perdrait donc de vrais enregistrements. À la place :
  1. filigrane = `MAX(EventTimeUtc)` archivé ; la passe **purge** de l'archive les lignes de
     ce tick puis relit l'audit en `>=` — le tick frontière est réarchivé entier à chaque
     passe (obligatoire : `batchUpdate` commite instruction par instruction, un échec à
     mi-lot laisse un tick à moitié archivé) ;
  2. lecture **ordonnée par `event_time`** — un échec à mi-passe laisse un préfixe
     *temporel*, que `>=` prolonge sans trou ;
  3. **borne haute de sécurité** `event_time <= SYSUTCDATETIME() − 5 min` (horloge de la
     source, celle qui horodate l'audit) — on ne lit jamais un tick encore en cours
     d'écriture (`QUEUE_DELAY` de l'audit : 1 s) ;
- **échec absorbé** : journalisé, jamais propagé — la passe suivante rejoue la borne et
  converge. La rotation laisse des semaines de marge à la cadence par défaut (60 min).
  Cas résiduel accepté : si le backend reste arrêté au-delà de la fenêtre de rotation, le
  tick frontière purgé peut ne plus être relisible — négligeable devant le trou de plusieurs
  semaines que la rotation aurait de toute façon creusé.

### Colonnes archivées

`EventTimeUtc` (l'audit horodate en **UTC**, décalage d'une heure constaté), `ActionId`
(normalisé des espaces de complétion : `'SL  '` → `'SL'`), `Succeeded`, `PrincipalNom`,
`BaseNom`, `SchemaNom`, `ObjetNom`, `Statement` (l'audit tronque à 4 000 caractères, la liste
de colonnes d'un INSERT est celle de l'ORM d'Omran, pas les champs réellement renseignés),
`ClientIp`, plus la position physique `(FichierNom, FichierOffset, SequenceNo)` — gardée pour
le diagnostic, **pas comme clé** (voir ci-dessus) — et `ArchiveLe`. Clé technique `PKey`
(identity), aucune contrainte d'unicité.

### Activation

Deux conditions, toutes deux nécessaires :

| Propriété | Variable | Défaut | Rôle |
|---|---|---|---|
| `app.source-de-verite.host` | `SOURCE_DB_HOST` | vide | mode deux-bases (scheduler + accès nommé à la copie) |
| `app.trace-omran.chemin` | `AUDIT_TRACE_PATH` | vide | chemin des fichiers d'audit **sur la source**, ex. `C:\backup\*.sqlaudit` |
| `app.trace-omran.minutes` | `AUDIT_TRACE_MINUTES` | 60 | cadence du cycle |

Chemin vide = rien n'est instancié : la suite de tests et un déploiement dont la source n'a
pas d'audit configuré ne changent pas de comportement.

**En production (source = 115) : vérifier qu'un audit y est configuré** (`SELECT name,
is_state_enabled FROM sys.server_audits` — non vérifiable d'ici, le mot de passe `sasa` est
détenu par l'utilisateur). S'il n'y en a pas, le poser (même `CREATE SERVER AUDIT` que
localement, chemin local au serveur) est un préalable à `AUDIT_TRACE_PATH` en prod — et le
chemin est résolu **par l'instance SQL du 115**, pas par le conteneur du backend.

## Vérification

- `ArchivageTraceOmranTest` (unitaire pur, comme `SynchronisationDescendanteTest`) : borne
  large (`>=`) et ses paramètres, lecture ordonnée, borne haute, filtre OMRAN côté serveur,
  ordre des paramètres d'insertion, normalisation d'`action_id`.
- Banc local (22/09/2026) : migration 010 appliquée sur `yesRemsProd`, première passe rejouée
  en T-SQL (la même requête que le cycle), puis seconde passe (purge-et-rejeu de la borne) —
  comptes identiques à l'audit brut, aucun doublon, aucun trou.

## Impact front

Aucun. Aucun endpoint ajouté ni modifié ; la table `AppTraceOmran` est un outil d'analyse
interne, hors API.
