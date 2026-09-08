# Session Memory
_Last updated: 2026-09-08T19:15+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Cinq chantiers dans cette session. **Quatre sont mergés dans `main`**, le cinquième attend sa PR.

| # | Chantier | État |
|---|---|---|
| 1 | Service d'envoi d'email (SMTP Titan) + mot de passe temporaire | **mergé** (PR #20, #21) |
| 2 | Synchronisation des trois bases `.115` → `yesRemsProd` → `yesRemsDev` | **terminée et vérifiée** |
| 3 | Note d'interaction : lecture + écriture | **mergé** (PR #22, `b688a55`) |
| 4 | Recherche hotline par nom | **mergé** (PR #23, `675f24a`) |
| 5 | Contact + dernière alerte dans la liste des prospections | branche `feat/prospection-contact-alerte` (`e9aaba3`), **poussée, PR à ouvrir** |

**573 tests verts** (525 en début de session).

---

## ✅ What Worked

- **Mesurer avant de spécifier.** Décisif à chaque chantier :
  - « 32 % des comptes ont un email » est trompeur — 60 comptes sur 75 sont désactivés, la
    population vivante est **10 sur 15** ;
  - `ComActionMessage.Description` est remplie à **100 %** mais fait 22–41 caractères : libellé
    généré, pas contenu ;
  - « MOHAMED » remonte **555 fiches** — le plafond de la recherche par nom est mesuré ;
  - la collation `French_CI_AS` est **sensible aux accents**, et la base contient `Héjér F'H` ;
  - `ComProspection.AlertValidityDate` est **morte** (0/8 613) — les alertes vivent sur les
    interactions.
- **Envoyer un vrai email** : seul moyen d'avoir trouvé le mauvais serveur SMTP et l'expéditeur
  manquant. Aucun test hors ligne ne pouvait les voir.
- **Extraire le tronc commun avant d'ajouter une variante** (hotline `assembler()`).
- **Lancer la suite complète après chaque chantier**, et **après un cherry-pick** : un commit
  écrit sur un `main` antérieur peut ne pas entrer en conflit textuel tout en cassant
  sémantiquement.
- **Calquer sur l'existant** : la brique mail reprend le dispositif de `storage/`.

## ❌ What Failed

- **`smtp.hostinger.com` n'est pas le serveur de la boîte pro** → `535 authentication failed`.
  Boîte facturée par Hostinger, **hébergée par Titan**.
- **`spring-boot-starter-mail` casse `/actuator/health`** : connexion SMTP à chaque appel → 503.
- **Synchronisation par INSERT seul** : compteurs identiques, 12 lignes divergentes.
- **Synchronisation par horodatage** : 4 règlements modifiés sans incrément de `UserUpdateDateTime`.
- **Diff shell sur fichiers CRLF** → 942 fausses divergences. **Toujours `tr -d '\r'`.**
- **`EXCEPT` refuse `text`/`ntext`/`image`** ; **`STRING_AGG` tronque à 4 000** si les entrées
  ne sont pas `max`.
- **Thymeleaf nu → `NoClassDefFoundError: ognl/PropertyAccessor`** : injecter l'`ITemplateEngine`
  du contexte (le starter Spring fournit SpEL, pas OGNL).
- **JPQL ne connaît pas `COLLATE`** → requêtes natives obligatoires pour la recherche par nom.
- **Une requête native n'entre pas dans les espaces de requête d'Hibernate** → pas d'auto-flush ;
  `saveAndFlush` dans les fixtures.
- **`sp_addlinkedserver` utilisé dans le même lot** → « serveur introuvable » : il faut un `GO`.
- **Sous-requête dans un `GROUP BY`** T-SQL → passer par une CTE.
- **Guillemets échappés dans `sqlcmd -Q "..."`** → argument coupé ; passer par `-i fichier`.
- **`-Dtest=A,B`** avec des virgules, pas des `+`.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 26 | `535 authentication failed` : défaut `smtp.hostinger.com`, boîte chez Titan | **Fixed** | Défaut `smtp.titan.email` |
| 27 | `MAIL_FROM` absent ⇒ service silencieusement INACTIF | **Fixed** | Repli sur `spring.mail.username` |
| 25 | `MailHealthIndicator` rend `/actuator/health` 503 | **Fixed** | `management.health.mail.enabled=false` dans `main` **et** `test` |
| 28 | Synchro par INSERT seul : 12 lignes périmées invisibles aux compteurs | **Fixed** | Upsert avec comparaison de ligne entière |
| 30 | `GET /interactions` n'exposait pas la note | **Fixed** | Champ `note` depuis `AlertMessage` |
| 31 | `POST /interactions` écrivait dans la colonne du libellé, sans auteur | **Fixed** | Texte → `AlertMessage`, libellé généré, auteur depuis le jeton. **0 ligne à réparer** |
| 33 | **Trois tests étaient de faux verts** : `doesNotExist` sur une liste vide | **Fixed** | `content.length() == 1` exigé avant toute autre assertion |
| 29 | `RESOURCE_SEMAPHORE` sur la vérification du script VPS (SQL Express) | Contourné | Vérification par requêtes légères |
| 21 | Migrations `005`/`006` non appliquées | **Fixed** | Appliquées par l'utilisateur, vérifiées sur le VPS |
| 32 | **VS Code retient les ports 9090 et 9091** → l'app ne démarre pas | **Open** | Ajouter `MANAGEMENT_SERVER_PORT=9092` à la run config |
| 11 | Règlement `CANCELED` : `sumAmountByComDocumentId` ne filtre pas sur le statut | **Open** | Latent |
| 12 | `POST /echeancier` sans `exclureDuPrix` | **Open** | Non corrigé |
| 13 | `S3_ENDPOINT` de la run config pointe la **production** | **Open** | Aucune écriture faite |
| 24 | `mvnw clean` tue l'app IntelliJ lancée depuis `target/classes` | Contourné | Ne pas builder pendant qu'une instance tourne |

---

## 🔑 Key Points to Remember

### Les trois leçons transversales

**1. Compter ne prouve rien.** Après une synchro par INSERT seul, les totaux concordaient
exactement pendant que 12 lignes divergeaient. Un sondage par date annonçait 8 lignes périmées
là où il y en avait 16. Un diff shell en affichait 942 à cause d'un retour chariot.
→ Seule méthode fiable : `EXISTS (SELECT s.<cols> EXCEPT SELECT t.<cols>)`, avec cast des
colonnes `text`/`ntext`/`image`.

**2. Une colonne bien remplie n'est pas une colonne utile.** `Description` : 46 546/46 546,
inexploitable. `ComProspection.AlertValidityDate` : la colonne « évidente » pour une alerte, et
elle est morte (0/8 613). → Regarder **la distribution des longueurs et un échantillon**, pas
le seul taux de remplissage.

**3. Un test qui passe peut ne rien tester.** `/api/prospections` exclut les pré-prospections
(`CfgProspectId` ET `CfgCommercialId` nuls) ; une fixture sans prospect n'est jamais renvoyée, et
`doesNotExist` sur une liste vide **passe trivialement**. → Dans tout test de liste, **affirmer
d'abord que la ligne est là** (`content.length()`).

### Le mail

- **Serveur : `smtp.titan.email`**, pas Hostinger — qui **revend** Titan. Port 587 + STARTTLS.
- **Variables suffisantes** : `MAIL_USERNAME` + `MAIL_PASSWORD` ; `MAIL_FROM` facultatif.
- **`management.health.mail.enabled=false` obligatoire**, dans les DEUX fichiers de propriétés
  (celui de `test` REMPLACE celui de `main`).

### Interactions

- **La note est dans `AlertMessage`**, jamais dans `Description` (libellé
  `JJ/MM/AAAA HH:MM <auteur>` généré). `AlertMessage` ne relève pas des relances : 6 502
  interactions ont une note sans date d'alerte.
- **L'auteur est `AdmUserId`**, pas `CfgEmployeeId` (vide 4 fois sur 5).

### Prospections / hotline

- **Le contact a deux sources et aucune ne suffit** : fiche 5 034, premier contact 7 596,
  **8 591 par l'une ou l'autre** sur 8 613 ouvertes.
- **Les alertes viennent des interactions**, résolues par un `ROW_NUMBER()` sur toute la page en
  **une** requête (sinon N+1).
- **`ProspectionListItemDto` avait TROIS mappeurs identiques** (`ProspectionService`,
  `HotlineService`, `ProspectSubResourcesService`) — fusionnés en
  `ProspectionService.toListItem()`. C'est la duplication qui les avait fait diverger.
- **Recherche par nom plafonnée à 20 fiches** + `tronque` ; le téléphone n'est pas plafonné.
- **Parité de périmètre commercial obligatoire** sur toute nouvelle route de recherche, sinon
  elle devient un contournement.

### Ce qui est intestable tel quel

- **`@TransactionalEventListener(AFTER_COMMIT)` ne se déclenche JAMAIS sous test** →
  `@RecordApplicationEvents`.
- **Un bean `@Async` injecté est un proxy** → instancier l'écouteur à la main.

### La topologie des bases

| Base | Rôle | Version |
|---|---|---|
| `192.168.11.115` / `YesRems` | **source de vérité**, app de bureau | SQL 2019 |
| `yesRemsProd` (local, `49687`) | copie de travail | SQL 2019 |
| `yesRemsDev` (VPS, tunnel `127.0.0.1:1433`) | environnement **déployé**, Docker | SQL 2022 |

- **Le VPS ne peut pas joindre le `.115`** : la synchro passe par le local en relais.
- **Aucune dérive de schéma légataire** : seul écart, `AdmAttachedFile.AttachedFile`, plus nos
  trois tables `App*`.
- **Lignes propres à chaque base, à ne jamais écraser** : `t.admin` + sa fiche « Tsiory Admin »
  en local ; une interaction et les `AppUserCredential` sur le VPS.
- **`SET DATEFORMAT ymd;`** en tête de toute requête datée.
- **`CHECKSUM()` n'est pas stable entre SQL 2019 et 2022.**
- **Collation `French_CI_AS`** : toute recherche sur un nom exige `COLLATE French_CI_AI`, donc
  une requête **native**.

### Ce que les données disent du produit

- **L'acquisition est automatisée** (`n8n_api` + une source sans `UserCreate`), ~19 leads/jour
  depuis Facebook Ads et les sites web.
- **84 % des leads entrants ne sont ni assignés ni contactés**, sans une seule exception :
  assignation et contact sont le même geste.
- **Stock dormant : 2 994**, **+91 en six jours**. 664 ont plus d'un an.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-service-envoi-email.md` — §4 l'encadré Titan, §7 bis la sonde de santé.
- `instructions/FIX-interaction-note.md` — §2 les mesures, §5 les règles d'écriture.
- `instructions/FEATURE-hotline-recherche-nom.md` — §2 collation et volumétrie.
- `instructions/FEATURE-prospection-contact-alerte.md` — §2 les deux sources du contact,
  §5 les deux pièges (auto-flush natif, faux verts).
- `src/main/java/.../mail/` — port `EnvoiEmail`, `EnvoiEmailSmtp`, `EmailConfig`,
  `MotDePasseTemporaireEmis` (`toString()` masqué), `AdresseMasquee`.
- `src/main/java/.../services/hotline/HotlineService.java` — `assembler()` commun,
  `FICHES_MAX_PAR_NOM = 20`.
- `src/main/java/.../services/prospection/ProspectionService.java` — `versListe()` (requête
  groupée des alertes), `toListItem()` **unique mappeur**, `libelleInteraction()`.
- **`../sync-yesRemsProd-depuis-115.sql`** et **`../sync-vps-yesRemsDev.sql`** — upserts
  idempotents, **hors dépôt git**. La section 4 du second se bloque en `RESOURCE_SEMAPHORE`.

---

## 🔧 Environment & Config

- **Tests** : `573 tests, verts`.
  ```powershell
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
- **Run config IntelliJ** : porte `MAIL_USERNAME` / `MAIL_PASSWORD`. **Ajouter
  `MANAGEMENT_SERVER_PORT=9092`** — VS Code occupe 9090 et 9091 (bug #32). Mots de passe **non
  consignés ici**.
- **⚠️ Ne pas lancer `mvnw clean`** pendant qu'une instance tourne depuis IntelliJ (bug #24).

---

## ⏭️ Next Step

> **Ouvrir et merger la PR de `feat/prospection-contact-alerte`** (`e9aaba3`, un seul commit,
> 7 fichiers) :
> https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Fprospection-contact-alerte?expand=1
>
> Puis `node .claude/scripts/git-flow.mjs sync back`.

### Ménage de branches (après ce merge seulement)
- [ ] **`docs/memoire-de-session`** — à fermer/supprimer sans merger : ses commits ne portent
      qu'un `SESSION_MEMORY.md` daté de 14:15, dépassé par celui déjà dans `main`.
- [ ] **`feat/hotline-recherche-nom`** — mergée (PR #23), mais traîne `070254f` dont `e9aaba3`
      est la copie propre. **Ne la supprimer qu'après** le merge ci-dessus.
- [ ] Vérifier pourquoi `crm-prototype` est sur une branche `feat/hotline-recherche-nom` —
      ce n'est pas ce dépôt qui l'a créée.

### Backlog
- [ ] Relayer au dépôt frontend (autre lane), quatre lots :
      **mail** (400 sans email, `email` obligatoire, `emailEnvoye`/`destinataire`) ;
      **interactions** (afficher `note`, pas `description`) ;
      **hotline** (champ nom, **afficher `tronque`**) ;
      **liste prospections** (`telephone`, `email`, `derniereAlerteDate`/`Message`).
- [ ] **Écran d'affectation en masse des leads** — 2 994 dormants, +91/semaine. Le plus rentable
      du backlog, et la liste vient d'obtenir le contact.
- [ ] **5 comptes actifs sur 15 sans adresse** : plus réinitialisables tant que leur fiche n'est
      pas complétée.
- [ ] Relancer l'app depuis IntelliJ après ajout de `MANAGEMENT_SERVER_PORT` (bug #32).
- [ ] Alléger la section 4 de `sync-vps-yesRemsDev.sql`.
- [ ] Suite des DELETE manquants : règlement, échéance, interaction.
- [ ] Endpoint de désactivation d'un compte (`AdmUser.Disabled` lu partout, jamais écrit).
- [ ] Autres usages du mail : rappels d'échéance, notification d'affectation.
- [ ] Déclarer le check **`Tests` comme requis** sur `main` ; `deploy.yml` ne joue pas les tests.
- [ ] Garde-fou sur `S3_ENDPOINT` en local (bug #13).
- [ ] Balayage hors ligne du bucket : rien ne supprime les objets dont `DeletedAt` est renseigné.
- [ ] Règlement annulé : filtrer `sumAmountByComDocumentId` sur le statut (bug #11).
- [ ] `exclureDuPrix` : le dériver de la catégorie à défaut (#12).
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic`.
- [ ] Sauvegarde du bucket MinIO — seule copie active des 8 Go.
- [ ] Révoquer le PAT GHCR et la clé de service MinIO passés en clair dans des conversations.
