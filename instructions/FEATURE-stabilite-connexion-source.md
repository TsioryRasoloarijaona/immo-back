# Stabiliser la liaison backend → source de vérité

Backend seulement (immo-crm). Branche `feat/stabilite-connexion-source`.

## Pourquoi

En production, le backend (VPS) joint la source de vérité OMRAN (`192.168.11.115`) **par
internet** : `SOURCE_DB_HOST=197.230.172.136`, port 1433, redirigé par la box du bureau.
Le 08/10/2026 vers 10:04 UTC, la liaison a été coupée, log exact :

```
WARN [onnection-adder] c.m.s.j.internals.SQLServerConnection : ConnectionID:270 …
Prelogin error: host 197.230.172.136 port 1433 Error reading prelogin response: Connection reset
```

La connexion TCP s'ouvre puis est coupée par un équipement du chemin (box, filtrage). On ne
peut pas supprimer la coupure ici ; on peut (1) garder les connexions vivantes pour que la
box ne les « oublie » pas, (2) remplacer une connexion morte avant qu'un utilisateur ne
tombe dessus, (3) absorber les micro-coupures, (4) **dire clairement** à l'utilisateur quand
la source est injoignable — ce matin-là, des saisies ont été crues enregistrées.

## État actuel

`config/DataSourceConfig#ecriture` (pool `hikari-source-de-verite`) ne règle que
`connectionTimeout` (`timeoutConnexionMs`, 5 s) et `maximumPoolSize` (5). URL :
`SourceDeVeriteProperties#urlJdbc()` = `…;encrypt=true;trustServerCertificate=true`.
**Ce pool sert aussi à `SynchronisationDescendante`** (`surSource`) : tout délai par requête
doit laisser passer le cycle de synchronisation le plus long (baseline comprise).
Une base injoignable finit aujourd'hui en 500 générique (rien dans `GlobalExceptionHandler`).

## 1. Réglages du pool d'écriture

Chaque valeur dans `SourceDeVeriteProperties` (préfixe `app.source-de-verite`), surchargeable
par variable d'environnement dans `application.properties`, défaut ci-dessous :

| Propriété | Variable | Défaut | Appliqué à |
|---|---|---|---|
| `keepalive-ms` | `SOURCE_DB_KEEPALIVE_MS` | 60000 | Hikari `keepaliveTime` |
| `duree-vie-max-ms` | `SOURCE_DB_MAX_LIFETIME_MS` | 600000 | Hikari `maxLifetime` |
| `validation-timeout-ms` | `SOURCE_DB_VALIDATION_MS` | 3000 | Hikari `validationTimeout` |
| `tentatives-reconnexion` | `SOURCE_DB_RETRY_COUNT` | 3 | URL `connectRetryCount` |
| `intervalle-reconnexion-s` | `SOURCE_DB_RETRY_INTERVAL_S` | 5 | URL `connectRetryInterval` |
| `timeout-socket-ms` | `SOURCE_DB_SOCKET_TIMEOUT_MS` | voir R4 | URL `socketTimeout` |

Le timeout de connexion existant (`timeoutConnexionMs`) passe aussi dans l'URL comme
`loginTimeout` (en secondes, arrondi au supérieur, minimum 1).

Règles :

- **R1** `keepaliveTime < maxLifetime`, sinon Hikari ignore le keepalive : refuser au
  démarrage (message clair) une configuration qui viole ça ou un keepalive < 30 000 ms
  (minimum Hikari).
- **R2** Le pool garde des connexions prêtes : `minimumIdle` = `taillePool` (défaut Hikari,
  à rendre explicite).
- **R3** L'URL reste `encrypt=true;trustServerCertificate=true` et ne contient jamais le mot
  de passe (`description()` inchangée pour les logs).
- **R4** `socketTimeout` : mesurer d'abord la plus longue requête de
  `SynchronisationDescendante` (baseline incluse — lire le code, et les durées de cycle
  `sync_cycle_duration_seconds` si disponibles), puis choisir un défaut qui la couvre avec
  marge ; noter le chiffre et sa justification ici en §Mesures. À défaut de mesure fiable :
  `0` (désactivé) et le dire dans le rapport.
- **R5** Le pool de lecture (copie locale) **ne change pas**.
- **R6** Mode mono-base (`SOURCE_DB_HOST` vide) : rien de tout ceci ne s'applique ; la suite
  passe sans variable d'environnement nouvelle.

## 2. 503 quand la source est injoignable

Dans `GlobalExceptionHandler`, une erreur d'accès à la base due à la **liaison** (et non à
une requête fautive) répond **503** en `ProblemDetail`, avec la propriété
`code` pour que le front puisse la reconnaître sans lire le texte :

| Cas | Comment le reconnaître | `code` | `detail` |
|---|---|---|---|
| A. Aucune connexion obtenue | la chaîne de causes contient un `SQLTransientConnectionException` (Hikari : pool épuisé / connexion impossible) ou un `CannotGetJdbcConnectionException` / `CannotCreateTransactionException` | `SOURCE_INJOIGNABLE` | « La base OMRAN est injoignable : rien n'a été enregistré. Réessayez dans quelques minutes. » |
| B. Liaison coupée en cours d'opération | une autre exception dont la chaîne contient une `SQLException` de classe connexion (`SQLState` commençant par `08`) ou un `SQLServerException` de connexion fermée/réinitialisée | `LIAISON_COUPEE` | « La liaison avec la base OMRAN a été coupée pendant l'opération. Vérifiez si la saisie a été enregistrée avant de la refaire. » |

- **R7** Le cas A affirme « rien n'a été enregistré » : il ne doit couvrir **que** les cas
  où aucune instruction n'a pu partir. Dans le doute, c'est B (on ne promet rien).
- **R8** Une erreur SQL ordinaire (contrainte, syntaxe, `SQLState` hors `08`) n'est **pas**
  transformée en 503 : comportement actuel conservé.
- **R9** Les 503 sont loggées en `WARN` une ligne (cas, `description()` de la source, message
  racine), sans pile ni mot de passe.
- **R10** Les écritures ne sont **jamais** rejouées automatiquement par le backend (risque de
  doublon de numéro `AdmSequence`). `connectRetryCount` n'agit que sur une connexion inactive
  coupée, ce qui est sûr.
- Documenter la 503 (OpenAPI) au niveau global si le projet a un mécanisme pour les réponses
  communes ; sinon ne pas annoter chaque contrôleur — le signaler dans le rapport.

## Hors périmètre

- Le front (`crm-prototype`) : il affiche déjà le `detail` du `ProblemDetail`. Ne pas y toucher.
- Sondes / Prometheus / Grafana / alertes : feature suivante.
- NetBird, réseau, box : hors code.
- Le pool de lecture, `SynchronisationDescendante` (hors lecture de ses requêtes pour R4).

## Vérification

Tests qui doivent exister et passer :

1. `DataSourceConfig` / `SourceDeVeriteProperties` : avec des propriétés données, le pool
   d'écriture construit a `keepaliveTime`, `maxLifetime`, `validationTimeout`,
   `minimumIdle`, `connectionTimeout` attendus ; l'URL contient `connectRetryCount`,
   `connectRetryInterval`, `loginTimeout`, `socketTimeout` (si ≠ 0), `encrypt=true`, et
   jamais le mot de passe.
2. R1 : keepalive ≥ durée de vie → échec au démarrage avec message ; keepalive < 30 s → idem.
3. `GlobalExceptionHandler` : cas A → 503 + `SOURCE_INJOIGNABLE` ; cas B (SQLState `08S01`)
   → 503 + `LIAISON_COUPEE` ; erreur SQL ordinaire (ex. SQLState `23000`) → **pas** 503.
4. Suite complète verte (`./mvnw test`), nombre de tests noté.

Vérification manuelle (rapportée) : démarrer en mode deux-bases avec `SOURCE_DB_HOST`
pointant un port fermé (ex. `127.0.0.1`, `SOURCE_DB_PORT=1`) et la copie sur `yesRemsTest`,
s'authentifier, faire un `POST` d'écriture → 503 `SOURCE_INJOIGNABLE` en ≈ `timeoutConnexionMs`,
pas une 500, et une lecture (`GET`) qui marche toujours.

## Mesures

### R4 — `socketTimeout` par défaut : **300 000 ms** (5 min)

`socketTimeout` borne l'attente d'**une** lecture sur le socket (pas la durée d'une requête) :
ce qui compte est le plus long silence du serveur avant d'envoyer des octets.

Qui utilise le pool d'écriture hors geste utilisateur (lecture du code) :

| Tâche | Requête la plus longue côté serveur | Mesure (banc, 08/10/2026) |
|---|---|---|
| `SynchronisationDescendante`, cycle delta | `CHANGETABLE` par table, ~18 tables | cycle vide ~2 s (FEATURE-source-de-verite-115.md) |
| idem, **baseline** (`RemonteeLecture#fusionnerTout`) | `SELECT *` de la table, lu en flux | plus grosse table suivie : `ComActionMessage`, 48 221 lignes / 35 Mo ; 1re ligne en **1,3 s** sur `yesRemsProd` |
| **`ArchivageTraceOmran`** (même pool, absent de « État actuel ») | `sys.fn_get_audit_file(...) … ORDER BY event_time` : le serveur lit **tous** les fichiers d'audit et trie avant la 1re ligne | 7 fichiers / 165 Mo dans `C:ackup` : **7,3 s** avant la 1re ligne |

La rotation de l'audit plafonne à 20 × 100 Mo = 2 Go, soit ~12 × le banc : **~90 s** extrapolés
au pire pour la passe d'archivage. `sync_cycle_duration_seconds` n'est pas disponible ici (le
backend local n'était pas en deux-bases, la métrique du VPS n'est pas accessible).

Défaut retenu : 300 s ≈ 3 × le pire extrapolé. Une connexion morte en cours d'opération est
coupée au bout de 5 min au lieu d'attendre indéfiniment. Si l'archivage échoue sur ce timeout
en production (log `Archivage de la trace OMRAN en echec` avec `Read timed out`), monter
`SOURCE_DB_SOCKET_TIMEOUT_MS` ou le mettre à `0`.

### Écarts de mise en œuvre (constatés à la vérification manuelle)

- **Pool d'écriture `initializationFailTimeout = -1`.** Avec le réglage Hikari par défaut, un
  pool qui n'a jamais démarré (source injoignable dès le boot) relance à chaque appel
  l'**erreur brute du pilote** (`SQLServerException`, `08S01`), sans `SQLTransientConnectionException` :
  la requête tombait en cas B. Avec `-1`, Hikari attend `connectionTimeout` et lève
  `SQLTransientConnectionException` → cas A ; le pool se remplit seul au retour de la source.
- **`LazyConnectionDataSourceProxy` : auto-commit `true` et isolation `READ_COMMITTED` déclarés.**
  Sinon, tant qu'il n'a pas pu les lire sur une vraie connexion, le proxy les **sonde à chaque
  `getConnection`**, et hors transaction la sonde part vers la source : chaque lecture payait
  une tentative de connexion à la source (GET 5,9 s avant la feature, 21,8 s avec le `-1`,
  0,03 s une fois les défauts déclarés).
- **R7 appliqué par requête HTTP** : le pool aiguillé « écriture » est enveloppé
  (`ConnexionSourceSuivie`) et note qu'une connexion à la source a été obtenue pendant la
  requête ; un échec de connexion **ultérieur** dans la même requête (ex. `OffrePrixService` :
  offre validée, puis rendu PDF) répond `LIAISON_COUPEE`, jamais « rien n'a été enregistré ».
  Hors requête (tâches planifiées), rien n'est noté. **En mono-base (R6), aucune 503** : le gestionnaire
  relance l'erreur telle quelle (500, comportement antérieur), faute de suivi pour garantir R7.

### Vérification manuelle (08/10/2026)

Deux-bases, `SOURCE_DB_HOST=127.0.0.1`, `SOURCE_DB_PORT=1`, copie sur `yesRemsTest` :
`POST /api/prospects` → **503 `SOURCE_INJOIGNABLE` en 5,0–5,2 s** (`timeoutConnexionMs` = 5 000),
une ligne `WARN` `Base injoignable (SOURCE_INJOIGNABLE) : 127.0.0.1:1/yesRemsTest — …`, mot de
passe absent du journal ; `GET /api/prospects` → **200** (1,6 s à froid, puis 0,03 s).

### OpenAPI

Pas de mécanisme projet pour les réponses communes (`DocsApiMarkdown` n'injecte que des
textes) : la 503 n'est pas annotée par contrôleur. `/api-docs` inchangé (185 opérations, mêmes
codes de réponse que `docs/api-docsV4.json`).

## Impact front

Aucun changement de contrat. Une écriture pendant une coupure renvoie désormais **503**
`ProblemDetail` avec `code` (`SOURCE_INJOIGNABLE` | `LIAISON_COUPEE`) au lieu d'une 500 ; le
front affiche déjà `detail`.
