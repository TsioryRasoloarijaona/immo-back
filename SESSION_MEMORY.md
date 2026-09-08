# Session Memory
_Last updated: 2026-09-08T22:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Trois chantiers, un seul fil : **prévenir le prospect quand un rendez-vous est pris**, puis
fiabiliser la donnée qui le rend possible.

| # | Chantier | État |
|---|---|---|
| 1 | Mail de confirmation de visite (adresse, conseiller, plans en pièce jointe) | **mergé** (PR #26, `e5a6d60`) |
| 2 | Même confirmation par **WhatsApp** (UltraMsg) | **mergé** (PR #27, `c333609`) |
| 3 | **Indicatif obligatoire** + formatage des numéros à l'écriture | branche `feat/indicatif-telephone` (`adbaac9`), **poussée, PR à merger** |

**632 tests verts** (573 en début de session).

---

## ✅ What Worked

- **Mesurer avant de spécifier**, encore et systématiquement. Chaque décision de conception de
  cette session vient d'un `COUNT`, jamais d'une intuition :
  - l'adresse vient du **projet**, pas de la tranche — 5 071/5 071 contre 1 639 ;
  - l'article n'est connu qu'une fois sur trente à la prise de RDV (**173/5 078**), donc les
    plans sont l'exception et non la règle ;
  - **99 % des prospects ont un numéro exploitable contre 57 % un email** — c'est ce seul écart
    qui a décidé « les deux canaux » plutôt qu'un repli ;
  - `MobileIndicatif` + `Mobile` sans espaces donne directement le `+212…` d'UltraMsg, et
    **aucune ligne réelle ne porte de caractère parasite** hors espaces.
- **Séparer la résolution du contenu de son acheminement** dès le premier chantier, par
  anticipation. L'ajout du WhatsApp n'a modifié **aucune règle** de résolution — seulement leur
  mise en forme. C'est le pari le plus rentable de la session.
- **Factoriser au lieu de recopier** : `utils/NumeroTelephone` extrait de `NumeroWhatsApp` au
  moment d'ajouter la validation. Les 10 tests WhatsApp restés verts prouvent la neutralité.
- **Lire la convention du dépôt plutôt que sa propre spec** : j'avais écrit « 403 hors
  périmètre », le dépôt fait **404** partout (un 403 confirmerait l'existence de la ressource).
- **Vérifier l'état réel d'un merge** au lieu de croire « c'est mergé » : la PR #26 avait été
  mergée *avant* le push du second commit, qui serait resté orphelin.
- **Refuser en amont plutôt que laisser échouer en aval** : un fixe rangé dans la colonne mobile
  est rejeté avant l'appel UltraMsg — sinon l'API l'accepte, échoue à livrer, ou pire livre à un
  inconnu si le numéro a été réattribué.

## ❌ What Failed

- **`yesRemsTest` a dérivé** : colonnes `ComActionMessage.AdmUserId`, `AppFileObject.DeletedBy`,
  `StkItemType.StkKindId`, `CfgCompany.IsActive` absentes → 28 tests en `SQLGrammarException`.
  **La base de référence est `yesRemsCiCheck`.**
- **Spring Boot 4 + `starter-webmvc` n'autoconfigure PAS `RestClient.Builder`** → le contexte
  refusait de démarrer. Construire le client à la main (et poser enfin les délais).
- **`403` annoncé dans ma spec pour le hors-périmètre** — faux, c'est `404`, et
  `PerimetreInterceptor` le fait déjà pour `/api/prospections/{id}/**` : aucun code à écrire.
- **Avoir empaqueté de l'hygiène de données avec un prérequis** : la correction du `11`
  Canada/USA n'était pas nécessaire à la feature, je l'avais présentée comme telle. Corrigé après
  la question de l'utilisateur.
- **`nettoyer()` appelé avant d'être écrit** → 6 erreurs de compilation.
- **`SUM(CASE WHEN EXISTS (sous-requête))`** → T-SQL refuse un agrégat sur une sous-requête
  corrélée ; passer par une CTE + `LEFT JOIN` sur un `DISTINCT`.
- **`AS Plan`** comme alias de colonne → erreur de syntaxe T-SQL.
- **`Format-Table` tronque silencieusement** une colonne de droite : un `avec_event` invisible
  m'a fait relancer la requête pour rien.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 34 | `RestClient.Builder` absent du contexte (Boot 4 + starter-webmvc) | **Fixed** | Client construit à la main dans `EnvoiWhatsAppUltraMsg` |
| 35 | `yesRemsTest` : 4 colonnes manquantes, 28 tests en erreur | Contourné | Utiliser **`yesRemsCiCheck`** (voir Environment) |
| 36 | PR #26 mergée avant le push du 2ᵉ commit → WhatsApp orphelin | **Fixed** | 2ᵉ PR (#27) sur la même branche |
| 37 | `app.whatsapp.timeout-secondes` documenté mais appliqué nulle part | **Fixed** | Posé sur `SimpleClientHttpRequestFactory` |
| 38 | Correction du référentiel `AdmCountry` vivant **seulement** dans `yesRemsProd` | **Open** | Sera écrasée à la prochaine synchro depuis le `.115` — script rejouable |
| 32 | **VS Code retient les ports 9090 et 9091** → l'app ne démarre pas | **Open** | Ajouter `MANAGEMENT_SERVER_PORT=9092` à la run config |
| 11 | Règlement `CANCELED` : `sumAmountByComDocumentId` ne filtre pas sur le statut | **Open** | Latent |
| 12 | `POST /echeancier` sans `exclureDuPrix` | **Open** | Non corrigé |
| 13 | `S3_ENDPOINT` de la run config pointe la **production** | **Open** | Aucune écriture faite |

---

## 🔑 Key Points to Remember

### Le déclencheur, et ce qu'il ne fait pas

- La confirmation part quand `ProspectionService.addInteraction` crée un `AdmEvent` — soit sur
  les résultats **« Rendez vous pris », « RDV reporté », « Le prospect veut faire une visite du
  projet »**. **Aucune règle métier nouvelle** : c'est exactement l'ensemble qui alimente déjà le
  calendrier. Un test verrouille qu'une interaction ordinaire ne publie rien.
- `AdmEvent` est **vide en base légataire** (0 ligne) : le rendez-vous horodaté est une création
  *de notre backend*. Le legacy ne stockait qu'une date sans heure — `AlertValidityDate` est à
  minuit sur **8 166/8 166**.

### Les chiffres qui portent les décisions

| | |
|---|---|
| Prospections avec RDV | 5 078 |
| Email exploitable | **2 910 (57 %)** — 33 valeurs non vides sans `@` |
| Numéro exploitable | **5 027 (99 %)** |
| Tranche principale connue | 5 071 (99,9 %) |
| Adresse via **projet** | 5 071/5 071 — via tranche : 1 639 seulement |
| Article connu | **173 (3,4 %)**, dont 153 avec un plan |
| Volume | ~30 RDV/semaine, 1 569 sur 12 mois |

### Règles à ne pas défaire

- **Adresse : tranche si valide, sinon projet — jamais un panachage.** Deux garde-fous calibrés
  sur des cas réels : moins de 8 caractères (`YESTEST` porte littéralement « adresse ») ou égale
  au libellé du porteur (`MBR` recopie « Mansouria Beach Resort »). **Omettre plutôt qu'inventer.**
- **Seules les pièces typées `PLAN*` sont expédiables.** Sans ce filtre, le niveau projet
  enverrait au prospect tout document déposé — il existe précisément un contrat signé mal typé
  sur CITEB (sans risque aujourd'hui : 0 prospection ouverte).
- **Les plans ne partent que par mail.** UltraMsg exige une URL publique pour un document, les
  nôtres expirent en 15 min. Le texte WhatsApp n'en parle donc pas.
- **UltraMsg répond `200` même pour un refus** (`"sent":"true"` dans le corps) → `EnvoiWhatsApp.envoyer`
  renvoie un **booléen**, contrairement au port mail. Sans ça, `envoye` mentirait au commercial.
- **Pas de repli sur `TempTel` pour WhatsApp** : +8 prospections, et il faudrait supposer le
  Maroc — ce qui, sur les 225 tiers d'indicatif « 33 », enverrait à un inconnu.
- **La règle « 9 chiffres, commence par 6 ou 7 » ne vaut que pour envoyer**, et n'est mesurée que
  Maroc/France. À l'écriture on valide seulement 6–14 chiffres : 47 fiches portent un mobile
  nord-américain à 10 chiffres.

### Indicatif et format des numéros

- `MobileIndicatif` = indicatif **sans `+`** (« 212 » sur 14 078 tiers, « 33 » sur 225).
  `Mobile` = national **espacé sans zéro initial** (`6 61 22 48 57`), sur **14 285/14 458 (98,8 %)**.
  C'est la forme de stockage retenue : nos écritures restent indiscernables du legacy.
- **30 fiches portaient un indicatif ingérable** (`00`, `0393`, `1483`, `514`, `22`…) — des
  fautes de frappe. C'est toute la justification du sélecteur.
- **`GET /api/referentiels/pays` existait déjà** avec `indicatifTelephone` : rien à créer.
  Le sélecteur **ne peut pas** venir de `CfgTier` — il offrirait les valeurs parasites au menu.
- **Le référentiel plafonne le monde à 44 pays.** Le 45ᵉ sera refusé. Endpoint d'administration
  gardé en réserve, à n'écrire que si le cas se présente.
- Les 47 fiches en indicatif « 11 » sont **nord-américaines** ; une seule règle les couvre —
  garder les 10 derniers chiffres. **Non corrigé** (hygiène, pas prérequis).

### Ce qui est intestable tel quel

- **`@TransactionalEventListener(AFTER_COMMIT)` ne se déclenche JAMAIS sous test** →
  `@RecordApplicationEvents` pour la publication, appel direct du service pour l'envoi.
- **Un bean `@Async` injecté est un proxy** → ne pas passer par lui.
- La `@Formula` `hasFile` interroge `AppFileObject`, hors des espaces de requête d'Hibernate →
  `saveAndFlush` dans les fixtures.

### La topologie des bases (inchangée)

| Base | Rôle |
|---|---|
| `192.168.11.115` / `YesRems` | **source de vérité**, app de bureau |
| `yesRemsProd` (local, `49687`) | copie de travail — **c'est elle que le backend local utilise** |
| `yesRemsCiCheck` (local) | **base des tests** |
| `yesRemsDev` (VPS) | environnement déployé |

---

## 📁 Important Files & Paths

- `instructions/FEATURE-mail-confirmation-visite.md` — §2 les mesures, **§4 bis le WhatsApp**,
  §5 l'endpoint de renvoi, §7 ce qui reste à faire côté données.
- `instructions/FEATURE-indicatif-telephone.md` — §2 où la règle s'applique (et où non), §5 les
  règles V1–V5, §7 la rupture de contrat front.
- `src/main/java/.../services/prospection/ConfirmationVisiteService.java` — `ContenuVisite`
  résolu une fois, `parEmail()` / `parWhatsApp()` indépendants.
- `src/main/java/.../whatsapp/` — port, impl UltraMsg, `NumeroWhatsApp`, `NumeroMasque`.
- `src/main/java/.../utils/NumeroTelephone.java` — primitives partagées écriture ↔ envoi.
- `src/main/java/.../services/prospect/NumeroTelephoneSupport.java` — contrôle indicatif+numéro.
- **`../creer-prospection-test-tsiory.sql`** — hors dépôt. Crée `PSN-TEST-TSIORY` /
  `TSIORY NAHOMA` (`hei.tsiory@gmail.com`), tranche ARS, article ARS0814 (a un plan).
  **Déjà appliqué sur `yesRemsProd`.** Bloc de nettoyage en fin de fichier.
- **`../corriger-referentiel-indicatifs.sql`** — hors dépôt. Sénégal + 3 pays.
  **Déjà appliqué sur `yesRemsProd`** (40 → 44 indicatifs). À porter sur le `.115`.

---

## 🔧 Environment & Config

- **Tests — la commande qui marche** (`yesRemsTest` échoue, voir bug #35) :
  ```powershell
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
- **Run config IntelliJ** : ajouter **`WP_TOKEN`** et **`WP_INSTANCE`** (`instance110131`) à côté
  des `MAIL_*`, plus **`MANAGEMENT_SERVER_PORT=9092`** (bug #32). Secrets **non consignés ici**.
- **⚠️ Le jeton UltraMsg a circulé en clair dans la conversation** — à régénérer côté UltraMsg
  une fois les tests finis. Même dette que le PAT GHCR et la clé MinIO.
- Le backend local tourne contre **`yesRemsProd`** (vérifiable par
  `sys.dm_exec_sessions` filtré sur `program_name LIKE '%JDBC%'`).
- **⚠️ Ne pas lancer `mvnw clean`** pendant qu'une instance tourne depuis IntelliJ (bug #24).

---

## ⏭️ Next Step

> **Merger la PR de `feat/indicatif-telephone`** (`adbaac9`, 11 fichiers) :
> https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Findicatif-telephone?expand=1
>
> Puis `node .claude/scripts/git-flow.mjs sync back`.
>
> **Vérifier que le commit est bien dans `origin/main` après merge** — le piège s'est produit
> cette session (bug #36) : `git log origin/main..origin/<branche>` doit être vide.

### Test manuel encore à faire
- [ ] Relancer le backend **sur `main` mergé** avec `WP_TOKEN`/`WP_INSTANCE`, puis prendre un
      rendez-vous sur `PSN-TEST-TSIORY` avec le résultat « Le prospect veut faire une visite du
      projet ». Attendu : mail à `hei.tsiory@gmail.com` avec **1 plan joint** (~1,2 Mo) + adresse
      « 100 BD ZERKTOUNI ».
- [ ] Le WhatsApp **ne partira pas** en l'état : la fiche a `Mobile = 6 98 29 80 95` mais
      `MobileIndicatif` **vide** (demandé ainsi pour éprouver la règle). Motif attendu :
      `NUMERO_INEXPLOITABLE`. Pour un envoi réel :
      `UPDATE CfgTier SET MobileIndicatif='212' WHERE PKey='7C51013E-0001-4A00-9000-000000000001';`

### Backlog
- [ ] **Porter la correction du référentiel sur le `.115`** (bug #38), sinon la synchro l'efface
      et la validation se met à refuser le Sénégal.
- [ ] Relayer au dépôt frontend (autre lane), trois lots :
      **confirmation de visite** (endpoint de renvoi, réponse à **deux blocs** `email`/`whatsapp`) ;
      **indicatif** — **RUPTURE DE CONTRAT** : `POST`/`PATCH /api/prospects` renvoient 400 si un
      numéro arrive sans indicatif ; nouveaux champs `indicatifMobile`/`indicatifTelephone`,
      sélecteur alimenté par `GET /api/referentiels/pays` ;
      **liste prospections** (`telephone`, `email`, `derniereAlerteDate`/`Message`).
- [ ] **Déposer une plaquette/plan de masse par projet** (`Category = Yes.Cfg.CfgProject`,
      type `PLAN`). Le code le lit déjà : la couverture des plans passerait de 3 % à la
      quasi-totalité **sans une ligne de code**. ARS/ART/APF couvrent 7 299 des 7 676 ouvertes.
- [ ] Supprimer `5166463_t218682-12_Signed.pdf` sur CITEB — contrat signé typé `PLAN`, donc
      éligible à l'envoi (risque nul aujourd'hui, CITEB n'a aucune prospection ouverte).
- [ ] **Écran d'affectation en masse des leads** — 2 994 dormants. Le plus rentable du backlog.
- [ ] Renseigner l'adresse de `APO` et `LAZ` (projets sans adresse).
- [ ] Les 47 fiches en indicatif « 11 » → `1` + 10 derniers chiffres (hygiène, script prêt dans
      l'historique de conversation, retiré du script livré).
- [ ] Endpoint d'administration des pays, **si et seulement si** le plafond des 44 gêne.
- [ ] Lien de téléchargement longue durée — débloquerait les plans sur WhatsApp. Choix de
      sécurité à part entière, resté ouvert depuis le premier chantier.
- [ ] `ProspectListItemDto` n'expose pas l'indicatif : la liste affiche un numéro incomplet.
- [ ] Réparer les 24 indicatifs parasites restants — la validation les fera remonter d'elle-même.
- [ ] Règlement annulé : filtrer `sumAmountByComDocumentId` sur le statut (bug #11).
- [ ] `exclureDuPrix` : le dériver de la catégorie à défaut (#12).
- [ ] Garde-fou sur `S3_ENDPOINT` en local (bug #13).
- [ ] Sauvegarde du bucket MinIO — seule copie active des 8 Go.
- [ ] Déclarer le check `Tests` comme requis sur `main`.
- [ ] Révoquer le PAT GHCR, la clé MinIO **et le jeton UltraMsg** passés en clair.
