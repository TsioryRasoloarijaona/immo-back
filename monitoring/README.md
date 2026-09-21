# Monitoring — pipeline de synchronisation descendante

Tableau de suivi du pipeline OMRAN (115) → copie de lecture : **le pipeline est-il
vivant ?** et **tout ce qui est écrit dans la source — par ce backend comme par le
logiciel de bureau — est-il bien importé ?**

## Contenu

- `grafana-sync-pipeline.json` — dashboard Grafana importable
  (`Dashboards → Import → Upload JSON`, puis choisir la datasource Prometheus).

## Prérequis

1. **Backend en mode deux-bases.** Les métriques `sync_*` n'existent que si le backend
   tourne avec `SOURCE_DB_HOST` défini (sinon pas de pipeline, pas de moniteur — voir
   `MoniteurSynchronisation`). Vérifier sur l'hôte :
   ```bash
   curl -s http://127.0.0.1:9091/actuator/prometheus | grep '^sync_'
   ```
2. **Prometheus scrape l'actuator** du backend sur `:9091`, `metrics_path:
   /actuator/prometheus`. Entre conteneurs Docker, viser le **nom de service**
   (`immo-back:9091`), pas `127.0.0.1`.

## Métriques exposées

| Métrique | Type | Sens |
|---|---|---|
| `sync_watermark_lag{table}` | gauge | **retard par table** = version CT source − filigrane copie. `0` = à jour. Signal central. |
| `sync_last_success_timestamp_seconds` | gauge | epoch du dernier cycle ; `time() − valeur` = âge (alerter si muet). |
| `sync_tables_failed` | gauge | tables en échec au dernier cycle. |
| `sync_source_version` | gauge | version CT courante de la source au dernier cycle. |
| `sync_rows_merged_total` / `sync_rows_deleted_total` | counter | lignes fusionnées / suppressions, cumulées. |
| `sync_table_failures_total` | counter | passes de table en échec, cumulées. |
| `sync_cycle_duration_seconds` | timer | durée d'un cycle (`_sum`/`_count` → moyenne ; pas d'histogramme). |

## Alertes recommandées (PromQL)

- Pipeline muet : `time() - max(sync_last_success_timestamp_seconds) > 360` (> 3× la
  cadence de 2 min) pendant 5 min.
- Import en retard **persistant** : `max(sync_watermark_lag) > 0` pendant 10 min (un
  retard transitoire est normal : convergence des clés étrangères au cycle suivant).
- Échec répété : `increase(sync_table_failures_total[15m]) > 0`.

Voir `instructions/FEATURE-moniteur-pipeline-synchro.md` pour le détail.
