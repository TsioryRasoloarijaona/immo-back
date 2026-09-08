# Session Memory
_Last updated: 2026-09-08T18:00+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Quatre chantiers dans cette session :

1. **Service d'envoi d'email (SMTP)** + expédition du mot de passe temporaire — **mergé**
   (PR #20 `8697d01`, PR #21 `697a2ed`).
2. **Synchronisation des données du légataire** sur les trois bases
   (`192.168.11.115` → `yesRemsProd` → `yesRemsDev`) — **terminée et vérifiée**.
3. **Correction de la note d'interaction** — branche `fix/interaction-note` (`b688a55`),
   **poussée, en attente de merge**.
4. **Recherche hotline par nom** — branche `feat/hotline-recherche-nom` (`675f24a`),
   **poussée, en attente de merge**.

**564 tests verts** (525 en début de session).

---

## ✅ What Worked

- **Mesurer avant de spécifier.** Décisif quatre fois de suite :
  - « 32 % des comptes ont un email » est trompeur — 60 comptes sur 75 sont désactivés, la
    population vivante est **10 sur 15** ;
  - `ComActionMessage.Description` est remplie à 100 % mais fait 22 à 41 caractères : c'est un
    libellé généré, pas un contenu ;
  - « MOHAMED » remonte **555 fiches** — le plafond de la recherche par nom n'est pas théorique ;
  - la collation `French_CI_AS` est **sensible aux accents**, et la base contient `Héjér F'H`.
- **Calquer sur l'existant plutôt qu'inventer** : la brique mail reprend exactement le dispositif
  de `storage/` (port + implémentation unique + double `@Primary` en test).
- **Lire les noms d'artefacts dans le BOM** — les starters ont été renommés en Boot 4.
- **Envoyer un vrai email** : seul moyen d'avoir trouvé le mauvais serveur SMTP et l'expéditeur
  manquant. Aucun test hors ligne ne pouvait les voir.
- **Extraire le tronc commun avant d'ajouter une variante** (hotline `assembler()`) : c'est ce
  qui fait hériter la recherche par nom des règles déjà éprouvées.
- **Lancer la suite complète après chaque chantier** : elle a révélé une régression de
  production que rien d'autre n'aurait attrapée (bug #25).

## ❌ What Failed

- **`smtp.hostinger.com` n'est pas le serveur de la boîte pro** → `535 authentication failed`.
  Boîte facturée par Hostinger, **hébergée par Titan**. L'erreur ressemble à un mot de passe faux.
- **`spring-boot-starter-mail` casse `/actuator/health`** : `MailHealthIndicator` ouvre une
  connexion SMTP à chaque appel → 503 en 7 s.
- **Synchronisation par INSERT seul** : les compteurs concordaient exactement alors que 12 lignes
  divergeaient.
- **Synchronisation par horodatage** : 4 règlements modifiés **sans** incrément de
  `UserUpdateDateTime`.
- **Diff shell sur des fichiers CRLF** : le retour chariot écrase la moitié gauche à l'affichage
  → 942 fausses divergences et un dossier fantôme. **Toujours `tr -d '\r'` avant de comparer.**
- **`EXCEPT` refuse `text`/`ntext`/`image`** — `ComFolder.ComDocumentDetail` est en `text`.
- **`STRING_AGG` renvoie `nvarchar(4000)` et TRONQUE** si ses entrées ne sont pas `max`.
- **Thymeleaf nu → `NoClassDefFoundError: ognl/PropertyAccessor`** : le starter Spring fournit
  SpEL, pas OGNL. Injecter l'`ITemplateEngine` du contexte.
- **JPQL ne connaît pas `COLLATE`** → requêtes natives obligatoires pour la recherche par nom.
- **`sp_addlinkedserver` et son usage dans le même lot** → « serveur introuvable dans
  sysservers » : le nom se résout à la compilation, il faut un `GO`.
- **Sous-requête dans un `GROUP BY`** en T-SQL → erreur d'agrégat ; passer par une CTE.
- **Séparateur de `-Dtest=` : virgule, pas `+`.**
- **Guillemets échappés dans `sqlcmd -Q "..."`** → l'argument est coupé ; passer par `-i fichier`.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 26 | `535 authentication failed` : défaut `smtp.hostinger.com` alors que la boîte est chez Titan | **Fixed** | Défaut `smtp.titan.email` (`697a2ed`) |
| 27 | `MAIL_FROM` absent ⇒ service silencieusement INACTIF | **Fixed** | Repli de l'expéditeur sur `spring.mail.username` |
| 25 | `MailHealthIndicator` rend `/actuator/health` 503 | **Fixed** | `management.health.mail.enabled=false` dans `main` **et** `test` |
| 28 | Synchro par INSERT seul : 12 lignes périmées invisibles aux compteurs | **Fixed** | Upsert avec comparaison de ligne entière |
| 30 | **`GET /interactions` n'exposait pas la note** ; `description` = libellé généré | **Fixed** | Champ `note` depuis `AlertMessage` (`b688a55`) |
| 31 | **`POST /interactions` écrivait dans la mauvaise colonne** et sans auteur | **Fixed** | Texte → `AlertMessage`, libellé généré, auteur depuis le jeton. **0 ligne à réparer** (défaut latent) |
| 29 | `RESOURCE_SEMAPHORE` : la vérification du script VPS n'obtient pas sa mémoire (SQL Express) | Contourné | Session tuée, vérification par requêtes légères |
| 21 | Migrations `005`/`006` non appliquées | **Fixed** | Appliquées par l'utilisateur ; vérifiées présentes sur le VPS |
| 11 | Règlement `CANCELED` : `sumAmountByComDocumentId` ne filtre pas sur le statut | **Open** | Latent |
| 12 | `POST /echeancier` sans `exclureDuPrix` | **Open** | Non corrigé |
| 13 | `S3_ENDPOINT` de la run config pointe la **production** | **Open** | Aucune écriture faite |
| 32 | VS Code retient les ports **9090 et 9091** → l'app ne démarre pas | **Open** | Contourner par `MANAGEMENT_SERVER_PORT=9092` dans la run config |
| 24 | `mvnw clean` tue l'app IntelliJ lancée depuis `target/classes` | Contourné | Ne pas builder tant qu'une instance tourne |

---

## 🔑 Key Points to Remember

### Le mode de défaillance central de la session

**Compter ne prouve rien.** Trois occurrences :

1. Après une synchro par INSERT seul, les totaux concordaient **exactement** des deux côtés
   alors que 12 lignes divergeaient.
2. Un sondage par `UserUpdateDateTime` annonçait 8 lignes périmées sur le VPS ; la comparaison
   de ligne entière en a trouvé **16**, dont 4 règlements modifiés sans que la date bouge.
3. Un diff shell sur des fichiers CRLF a affiché 942 fausses divergences.

**Seule méthode fiable** : `EXISTS (SELECT s.<cols> EXCEPT SELECT t.<cols>)` — compare toutes
les colonnes, traite `NULL = NULL`, avec cast des colonnes `text`/`ntext`/`image`.

### Le corollaire : une colonne bien remplie n'est pas une colonne utile

`ComActionMessage.Description` est renseignée sur **46 546 lignes sur 46 546**. Elle est
pourtant inexploitable : longueur 22–41 caractères, motif `JJ/MM/AAAA HH:MM <auteur>` sans une
seule exception. C'est un libellé généré, redondant avec `date` + `employeNom`. **Le contenu
réel est dans `AlertMessage`** (1–944 caractères, 96,6 %), qui malgré son nom ne relève pas des
relances : 6 502 interactions ont une note sans aucune date d'alerte.

Toujours regarder **la distribution des longueurs et un échantillon**, pas seulement le taux de
remplissage.

### Le mail

- **Serveur : `smtp.titan.email`**, pas Hostinger — qui **revend** Titan. Vérifiable par les MX
  du domaine et le SPF. Port 587 + STARTTLS.
- **Variables suffisantes** : `MAIL_USERNAME` + `MAIL_PASSWORD`. `MAIL_FROM` est facultatif
  (repli sur le compte SMTP).
- **`management.health.mail.enabled=false` obligatoire**, dans les DEUX fichiers de propriétés
  (celui de `test` REMPLACE celui de `main`).
- Chaîne : publication de `MotDePasseTemporaireEmis` → écouteur `AFTER_COMMIT` +
  `@Async("mailExecutor")` → port `EnvoiEmail`. Échec SMTP journalisé, jamais propagé.

### Ce qui est intestable tel quel

- **`@TransactionalEventListener(AFTER_COMMIT)` ne se déclenche JAMAIS sous test** (suite
  `@Transactional`, annulée) → `@RecordApplicationEvents` capture les événements *publiés*.
- **Un bean `@Async` injecté est un proxy** → instancier l'écouteur à la main dans le test.

### La topologie des bases

| Base | Rôle | Version |
|---|---|---|
| `192.168.11.115` / `YesRems` | **source de vérité**, app de bureau | SQL 2019 |
| `yesRemsProd` (local, `49687`) | copie de travail | SQL 2019 |
| `yesRemsDev` (VPS, tunnel `127.0.0.1:1433`) | environnement **déployé**, Docker | SQL 2022 |

- **Le VPS ne peut pas joindre le `.115`** (LAN privé) : la synchro passe par le local en relais.
- **Aucune dérive de schéma légataire** : sur 7 100 colonnes, le seul écart est
  `AdmAttachedFile.AttachedFile` (FILESTREAM refusée au RESTORE), plus nos trois tables `App*`.
- **Chaque base a ses lignes propres, à ne jamais écraser** : le local porte `t.admin` et sa
  fiche `CfgTier` « Tsiory Admin » (`F7F6EC46-…`, **exclue** de la poussée vers le VPS) ; le VPS
  porte une interaction créée par l'app déployée et ses `AppUserCredential`.
- **`SET DATEFORMAT ymd;`** en tête de toute requête datée (session française ⇒ `dmy`).
- **`CHECKSUM()` n'est pas stable entre SQL 2019 et 2022** — inutilisable pour comparer local et VPS.
- **Collation `French_CI_AS`** : insensible à la casse, **sensible aux accents**. Toute recherche
  textuelle sur un nom exige `COLLATE French_CI_AI` — donc une requête **native**.

### Ce que les données disent du produit

- **L'acquisition est automatisée** : `n8n_api` et une source sans `UserCreate` injectent
  ~19 leads/jour depuis Facebook Ads et les sites web.
- **84 % des leads entrants ne sont ni assignés ni contactés.** Sur 114 nouveaux leads :
  18 assignés **et** contactés, 96 ni l'un ni l'autre — **aucune case hors diagonale**.
- **Stock dormant : 2 994** prospections ouvertes jamais assignées ni contactées, **+91 en six
  jours**. 664 ont plus d'un an.
- `CfgTierKind` est nul pour **98,8 %** des tiers : la norme, jamais une anomalie.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-service-envoi-email.md` — §4 la config **avec l'encadré Titan**,
  §7 bis le piège de la sonde de santé, §9 l'impact front.
- `instructions/FIX-interaction-note.md` — §2 les mesures qui prouvent que `Description` est un
  libellé, §5 les règles d'écriture, §6 l'impact front.
- `instructions/FEATURE-hotline-recherche-nom.md` — §2 la collation et la volumétrie, §3 le
  contrat, §7 l'impact front.
- `src/main/java/.../mail/` — `EnvoiEmail` (port), `EnvoiEmailSmtp` (repli d'expéditeur),
  `EmailConfig` (`@EnableAsync`), `MotDePasseTemporaireEmis` (`toString()` masqué), `AdresseMasquee`.
- `src/main/java/.../services/hotline/HotlineService.java` — `assembler()` commun aux deux
  recherches, `FICHES_MAX_PAR_NOM = 20`.
- `src/main/java/.../repositories/{cfg/CfgTierRepository,com/ComProspectionRepository}.java` —
  requêtes natives `COLLATE French_CI_AI`, avec variante « dans périmètre ».
- **`../sync-yesRemsProd-depuis-115.sql`** et **`../sync-vps-yesRemsDev.sql`** — upserts
  idempotents, **hors dépôt git** (dans `prototype/`), volontairement. La section 4 du second est
  trop lourde et se bloque en `RESOURCE_SEMAPHORE` : vérifier à la main.

---

## 🔧 Environment & Config

- **Tests** : `564 tests, verts`.
  ```powershell
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
- **Run config IntelliJ** : porte `MAIL_USERNAME` / `MAIL_PASSWORD`. **Ajouter
  `MANAGEMENT_SERVER_PORT=9092`** — VS Code occupe 9090 et 9091 (bug #32). Mots de passe **non
  consignés ici**.
- **⚠️ Ne pas lancer `mvnw clean`** pendant qu'une instance tourne depuis IntelliJ (bug #24).
- **Sources de données IntelliJ** : `prod`, `vps_dev`, `YesRems@192.168.11.115`. Les mots de
  passe y sont chiffrés (`master_key`), **non lisibles** depuis les fichiers de configuration.

---

## ⏭️ Next Step

> **Merger les deux PR de code sur GitHub**, dans n'importe quel ordre — elles ont été coupées
> du même `main` et portent sur des fichiers disjoints :
> - `fix/interaction-note` (`b688a55`)
> - `feat/hotline-recherche-nom` (`675f24a`)
>
> Puis `node .claude/scripts/git-flow.mjs sync back`.
>
> **⚠️ La PR `docs/memoire-de-session` est à FERMER sans merger** : elle ne contient qu'une
> version antérieure de ce fichier, entièrement reprise ici. La merger créerait un conflit.

### Backlog
- [ ] Relayer au dépôt frontend (autre lane), trois lots :
      **mail** — 400 sur la réinitialisation sans email, `email` obligatoire à la création,
      champs `emailEnvoye`/`destinataire` ;
      **interactions** — afficher `note` et non `description` ;
      **hotline** — champ de recherche par nom, et **afficher `tronque`**.
- [ ] **Écran d'affectation en masse des leads** — 2 994 dormants, +91 par semaine. Plus d'effet
      que tout le reste du backlog.
- [ ] **5 comptes actifs sur 15 sans adresse** : plus réinitialisables tant que leur fiche n'est
      pas complétée. Contrepartie assumée du choix du 400.
- [ ] Relancer l'app depuis IntelliJ après avoir ajouté `MANAGEMENT_SERVER_PORT` (bug #32).
- [ ] Alléger la section 4 de `sync-vps-yesRemsDev.sql`.
- [ ] Autres usages du mail : rappels d'échéance, notification d'affectation de prospection.
- [ ] Déclarer le check **`Tests` comme requis** sur `main` ; `deploy.yml` ne joue pas les tests.
- [ ] Garde-fou sur `S3_ENDPOINT` en local (bug #13).
- [ ] Balayage hors ligne du bucket : rien ne supprime les objets dont `DeletedAt` est renseigné.
- [ ] Règlement annulé : filtrer `sumAmountByComDocumentId` sur le statut (bug #11).
- [ ] `exclureDuPrix` : le dériver de la catégorie à défaut (#12).
- [ ] Suite des DELETE manquants : règlement, échéance, interaction.
- [ ] Endpoint de désactivation d'un compte (`AdmUser.Disabled` lu partout, jamais écrit).
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic`.
- [ ] Sauvegarde du bucket MinIO — seule copie active des 8 Go.
- [ ] Révoquer le PAT GHCR et la clé de service MinIO passés en clair dans des conversations.
