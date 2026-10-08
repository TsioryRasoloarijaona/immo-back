# FIX — Fiabilité SonarCloud (Quality Gate de `main`)

Backend seulement. Branche `fix/sonar-fiabilite`. **Aucun changement d'API ni de comportement
métier** : chaque correction garde exactement le résultat actuel sur les entrées réelles.

## Pourquoi

SonarCloud, `main` au 08/10/2026 : Quality Gate **ERROR**, une seule condition en échec :
`new_reliability_rating = 3` (C), seuil A. Tout le reste est vert. Les issues de fiabilité du
code récent de gravité ≥ MEDIUM sont la cause ; les 42 `java:S8688` (`.now()` sans fuseau, INFO)
ne dégradent pas la note et sont **hors périmètre** (chantier « horloge Africa/Casablanca »,
bug 66, à part).

## Les corrections (chemins sous `src/main/java/com/tsix/immocrm/`)

| # | Règle | Où | Correction attendue |
|---|---|---|---|
| 1 | `javabugs:S2259` BLOCKER (NPE) | `services/prospection/DiscriminantPreProspection.java:54` `estPreProspection(p)` | `p == null` → `false` (et lecture null-safe de `getStrEntityId()`, déjà via `ID.equals`). |
| 2 | `java:S1872` | `web/LiaisonSource.java:73` (`"com.microsoft.sqlserver.jdbc.SQLServerException".equals(t.getClass().getName())`) | `t instanceof SQLServerException` (driver mssql-jdbc déjà dépendance). Vérifier qu'aucune sous-classe ne change le résultat ; sinon garder l'égalité stricte de classe via `t.getClass() == SQLServerException.class`. |
| 3 | `java:S1872` | `config/apidocs/DocsApiMarkdown.java:305` (`getSimpleName().equals("AuthenticationPrincipal")`) | `a instanceof AuthenticationPrincipal` (spring-security sur le classpath). |
| 4 | `java:S8786` regex à backtracking | `config/apidocs/DocsApiMarkdownConfig.java:74` (`replaceFirst(".*\\.", "")`) | Sans regex : sous-chaîne après le dernier `.` (`lastIndexOf`). |
| 5 | `java:S8786` | `config/apidocs/FichierDocMarkdown.java:20` (`^(#{1,3})\s+(.+?)\s*$`) | Regex sans quantificateurs paresseux en concurrence (ex. `^(#{1,3})\s+(\S.*)$` puis `strip()` du titre) ou parsing manuel ; mêmes titres extraits, mêmes niveaux, `####` reste du texte. |
| 6 | `java:S8700` durée sans fuseau | `services/kpi/PerformanceCommercialService.java:153` | `Duration.between` sur `atZone(ZoneId.of("Africa/Casablanca"))` des deux bornes (heure murale marocaine, changements d'heure du Ramadan). Constante de zone partagée si une existe déjà, sinon une seule constante. |
| 7 | `java:S8700` | `services/dashboard/DashboardService.java:93` | Idem. |
| 8 | `java:S6218` | `mail/PieceJointeEmail.java:7` (record avec `byte[]`) | `equals`/`hashCode` qui comparent le contenu (`Arrays.equals` / `Arrays.hashCode`) ; `toString` inchangé. |

Tests (`src/test/**`), même règle `S8786` : `ProspectionInteractionAlerteTest.java:104`,
`ReglementStatutPreuveTest.java:151,396,420` — simplifier les regex si c'est trivial et sans
changer ce que le test vérifie ; sinon laisser.

Hors code — à marquer « Accepted » dans SonarCloud par le manager, pas à corriger :
`plsql:DeleteOrUpdateWithoutWhereCheck` sur `src/main/resources/db/migration/005_CfgCompany_IsActive.sql:56`
(UPDATE de masse **voulu** : on désactive tout puis on réactive la liste ; voir bug 71).

## Vérification

- Un test par correction quand il est raisonnable : 1 (null → false), 4–5 (mêmes résultats sur
  quelques entrées réelles, dont un titre avec espaces de fin et un `####`), 6–7 (durée inchangée
  hors changement d'heure), 8 (deux pièces au même contenu sont égales).
- Tests ciblés en local (`./mvnw test -Dtest=…`) ; **pas la suite complète en local** — la CI
  GitHub la joue à chaque push.
- Rien d'autre ne change : pas de refactoring opportuniste.
