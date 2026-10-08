# REFACTOR — Simplification (maintenabilité SonarCloud)

Backend seulement. Trois lots indépendants, une branche et un worktree chacun :
`refactor/simplification-prospection` (A, `services/prospection`), `refactor/simplification-dossier`
(B, `services/dossier` + `services/encaissement`), `refactor/simplification-divers` (C, le reste,
tests compris). Liste des issues de chaque lot : fichier JSON fourni à l'agent (export SonarCloud
`main` @ 4300c34, 08/10/2026 — 202 issues de maintenabilité ouvertes).

## Règle absolue

**Aucun changement de comportement ni de contrat HTTP.** Mêmes réponses, mêmes erreurs, mêmes
écritures en base, même OpenAPI (`/api-docs` identique : pas de paramètre renommé, pas de DTO
modifié). Un refactoring qui exigerait de toucher le contrat est **sauté** et listé.

## Comment traiter chaque règle

| Règle | Traitement |
|---|---|
| `java:S3776` complexité cognitive | Extraire des méthodes privées nommées en français métier (skill `java-refactoring-extract-method`), early returns ; pas de nouvelle classe sauf si évidente. |
| `java:S1192` chaîne dupliquée | Constante `private static final` dans la classe (ou constante déjà existante ailleurs si c'est la même notion). Pas de constante pour des fragments SQL/JPQL qui perdraient leur lisibilité — à sauter. |
| `java:S107` trop de paramètres | Méthodes **internes** : regrouper en `record` privé/paquet (skill `java-refactoring-remove-parameter`). **Méthodes de contrôleur** (paramètres `@RequestParam`) et signatures publiques consommées par des contrôleurs : **sauter** (le contrat HTTP et l'OpenAPI en dépendent) et lister pour acceptation. |
| `java:S3358` ternaires imbriqués | `if/else` ou méthode extraite. |
| `java:S9391`, `S6809`, `S135`, `S127`, `S1172`, `S2629`, `S112`, `S3457`, `S8694`, `S1905`, `S5411`, `S5778`, `S5838`, `S5853`… | Appliquer la correction standard de la règle si elle est locale et sûre ; sinon sauter et lister. `S2629` (log) : arguments paramétrés `{}`. `S5778`/`S5838`/`S5853` (tests) : forme AssertJ/JUnit recommandée sans affaiblir l'assertion. |

Skills disponibles : `java-refactoring-extract-method`, `java-refactoring-remove-parameter`,
`java-best-practices` (repo), et `/simplify` pour la relecture finale du diff.

## Garde-fous

- Ne pas toucher aux fichiers d'un autre lot (conflits de merge) : chaque agent reste dans son lot.
- Commentaires : règle 11 du `CLAUDE.md` du repo (pièges seulement, ≤ 3 lignes).
- Tests ciblés uniquement en local (`./mvnw test -Dtest=…` sur les classes qui couvrent les fichiers
  touchés) ; la suite complète tourne en CI. Les trois agents partagent `yesRemsTest` : ne jamais
  lancer la suite complète.
- Rapport : pour chaque issue du lot, `fait` / `sauté (raison)`.
