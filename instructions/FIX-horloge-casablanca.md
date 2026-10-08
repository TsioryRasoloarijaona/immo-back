# FIX — Horloge métier Africa/Casablanca (bug 66 + 42 `java:S8688`)

Backend seulement. Branche `fix/horloge-casablanca`. Aucun changement d'API.

## Pourquoi

- **Bug 66** : le serveur (conteneur du VPS) tourne en UTC. Toutes les dates « maintenant »
  sont prises avec `LocalDateTime.now()` / `LocalDate.now()`, donc à l'heure UTC, alors que la base
  (écrite par OMRAN, au Maroc) stocke l'**heure murale marocaine** et que les écrans raisonnent en
  jours marocains. Entre 00:00 et 01:00 (heure de Casablanca, hors Ramadan), « aujourd'hui » est
  encore hier côté serveur : relances du jour, tableau de bord, dates d'archivage, dates de
  création décalées d'une heure — et pendant le Ramadan (UTC+0) le décalage change.
- **SonarCloud `java:S8688`** : 42 appels `.now()` sans fuseau (34 `LocalDateTime.now()`,
  8 `LocalDate.now()` dans 21 fichiers de `src/main/java`).

## Règles

- **R1 Une seule horloge.** Un bean `java.time.Clock` (ex. `config/HorlogeConfig`) =
  `Clock.system(FuseauMetier.ZONE)` avec une **seule** constante
  `FuseauMetier.ZONE = ZoneId.of("Africa/Casablanca")` (classe du paquet `domain/`). Remplacer la
  constante locale `PerformanceCommercialService.FUSEAU_METIER` par celle-ci.
- **R2 Tous les `.now()` de `src/main/java`** passent par cette horloge : `LocalDateTime.now(clock)`,
  `LocalDate.now(clock)`, injectée par constructeur dans les services. Pour un contexte statique
  sans injection possible, `LocalDateTime.now(FuseauMetier.ZONE)` (jamais l'horloge système nue).
  `Instant.now()` (2 appels) est indépendant du fuseau : le laisser, ou `clock.instant()` si le
  service a déjà l'horloge.
- **R3 Rien d'autre ne change** : mêmes champs écrits, même logique ; seule la source de « maintenant »
  change. Pas de modification des requêtes SQL (`SYSDATETIME()` dans `SynchronisationDescendante`
  est l'heure du serveur SQL, hors périmètre — le noter seulement).
- **R4 Tests** : ne pas multiplier les contextes Spring (aujourd'hui 2 démarrages pour toute la
  suite, à préserver : pas de `@MockitoBean` ni `@DirtiesContext`). Prouver le bug 66 par des tests
  unitaires avec une `Clock.fixed(...)` : à `2026-10-08T23:30:00Z` (= 00:30 le 09/10 à Casablanca,
  UTC+1), « aujourd'hui » vaut le **09/10** pour au moins un calcul « du jour » réel (relances du
  jour / tableau de bord) ; et pendant le Ramadan (UTC+0, vérifier une date de la base tz), le
  décalage suit. Le contexte de test utilise l'horloge réelle Casablanca (bean de prod).

## Vérification

Tests ciblés en local (classes touchées + tests des services modifiés) ; la suite complète tourne
en CI. `grep -rn "\.now()" src/main/java` ne doit plus renvoyer que des `Instant.now()` assumés
(ou rien).

## Revue (08/10/2026)

- **Tests vs horloge** : 162 `now()` côté tests lisent l'horloge système ; le runner CI (UTC) casserait
  la suite chaque soir 23h–0h UTC (reproduit : 13 échecs avec `-Duser.timezone=Pacific/Kiritimati`).
  Corrigé par `TZ: Africa/Casablanca` dans `ci.yml`.
- **À DÉCIDER avant merge — « maintenant » côté SQL Server** : `ComSaleProspectionView` filtre avec
  `GETDATE()` (`DocDate <= GETDATE()` pour dernière action / visites / inactivité, et les alertes).
  Si le SQL Server **source** n'est pas à l'heure de Casablanca (UTC+1 hors Ramadan), une interaction
  écrite par l'appli paraît « dans le futur » pendant l'écart et disparaît de ces colonnes. Fuseau du
  serveur source non documenté ; le SQL Server local répond `(UTC+00:00) Casablanca` avec
  `SYSDATETIME() = SYSUTCDATETIME()` (données de fuseau Windows à vérifier). À mesurer sur la
  source : `SELECT SYSDATETIMEOFFSET(), CURRENT_TIMEZONE()`.
