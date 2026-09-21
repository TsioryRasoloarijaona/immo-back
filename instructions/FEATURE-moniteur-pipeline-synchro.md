# FEATURE — Moniteur du pipeline de synchronisation (métriques Micrometer)

Branche : `feat/moniteur-pipeline-synchro` (coupée de `main`).

## But

Un tableau de suivi qui répond à deux questions :

1. **Le pipeline est-il vivant ?** (un cycle de synchro tourne-t-il encore ?)
2. **Tout ce qui est écrit dans la source (115) — par ce backend comme par OMRAN — est-il
   bien importé dans la copie de lecture ?**

Les deux flux d'écriture convergent au même point : ils passent tous les deux par le Change
Tracking de la source, donc le **retard par table** (version CT courante − filigrane copie) est
le signal unique de « bien importé ». `0` = à jour ; un retard qui persiste = une table
bloquée.

## Ce qui a été fait — aucune dépendance ajoutée

`spring-boot-starter-actuator` + `micrometer-registry-prometheus` étaient déjà au `pom.xml`,
et `/actuator/prometheus` déjà exposé (port de management **9091**). On a seulement
**instrumenté le cycle** :

- `config/MoniteurSynchronisation.java` — composant deux-bases (même `@ConditionalOnExpression`
  que la synchro : en mono-base, pas de pipeline, pas de métriques) qui porte les mètres.
- `config/SynchronisationDescendante.java` — appelle `moniteur.enregistrerCycle(...)` en fin de
  chaque cycle (planifié **et** manuel `POST /api/synchronisation`), en lui passant le retard
  par table calculé depuis `AppSyncEtat`. L'échec du moniteur ne fait jamais échouer un cycle.

## Métriques exposées (`/actuator/prometheus:9091`)

| Métrique Prometheus | Type | Sens |
|---|---|---|
| `sync_cycle_duration_seconds` | timer | durée d'un cycle |
| `sync_rows_merged_total` | counter | lignes fusionnées dans la copie, cumulées |
| `sync_rows_deleted_total` | counter | suppressions répercutées, cumulées |
| `sync_table_failures_total` | counter | passes de table en échec, cumulées |
| `sync_tables_failed` | gauge | tables en échec au **dernier** cycle |
| `sync_last_success_timestamp_seconds` | gauge | epoch du dernier cycle exécuté |
| `sync_source_version` | gauge | version CT courante de la source au dernier cycle |
| `sync_watermark_lag{table="…"}` | gauge | retard par table (0 = à jour) |

## Panels Grafana / PromQL

- **Pipeline vivant** (stat, rouge si > 6 min) :
  `time() - sync_last_success_timestamp_seconds`
- **Tables en retard** (table/heatmap, tout ce qui est > 0 est suspect) :
  `sync_watermark_lag > 0`
- **Tables en échec au dernier cycle** (stat, rouge si > 0) :
  `sync_tables_failed`
- **Débit** (time series) :
  `rate(sync_rows_merged_total[5m])`, `rate(sync_rows_deleted_total[5m])`
- **Durée de cycle** (p95) :
  `histogram_quantile(0.95, rate(sync_cycle_duration_seconds_bucket[5m]))`

### Alertes recommandées
- Pipeline muet : `time() - sync_last_success_timestamp_seconds > 360` (6 min, soit 3× la
  cadence de 2 min) pendant 5 min.
- Import en retard : `max(sync_watermark_lag) > 0` pendant 10 min (un retard transitoire est
  normal — convergence FK au cycle suivant ; c'est la **persistance** qui alerte).
- Échec répété : `increase(sync_table_failures_total[15m]) > 0`.

## Déploiement / réseau

L'actuator est sur **9091**, publié sur `127.0.0.1` uniquement (voir `application.properties`
et `deploy.yml`) — donc **non exposé à internet**. Prometheus doit atteindre `:9091` par le
réseau privé / le VPN NetBird, pas par `api.tsiory.tech` (qui ne forwarde que 8080). Les
métriques n'existent qu'en **mode deux-bases** (`SOURCE_DB_HOST` défini) — cohérent, puisque
c'est le seul mode où le pipeline tourne.

## Fichiers

- `config/MoniteurSynchronisation.java` — les mètres + `enregistrerCycle`.
- `config/SynchronisationDescendante.java` — injection + appel + `retardsParTable`.
- `test/.../config/MoniteurSynchronisationTest.java` — 3 cas (SimpleMeterRegistry, sans Spring).
