# Session Memory
_Last updated: 2026-09-05T23:08:15+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective
Le backend est **conteneurisé, publié sur GHCR et déployé en production** sur le VPS, avec un
pipeline GitHub Actions **validé de bout en bout** (PR #9 mergée → build → push → déploiement →
sonde verte).

Chantier en cours : **monitoring unifié**. Les **phases 0 à 2 sont installées et vérifiées sur le
VPS** — logs des 8 conteneurs indexés dans Loki, Prometheus scrapant cAdvisor, node-exporter et
MinIO, Grafana provisionné. La **phase 3** (métriques applicatives) est **écrite et testée sur
`feat/metriques-prometheus`, non mergée** : elle attend la PR puis deux gestes côté VPS.

---

## ✅ What Worked
- **Inspecter l'image plutôt que relire le Dockerfile.** `docker run --entrypoint sh … -c "ls /app"`
  a révélé en une commande que l'extraction produisait `app.jar` + `lib/` et non une arborescence
  explosée. Aucune relecture n'aurait donné ça.
- **Le lancement à blanc comme test du Dockerfile** : `docker run --rm immo-back:local` **doit**
  échouer, et c'est le *message* qui valide. `Could not resolve placeholder 'JWT_SECRET'` avec un
  chemin `jar:file:/app/app.jar!/…` prouve que la JVM démarre, que les couches sont assemblées et
  que l'application est trouvée.
- **Même requête, origine différente** — l'expérience qui a tranché le diagnostic production :
  préflight `403` depuis `immo.tsiory.tech`, `200` depuis `localhost:5173`. Un seul paramètre varie.
- **Vérifier la fraîcheur des rapports Surefire** (`find target/surefire-reports -mmin -10`) avant
  de conclure : un `-Dtest=X` laisse en place les rapports d'une exécution antérieure, et les lire
  ferait croire à une suite complète verte.
- **Interroger le registre plutôt que le cache local** (`docker manifest inspect`) pour prouver
  qu'un push a réellement abouti.
- Suite complète : **473/473** (469 avant, +4 nouveaux tests).

## ❌ What Failed
- **`docker build … 2>&1 | tail -60`** → **faux vert**. Le code de sortie d'un pipeline est celui
  de la *dernière* commande. Le harnais a rapporté `exit 0` alors que le daemon était éteint et
  que rien n'avait été construit. **Rediriger (`> log 2>&1`), jamais `| tail`, quand le code de
  sortie compte.**
- **`docker --version` comme test de disponibilité** → répond même daemon arrêté (client seul).
  Le vrai test est **`docker info`**.
- **Transposer l'`ENTRYPOINT` de `layertools` vers `tools extract`** sans vérifier le format
  produit → `ClassNotFoundException: …JarLauncher`.
- **Casse du `LABEL org.opencontainers.image.source`** (`tsioryR…` au lieu de `TsioryR…`) → package
  GHCR non rattaché au dépôt.
- **`mv deploy_key .ssh/authorized_keys`** → `authorized_keys` écrasé par une clé **privée**,
  verrouillage évité de justesse. Un `authorized_keys` ne contient que des clés **publiques**, et
  on y ajoute avec **`>>`**.
- **Trois secrets collés en clair dans la conversation** (un PAT GHCR, deux clés privées SSH) →
  tous à considérer comme brûlés. La parade retenue : générer sur le poste et transférer par
  presse-papier (`Get-Content … -Raw | Set-Clipboard`), sans jamais afficher la privée.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | `mvnw` en `100644` → `Permission denied` dans le conteneur | Fixed | `RUN chmod +x mvnw` |
| 2 | `-Djarmode=layertools` retiré en Spring Boot 4 | Fixed | `-Djarmode=tools … extract --layers` |
| 3 | `ClassNotFoundException: …JarLauncher` | Fixed | Sans `--launcher`, pas de loader : `ENTRYPOINT java -jar app.jar` + `--application-filename app.jar` |
| 4 | `WORKDIR .` compilait dans `/` | Fixed | `WORKDIR /build` |
| 5 | Package GHCR non rattaché au dépôt | Fixed | Casse du `LABEL` corrigée + rattachement manuel dans Package settings |
| 6 | `depends_on.0 must be a string` | Fixed | Forme courte (`- sqlserver`) sans `condition:` |
| 7 | `Cannot open database "dev"` | Fixed | `DB_NAME=yesRemsDev` — le `.env` du VPS contenait `DB_NAME=dev` |
| 8 | Préflight CORS `403` depuis `immo.tsiory.tech` | Fixed | Origine ajoutée — déployé via PR #9 |
| 9 | `/actuator/health` en `401`, sonde impossible | Fixed | `permitAll` chemin exact — déployé via PR #9 |
| 10 | `authorized_keys` écrasé par une clé privée | Fixed | Réécrit avec la publique personnelle + la clé de déploiement |
| 11 | Login prod `401` (ancienne version déployée) | Fixed | Par l'utilisateur |
| 12 | Pas de rotation des logs Docker dans le compose du VPS | Fixed | `logging: max-size 10m / max-file 3` sur chaque service |
| 13 | `spring.jpa.show-sql=true` actif en production | Fixed | `SPRING_JPA_SHOW_SQL: "false"` en variable d'environnement du compose — **aucune modification de code**, le relaxed binding suffit |
| 14 | `additional properties 'SPRING_JPA_SHOW_SQL' not allowed` | Fixed | Les variables applicatives vont **sous `environment:`**, jamais directement sous le service |
| 15 | Montage `prometheus.yml` : `not a directory` | Fixed | Docker **crée un répertoire** quand la source d'un bind mount n'existe pas. Écrire le fichier **avant** le `up`, et le vérifier au `file` |

---

## 🔑 Key Points to Remember

### Conteneurisation — Spring Boot 4
- **`layertools` est retiré.** Le remplaçant `tools extract` change *le contenu* des couches :
  **sans `--launcher`**, il produit un jar exécutable ordinaire (`Main-Class` + `Class-Path`
  relatif) plus un répertoire `lib/`. **Aucun `JarLauncher` n'est extrait**, et
  `spring-boot-loader/` sort **vide** — ne pas le `COPY`. L'entrypoint est un simple
  `java -jar app.jar`.
- **`--application-filename app.jar`** fige le nom : par défaut il porte la version du projet, ce
  qui casserait l'`ENTRYPOINT` à chaque montée de version.
- Runtime en **`jre`-alpine** (~150 Mo de moins que `jdk`) : vérifié sans risque, les 10
  générateurs PDF n'importent que `java.awt.Color`, jamais `Font`/`Graphics2D`/`ImageIO`, donc
  pas de fontconfig requis sur musl.
- **Image : 478 Mo locale, ~200 Mo compressés au premier push.** Les pushes suivants coûtent
  **~1 Mo** tant que `pom.xml` ne bouge pas : seule la couche `application/` change. C'est
  l'ordre des `COPY` qui achète ça.

### GHCR
- **Le propriétaire doit être en minuscules** dans le nom d'image (`tsioryrasoloarijaona`), alors
  que le `-u` du `docker login` accepte les majuscules.
- Le **`LABEL org.opencontainers.image.source` est comparé de façon sensible à la casse** pour
  rattacher le package au dépôt. Le rattachement n'est pas cosmétique : il conditionne les
  permissions, donc la capacité d'Actions à pousser.

### Architecture du VPS
- **Tout est en conteneur** dans un seul compose (`~/app`, utilisateur `prod`) : `minio`,
  `sqlserver`, `immo-back`. **Caddy tourne sur l'hôte**, pas en conteneur.
- Les trois services publient sur **`127.0.0.1`** uniquement — le piège du contournement d'`ufw`
  par les règles iptables de Docker est déjà évité. **Ne jamais publier en `0.0.0.0`.**
- Caddy : `api.tsiory.tech → 127.0.0.1:8080`, `s3.tsiory.tech → :9000`,
  `console-s3.tsiory.tech → :9001`. ⚠️ Le compose annonce
  `MINIO_BROWSER_REDIRECT_URL=https://console.s3.tsiory.tech` (**point**) alors que Caddy sert
  `console-s3` (**tiret**) — incohérence non corrigée.
- Le backend joint la base par **nom de service** (`DB_HOST=sqlserver`), donc le port `1433` codé
  en dur dans `application.properties` **tombe juste**. Base : **`yesRemsDev`**.
- `MSSQL_PID: Express` plafonne chaque base à **10 Go**.

### Stockage objet — le piège du endpoint
- **`S3_ENDPOINT` doit rester l'URL publique `https://s3.tsiory.tech`**, même si MinIO est le
  conteneur voisin. Une seule propriété alimente les deux beans (`StockageConfig` l.46-47 et
  l.58-59), et **SigV4 signe l'en-tête `Host`** : une URL présignée pour `minio:9000` serait
  irrésolvable par le navigateur et non réécrivable.
- Répartition réelle : `S3Client` ne fait que `putObject` et `deleteObject` ; **toutes les
  lectures passent par le presigner** (URL consommées directement par le navigateur).
- **Amélioration identifiée, non faite** (~6 lignes) : ajouter `app.stockage.endpoint-interne`,
  utilisée par le seul `s3Client`, pour que les dépôts restent sur le réseau Docker.
  `pathStyleAccessEnabled(true)` est déjà posé sur les deux beans, donc `http://minio:9000`
  s'adresserait correctement.

### Sécurité applicative
- **`allowCredentials(true)` interdit le joker `*`** : chaque origine de production doit être
  listée dans `CorsConfig`. Un nouveau domaine = une modification de code, sans échappatoire par
  configuration.
- **`/actuator/health` est en `permitAll`, chemin exact — pas `/**`.** `/actuator/metrics` reste
  authentifié (il exposerait l'inventaire des URI et les latences par endpoint). Verrouillé par
  `ActuatorSecuriteTest`, qui teste les deux moitiés ensemble.
- Un préflight CORS refusé renvoie **`403 Invalid CORS request`**. Symptôme côté front :
  tout ce qui préflighte (POST JSON, PUT, PATCH, DELETE — **dont le login**) échoue, les GET
  simples « ne répondent pas » (réponse jetée par le navigateur, faute d'en-tête `ACAO`), et
  seul `/api-docs` marche car il est hors du mapping `/api/**`.

### Le pipeline
- `.github/workflows/deploy.yml` : `push: main` + `workflow_dispatch`, `concurrency: deploy-main`.
- **Le tag `sha-<7>` est le mécanisme de déploiement et de rollback** : le job réécrit
  `IMMO_BACK_TAG` dans le `.env` du VPS, puis `docker compose pull && up -d immo-back`.
  ⚠️ Suppose que le compose utilise `image: …:${IMMO_BACK_TAG:-latest}` — un `:latest` figé
  ferait échouer le déploiement **en silence**.
- **`up -d` ne tire l'image que si elle est absente localement** : sans `pull` explicite, un
  nouveau `latest` n'est jamais récupéré.
- La dernière étape **sonde `/actuator/health` jusqu'à 90 s** et affiche les logs en cas d'échec.
  Sans elle, le job serait vert avec un conteneur en boucle de crash.
- **Les tests ne tournent pas en CI** — la suite exige un SQL Server réel (`yesRemsTest`) qu'un
  runner GitHub n'a pas, et le `Dockerfile` construit avec `-DskipTests`. **Le pipeline déploie
  donc du code non testé** ; la validation reste manuelle avant le merge. C'est le vrai trou.
- **Pas d'outil de migration de schéma** (`ddl-auto=none`, tables applicatives créées par script
  SQL manuel). Un merge qui attend une nouvelle colonne partirait en boucle de crash tout seul :
  ces déploiements-là doivent rester manuels.
- Secrets de dépôt utilisés : `VPS_HOST`, `VPS_USER`, `VPS_PATH`, `VPS_SSH_KEY`, et `TOKEN` pour
  le login GHCR.

---

## 📁 Important Files & Paths
- `Dockerfile` — multi-stage, build `jdk-alpine` → runtime `jre-alpine`, user non-root, 3 `COPY`
  de couches, `ENTRYPOINT java -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -jar app.jar`.
- `.dockerignore` — contexte ramené de ~57 Mo à ~5 Mo ; exclut `.env*`.
- `.env.example` — gabarit du `--env-file` local, documente le format (pas de guillemets, pas de
  commentaire en fin de ligne) et l'astuce `SPRING_DATASOURCE_URL`.
- `.github/workflows/deploy.yml` — build+push GHCR puis déploiement SSH et sonde.
- `src/main/java/com/tsix/immocrm/web/CorsConfig.java` — origines autorisées.
- `src/main/java/com/tsix/immocrm/security/SecurityConfig.java` — le javadoc distingue désormais
  l'exception **non applicative** (`/actuator/health`) de celles que l'avertissement vise.
- `src/test/java/com/tsix/immocrm/security/ActuatorSecuriteTest.java` — **sans `@WithMockUser`**,
  c'est l'accès anonyme qui est sous test.
- `src/main/java/com/tsix/immocrm/storage/StockageConfig.java` — les deux beans S3, une seule
  propriété d'endpoint.

---

## 🔧 Environment & Config
- **VPS** (`vmi3546129`, user `prod`, `~/app`) : **11 Go RAM, 7,4 Go disponibles**, 197 Go de
  disque à **15 %**, **aucun swap**. Conteneurs : `immo-back` 928 Mo, `sqlserver` 1,28 Go,
  `minio` 494 Mo.
- **Image publiée** : `ghcr.io/tsioryrasoloarijaona/immo-back`, tags `latest` et `sha-7a583d7`,
  digest `sha256:c075e59c…`. Package **privé**.
- **Deux PAT distincts** : Actions pousse (secret `TOKEN`), le VPS tire (`read:packages` seul).
  Sur Linux, `~/.docker/config.json` stocke le token en **base64, non chiffré** → `chmod 600`.
- **Poste local** : Docker Desktop (client+daemon 29.7.2) ; SQL Server local sur **`49687`**
  (`yesRemsProd`, `yesRemsTest`, `yesRems`, `ayd-db`) ; tunnel SSH `vps-db` vers `yesRemsDev`,
  lié à `127.0.0.1` donc **invisible depuis un conteneur** (rebinder en `0.0.0.0` si besoin).
- **Git** : `main` à `11c330b` (merge PR #9), arbre propre, rien en attente.

---

## ⏭️ Next Step
> **Pousser `feat/metriques-prometheus`, ouvrir la PR, la faire merger — puis deux gestes sur
> le VPS.** La branche est commitée et la suite est verte (473/473) ; rien n'est poussé.
>
> Elle contient : `micrometer-registry-prometheus` (runtime, résolu en **1.17.0**),
> `management.server.port=9091`, `prometheus` ajouté à `exposure.include`, la sonde du workflow
> passée de `:8080` à `:9091`, et les javadocs de `SecurityConfig`/`ActuatorSecuriteTest` mis en
> cohérence.
>
> **Après le merge, sur le VPS :**
> 1. ajouter `- "127.0.0.1:9091:9091"` aux `ports:` du service `immo-back` ;
> 2. décommenter le job `immo-back` dans `monitoring/prometheus/prometheus.yml`, puis
>    `docker compose restart prometheus`.
>
> ⚠️ **`https://api.tsiory.tech/actuator/health` ne répondra plus** — l'actuator quitte le port
> 8080 que Caddy proxifie. La sonde devient interne, sur `127.0.0.1:9091`. C'était notre
> vérification publique la plus commode : ne pas conclure à une panne en la voyant échouer.

### État du monitoring (phases 0-2 : installées et vérifiées)
- Compose du VPS : `loki`, `alloy`, `grafana`, `prometheus`, `cadvisor`, `node-exporter`.
  Configs sous `~/app/monitoring/{loki,alloy,prometheus,grafana}`.
- **Jalon logs atteint** : `wget -qO- localhost:3100/loki/api/v1/label/container/values` depuis
  le conteneur `loki` renvoie les 8 conteneurs. C'est la commande qui dit si Alloy lit vraiment
  le socket Docker.
- **Jalon métriques** : `wget -qO- 'localhost:9090/api/v1/targets?state=active'` depuis
  `prometheus`. `"health":"unknown"` = pas encore scrapée (transitoire), pas en échec.
- `MINIO_PROMETHEUS_AUTH_TYPE: public` requis, sinon MinIO réclame un jeton et ressort en `401`.
- Tableaux de bord Grafana à importer par ID : **193** (cAdvisor), **1860** (Node Exporter),
  **13502** (MinIO).
- Restant à confirmer : DNS + Caddy pour `monitoring.tsiory.tech`. À défaut, tunnel
  `ssh -N -L 3000:127.0.0.1:3000 prod@api.tsiory.tech`.

### Backlog
- [ ] **Révoquer le PAT GHCR collé en clair** dans la conversation, s'il ne l'est pas déjà.
- [ ] **`healthcheck` sur le service `sqlserver`** + `depends_on: condition: service_healthy`.
      Reporté volontairement : l'ajouter recrée le conteneur de la base (30-60 s de coupure).
- [ ] **Tester le hairpin** : `docker exec immo-back wget -S -O /dev/null https://s3.tsiory.tech`.
      Jamais fait. S'il échoue, seuls les **uploads** cassent — panne partielle très trompeuse.
- [ ] **Dissocier l'endpoint S3 interne** (~6 lignes, voir Key Points).
- [ ] **Corriger `console.s3` → `console-s3`** dans `MINIO_BROWSER_REDIRECT_URL`.
- [ ] **Les tests en CI** : service container SQL Server dans Actions, ou runner self-hosted.
- [ ] **Sauvegarde du bucket MinIO** — seule copie active des 8 Go.
- [ ] **Faire tourner la clé de service MinIO** (passée en clair dans une conversation).
- [ ] **Politique de rétention GHCR** — peu urgent, le coût marginal par tag est de ~1 Mo.
- [ ] Ajouter un **swap de 2 Go** sur le VPS (change le mode de défaillance en cas d'OOM).
- [ ] **Décision produit** : exposer les lignes `IsCheckedButNotAttached` avec `deposee: false`.
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic` (4 fichiers réels).
- [ ] Endpoint d'upload pour les pièces jointes d'Article (lecture seule aujourd'hui).
- [ ] `UtilisateurAdminService` : `Description` jamais renseignée à la création du `CfgTier`.
- [ ] Anciens items : `sommePrixVenteTTC`, `PATCH` devis, source de `GET /api/commerciaux`,
      DESISTEMENT.
