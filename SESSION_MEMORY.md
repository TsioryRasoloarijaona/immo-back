# Session Memory
_Last updated: 2026-09-04T17:58:34+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective
Conteneuriser le backend et le déployer sur le VPS (branche `deploy/workflow`). Les chantiers
précédents — migration des 8 Go de pièces jointes vers MinIO (PR #6) et performance de la liste
des dossiers (PR #7) — sont **fusionnés dans `main`**. La session en cours a corrigé et complété
le `Dockerfile` ; **la construction de l'image n'est pas encore vérifiée** (daemon Docker arrêté).

---

## ✅ What Worked
- **Lire le mode git avant de faire confiance à un script** : `git ls-files -s mvnw` → `100644`.
  Le bit exécutable n'a jamais été posé (checkout Windows) — `RUN ./mvnw` aurait donné
  `Permission denied` dès la première couche.
- **Vérifier les imports avant de changer de base d'image** :
  `grep -rhoE "import (java\.awt[a-zA-Z.]*|javax\.imageio[a-zA-Z.]*);" src/main/java | sort | uniq -c`
  → **uniquement `java.awt.Color`, 11 fois**. Aucune `Font`, `Graphics2D` ni `ImageIO` ⇒ le
  passage `jdk-alpine` → `jre-alpine` est sans risque malgré les 10 générateurs PDF.
- **Lire `application.properties` avant d'ajouter un `HEALTHCHECK`** : les lignes 55-56
  documentent que `/actuator/**` est sous `anyRequest().authenticated()`. Sonde évitée.
- **Mesurer le contexte de build** (`du -sh target graphify-out instructions .git`) → 57 Mo pour
  ~5 Mo utiles, ce qui a justifié le `.dockerignore`.
- **Lire le script `mvnw` plutôt que supposer ses dépendances** : `distributionType=only-script`,
  bascule automatique sur `.tar.gz` faute d'`unzip` ⇒ busybox (`wget` + `tar`) suffit, **aucun
  `apk add` nécessaire**.

## ❌ What Failed
- **`docker build ... 2>&1 | tail -60`** → **faux vert**. Le code de sortie d'un pipeline est
  celui de la *dernière* commande (`tail`, toujours 0). Le harness a rapporté `exit code 0`
  alors que le build n'a jamais démarré. **Toujours rediriger (`> build.log 2>&1`), jamais
  `| tail`, quand le code de sortie compte.**
- **`docker --version` comme test de disponibilité** → répond même daemon éteint (c'est le
  client seul). Le vrai test est **`docker info`**.
- **`-Djarmode=layertools`** (version initiale du `Dockerfile`) → retiré de Spring Boot 4
  (déprécié en 3.3). Le projet est sur **4.1.0**.
- **`WORKDIR .`** → un chemin relatif se résout par rapport au `WORKDIR` précédent, défaut `/` :
  la compilation se faisait dans la racine du conteneur.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | `mvnw` en `100644` → `Permission denied` dans le conteneur | Fixed | `RUN chmod +x mvnw` dans le `Dockerfile` (alternative en attente d'arbitrage : `git update-index --chmod=+x mvnw`) |
| 2 | `-Djarmode=layertools` retiré en Spring Boot 4 | Fixed **non vérifié** | `-Djarmode=tools ... extract --layers` — à confirmer par un build réel |
| 3 | `WORKDIR .` compile dans `/` | Fixed | `WORKDIR /build`, destinations alignées |
| 4 | Runtime en `jdk` (~150 Mo de trop par tag) | Fixed | `eclipse-temurin:21-jre-alpine` |
| 5 | JVM limitée à 25 % de la mémoire du conteneur | Fixed | `-XX:MaxRAMPercentage=75.0` + `-XX:+ExitOnOutOfMemoryError` dans l'`ENTRYPOINT` |
| 6 | Contexte de build de 57 Mo, `.env` inclus | Fixed | `.dockerignore` créé |
| 7 | `Dockerfile` en CRLF, non couvert par `.gitattributes` | Fixed | `/Dockerfile text eol=lf` ajouté |
| 8 | **Daemon Docker Desktop arrêté** (`npipe:////./pipe/dockerDesktopLinuxEngine` introuvable) | **Open** | Démarrer Docker Desktop — bloque toute vérification |
| 9 | L'index git contient encore le template IntelliJ (`ubuntu:latest` / `top -b`) | **Open** | Sera écrasé au prochain `git add Dockerfile` |
| 10 | `api.tsiory.tech` renvoie **502** (backend VPS à terre) | **Open** | Non investigué cette session |

---

## 🔑 Key Points to Remember

**Décision de registre : GHCR (GitHub Container Registry), image privée.** Motifs retenus —
(a) le `GITHUB_TOKEN` d'Actions suffit (`permissions: packages: write`), **aucun secret à créer
ni à faire tourner**, contrairement à un access token Docker Hub ; (b) code, PR, Actions et
registre au même endroit ; (c) pas de limite de pull. Le `LABEL org.opencontainers.image.source`
déjà présent est **le mécanisme GHCR** de rattachement de l'image au dépôt (hérite visibilité et
permissions) — il ne sert à rien sur Docker Hub.
⚠️ **Contrepartie** : le stockage GHCR **privé** est décompté d'un quota (~500 Mo sur le plan
Free, à revérifier). D'où le runtime `jre` et une **politique de rétention à mettre en place dès
le départ** (garder `latest` + 3 derniers SHA).

- **`java.awt.Color` est la seule classe AWT utilisée** (11 imports, 10 générateurs PDF).
  Pas de fontconfig/freetype requis ⇒ **Alpine/musl est sûr**. À re-vérifier avant d'introduire
  `Font`, `Graphics2D` ou `ImageIO`, qui casseraient sur `jre-alpine`.
- **`/actuator/**` exige le cookie JWT** (`application.properties:55-56` : hors de la liste
  `permitAll`, donc `anyRequest().authenticated()`). Un `HEALTHCHECK` dessus recevrait `401` et
  marquerait le conteneur `unhealthy` en permanence. **Aucun `HEALTHCHECK` dans le Dockerfile** —
  choix délibéré, en attente d'arbitrage (ouvrir `/actuator/health` en `permitAll` vs sonde TCP).
- **`.gitattributes` épingle `/mvnw text eol=lf`** — indispensable : un `mvnw` en CRLF donnerait
  `bad interpreter` dans le conteneur, symptôme bien plus opaque que le `Permission denied` du
  bit exécutable. Vérifié LF à l'`od -c` (`#!/bin/sh\n`). `core.autocrlf=true` sur ce poste.
- **Le maven wrapper télécharge Maven 3.9.16 au build** (`wrapperVersion=3.3.4`,
  `distributionType=only-script`). Chaque build sans cache refait ce téléchargement.
- **Ne jamais conclure d'un `exit code` derrière un pipe.** Cf. « What Failed » — c'est la même
  classe d'erreur que le filtre projet « 7× plus rapide » de la session précédente : la mesure
  semblait bonne, elle ne mesurait rien.
- **`StkItem.CfgProjectId` est une colonne morte : 0 ligne sur 1068.** Le projet se résout
  **toujours** par `ComFolder.StkItemId → StkItem.StkHierarchyId → CfgTranche.CfgProjectId`.
- **Deux chemins de rattachement article↔dossier** : `ComFolder.StkItemId` (article principal)
  = **358/358** ; `StkItem.ComFolderId` (lien inverse) = **328/358**. Un filtre ne regardant que
  le lien inverse rate 30 dossiers en silence.
- **`IsCheckedButNotAttached = 1` = « document requis coché, jamais téléversé »** (272 lignes),
  masqué par l'API via `hasFile` — ni régression ni perte. Source de confusion n°1 des
  signalements « document manquant ».
- **Aucune pièce jointe perdue en migration S3** — établi par quatre preuves indépendantes
  (réconciliation ligne à ligne incluse). Question close.

---

## 📁 Important Files & Paths

**Modifiés/créés cette session — tous NON COMMITÉS :**
- `Dockerfile` — multi-stage réécrit. Build `jdk-alpine` (`WORKDIR /build`, `chmod +x mvnw`,
  `dependency:go-offline`, `package -DskipTests`, `jarmode=tools extract --layers`) ; runtime
  `jre-alpine`, user non-root `app`, 4 `COPY` de couches, `EXPOSE 8080`,
  `ENTRYPOINT` = `JarLauncher` + flags mémoire. **Statut git `AM`** : l'index contient encore
  le template IntelliJ.
- `.dockerignore` — **nouveau**, non suivi. Exclut `target/`, `graphify-out/`, `.git/`,
  `instructions/`, `docs/`, `.idea/`, `.claude/`, `*.md` et **`.env*`**.
- `.gitattributes` — ajout de `/Dockerfile text eol=lf`.

**Référence :**
- `src/main/resources/application.properties:55-61` — exposition actuator + auth requise.
- `.mvn/wrapper/maven-wrapper.properties` — `only-script`, Maven 3.9.16.
- `instructions/FEATURE-s3-storage.md` §4 — contient une affirmation CORS à corriger (backlog).

---

## 🔧 Environment & Config
- **Docker** : client **29.7.2** présent, **daemon arrêté**. Contexte actif `desktop-linux`
  (`npipe:////./pipe/dockerDesktopLinuxEngine`). Tester avec `docker info`, pas `docker --version`.
- **Base servie par le backend : `yesRemsDev`**, via tunnel SSH — **`127.0.0.1,1433`**, jamais
  `localhost` (résolution IPv6, le tunnel n'écoute qu'en IPv4 ; `Test-NetConnection` ment).
- **Bases locales `localhost,49687`** : `yesRemsProd` (migrée), `yesRemsTest` (suite de tests),
  **`yesRems` = passe d'essai interrompue — ne pas y brancher le backend.**
- **MinIO** : bucket **`immo-back`** préfixe `prod/` ; `immo-migration-test` préfixe `test/`
  (essai interrompu, à ne pas utiliser).
- **Variables d'env requises** (aucune valeur par défaut) : `DB_HOST`, `DB_NAME`, `DB_USER`,
  `DB_PWD`, `JWT_SECRET`, plus la configuration S3 (`S3_ENDPOINT`, `S3_BUCKET`, clés).
  Identifiants hors de ce fichier.
- **Git** : branche `deploy/workflow`, **à parité avec `origin/main` (0/0)**. Aucun commit en
  attente, mais **4 fichiers non commités** (voir ci-dessus). Rien n'est validé ⇒ ne pas
  committer avant que le build passe.

---

## ⏭️ Next Step
> **Démarrer Docker Desktop**, puis construire l'image **sans pipe** pour que le code de sortie
> soit exploitable :
> ```bash
> docker build -t immo-back:local . > build.log 2>&1; echo "exit=$?"
> ```
> Le point à confirmer en priorité est le `-Djarmode=tools ... extract --layers` (bug #2) — c'est
> la seule correction que la lecture seule ne permet pas de garantir.

### Backlog
- [ ] **Trois arbitrages en attente de l'utilisateur** : (1) `git update-index --chmod=+x mvnw`
      plutôt que le `chmod` du Dockerfile ? (2) `/actuator/health` en `permitAll` pour un vrai
      `HEALTHCHECK`, ou sonde TCP ? (3) rester sur `deploy/workflow` ou brancher depuis `main` ?
- [ ] Écrire le **workflow GitHub Actions** de build/push vers GHCR (`permissions: packages:
      write`, `docker/login-action` avec `GITHUB_TOKEN`, tags `latest` + SHA).
- [ ] **Politique de rétention GHCR** dès la mise en place (quota privé).
- [ ] **Diagnostiquer le 502 de `api.tsiory.tech`** — préalable à toute validation en production.
- [ ] **Contrôler `S3_BUCKET` sur le VPS** : doit valoir `immo-back` (les `StorageKey` de
      `yesRemsDev` sont en `prod/`). Symptôme sinon : liste correcte, téléchargements en échec.
- [ ] **Trancher le point `credentials: 'include'`** au `curl` sur une URL présignée, puis
      corriger `FEATURE-s3-storage.md` §4.
- [ ] **Sauvegarde du bucket** — MinIO reste la seule copie active des 8 Go.
- [ ] **Faire tourner la clé de service MinIO** (passée en clair dans une conversation).
- [ ] **Décision produit** : exposer les lignes `IsCheckedButNotAttached` avec `deposee: false`
      plutôt que les masquer.
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic` (4 fichiers réels).
- [ ] Endpoint d'upload pour les pièces jointes d'Article (lecture seule aujourd'hui).
- [ ] `UtilisateurAdminService` : `Description` jamais renseignée à la création du `CfgTier`
      employé — corrigé dans `ProspectService`, signalé, non corrigé ici.
- [ ] Réactiver l'auth sur les générateurs de documents (3 `permitAll()` dans `SecurityConfig`).
- [ ] Anciens items : bug cookie 401 (`app.jwt.cookie-secure` sans profil actif),
      `sommePrixVenteTTC`, `PATCH` devis, source de `GET /api/commerciaux`, DESISTEMENT.
