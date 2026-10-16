# Monitoring — pipeline de synchronisation descendante

Tableau de suivi du pipeline OMRAN (115) → copie de lecture : **le pipeline est-il
vivant ?** et **tout ce qui est écrit dans la source — par ce backend comme par le
logiciel de bureau — est-il bien importé ?**

## Contenu

- `grafana-sync-pipeline.json` — dashboard Grafana importable
  (`Dashboards → Import → Upload JSON`, puis choisir la datasource Prometheus).
- `grafana-vue-generale.json` — vue générale de l'API (santé, HTTP, JVM, pools) ; son
  premier panneau, **« Version déployée »**, affiche le tag de l'image qui tourne
  (`sha-…` = commit de merge sur `main`). Réimporter après chaque modification du JSON.
- `grafana-utilisateurs.json` — **qui utilise Arpent** : connectés maintenant, actifs dans le
  temps, dernière activité par utilisateur, requêtes et routes appelées par utilisateur
  (voir « Suivi des utilisateurs » ci-dessous).

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
| `immo_back_build_info{tag,commit}` | gauge | **version qui tourne** : valeur constante 1, le tag d'image (`sha-…`) et le SHA du commit sont dans les labels. Même chose sur `curl -s http://127.0.0.1:9091/actuator/info`. `inconnu` = image construite hors du pipeline. |
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

## Suivi des utilisateurs

| Métrique | Type | Sens |
|---|---|---|
| `arpent_utilisateur_requetes_total{utilisateur,type,methode,uri}` | counter | requêtes authentifiées ; `uri` = modèle de route (`/api/prospections/{id}`), jamais l'id ; `type` = `session` ou `cle_api` |
| `arpent_utilisateur_derniere_activite_secondes{utilisateur,type}` | gauge | epoch de la dernière requête, flux SSE de notifications exclu ; repart vide au redémarrage (le dashboard lit `max_over_time`) |

Vie privée (loi 09-08) : seuls login, type d'accès, méthode et modèle de route sont collectés —
jamais corps, query string, id, IP ni donnée client. **Informer le personnel avant la mise en
service.**

Mise en service après le merge (le déploiement de l'image suffit côté application) :

1. Vérifier sur l'hôte, après une action dans Arpent :
   ```bash
   curl -s http://127.0.0.1:9091/actuator/prometheus | grep '^arpent_utilisateur'
   ```
2. Grafana → `Dashboards → Import → Upload JSON` → `grafana-utilisateurs.json`, datasource
   Prometheus.
3. Historique = rétention Prometheus (15 j par défaut). Pour comparer des mois, passer
   `--storage.tsdb.retention.time=90d` dans la commande du service Prometheus du compose, puis
   `docker compose up -d prometheus` (≈ quelques Mo pour ~2 000 séries).
