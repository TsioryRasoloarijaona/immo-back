# Session Memory
_Last updated: 2026-09-22T19:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Fiabiliser la chaîne complète leads → articles → dossiers en s'alignant sur le légataire
OMRAN**, avec la trace d'audit comme source de vérité. Session du 22/09 : archivage de la
trace, fix auteur des interactions, mail de visite avec localisation, insertion manuelle des
leads Ferme Bretonne sur le 115, test endpoint↔base (écart pré-CT résorbé), sélection
d'articles alignée sur la règle `IsUnderItem` d'OMRAN, dashboard Grafana « vue générale ».

---

## ✅ What Worked

### Livré et MERGÉ (main = tout jusqu'à #68)
1. **`feat/archivage-trace-omran`** (#66) : l'audit tourne sur 20×100 Mo et supprime les plus
   anciens → `ArchivageTraceOmran` copie la part OMRAN vers `AppTraceOmran` (migration 010,
   appliquée sur yesRemsProd — **reste yesRemsDev**). Aucune clé naturelle dans
   `fn_get_audit_file` (18 848 lignes brutes strictement identiques) → purge-et-rejeu de la
   borne en `>=`, lecture ordonnée, borne haute −5 min. Banc : 42 852 lignes archivées,
   idempotence prouvée. Activation : deux-bases + `AUDIT_TRACE_PATH` (vide = inactif).
2. **`fix/auteur-interactions`** (#67) : une action saisie dans le CRM s'affichait « Système ».
   L'écriture était bonne ; la lecture dérivait `employeNom` du seul `CfgEmployeeId` (77 %)
   au lieu d'`UserName` (99,996 %). Règle unique `ProspectionService.nomAuteur`
   (employé → UserName → UserCreate), partagée avec Hotline.
3. **Mail de visite** (#68, même branche) : plus de plan joint ; adresse R4 + **lien Google
   Maps** (`lienCarte`) dans mail HTML/texte/WhatsApp. Aucun GPS en base (0/1 068 lots) →
   le lien est construit sur l'adresse. `nombrePiecesJointes` = toujours 0 (champ conservé).

### En attente de merge : `fix/selection-articles-hors-annexes` (3 commits)
- **`IsUnderItem = 0`** : la sélection d'articles (`/api/referentiels/articles`) écartait déjà
  les sous-articles (ParentId) mais montrait **241 parkings RACINE**. La trace d'audit (écran
  Articles rejoué) a donné la règle exacte d'OMRAN : `IsUnderItem = 0 AND ParentId IS NULL`.
  Mesuré : PARKING/TERRASSE/JARDIN tous à IsUnderItem=1 (même racines), APPARTEMENT/COMMERCE/
  LOT à 0, zéro NULL ; lot principal des 358 dossiers = toujours APP (353) ou COM (5).
  Exclusion désactivée si `categorieId` explicite. 1re version par codes de catégories
  remplacée (21837a5 → règle du drapeau).
- **`monitoring/grafana-vue-generale.json`** (64a60bc) : dashboard 27 panneaux, uid
  `immo-back-general` — santé (8 stats, « API DOWN »/« MONO-BASE ? » sur no-data), HTTP
  (débit par code, p50/95/99, top appelés/lents, 5xx par endpoint), synchro résumé, pools
  Hikari (actives/max, pending+timeouts, acquisition, top repositories), JVM/CPU/disque/logs.
  Toutes les métriques vérifiées présentes sur :9091/actuator/prometheus.

### Opérations sur les bases (pas de code)
- **7 pré-prospections FERME BRETONNE insérées sur le VRAI 115** (n8n à l'arrêt depuis le
  08/09) : motif n8n copié (Code='50', TempTel 9 chiffres sans 0, StrEntity=ComPreProspection,
  origine FACEBOOK, vocation VENTE, type WEB, nature « 1 A TRAITER », tranche APF
  `c273dec7-08d1-4256-9e3c-efa1485c4ee8`). Script idempotent (PKey figées + NOT EXISTS),
  7/7 visibles dans `ComSalePreProspectionView`. 6 des 7 personnes = re-soumissions (une
  ligne PAR soumission est le comportement légataire).
- **Test endpoint ↔ yesRems local** : 20 articles APF OCCUPE sur la source mais DISPONIBLE
  via l'API — réservations d'août **antérieures à l'activation du CT** (version 109),
  invisibles de CHANGETABLE, filigranes « à jour ». Remède : `DELETE FROM AppSyncEtat`
  (copie) → baseline intégrale au cycle suivant → **0 écart** sur les 80 articles APF.
- **Grand nettoyage git** (demandé) : 26 branches distantes + toutes les locales supprimées,
  il ne reste que `main` des deux côtés.

### Méthode (toujours payante)
- **Mesurer avant de concevoir** : a invalidé 2 designs d'archivage, remplacé la liste de
  catégories par `IsUnderItem`, et confirmé notre `StatutArticle` (aucun `StrStatus` à
  `BoolField009=1` → la dispo OMRAN se réduit à `ComFolderId IS NULL`).
- **La trace d'audit reconstituée** : fragments `sequence_number` concaténés = requête
  complète de l'écran (fichier temporaire, méthode réutilisable).
- `git commit -F <fichier scratchpad>` pour les messages multilignes.

---

## ❌ What Failed

- **Clé naturelle pour dédupliquer l'audit** (offset, puis +object_name) → vraies lignes
  identiques ; seul le filigrane discipline l'exactement-une-fois.
- **Filigrane strict `>` + lots coupés aux ticks** → cassé par batchUpdate qui commite par
  instruction ; design final = purge-et-rejeu `>=`.
- **`POST /api/auth/login`** → 401 trompeur : le chemin est **`/api/auth/connexion`** (une
  URL inconnue protégée rend 401, pas 404), corps `{"username","password"}`.
- **Cookie JWT via PowerShell** → jamais envoyé sur http://localhost (cookie `Secure`,
  exception navigateur absente de .NET) → **utiliser `curl.exe -c/-b`**.
- **Exclusion des annexes par codes de catégories** → correcte mais fragile ; la règle
  légataire `IsUnderItem` la remplace.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 26 | Actions affichées « Système » au lieu de l'auteur | **Fixed** (#67) | `nomAuteur` : employé → UserName → UserCreate |
| 27 | Copie du banc divergente (20 articles réservés affichés dispo) | **Fixed** (banc) | Écart pré-CT → baseline via `DELETE AppSyncEtat`. **Risque identique en prod au 1er déploiement deux-bases** : ne pas pré-remplir les filigranes si la copie n'est pas restaurée au moment de l'activation du CT sur le 115 |
| 28 | Sélection d'articles noyée de parkings racine | **Fixed** (PR en attente) | `IsUnderItem = 0` (règle légataire) |
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 (déjà 623 h de trou historique) | **Open** (externe) | Leads insérés à la main ; monitoring proposé (voir backlog) |
| 30 | Heure de RDV non captée (front `type="date"` ; OMRAN ne la capte pas non plus — 2 454 RDV à minuit, l'heure en texte libre) | **Open** (front) | Backend prêt (`dateRdv` LocalDateTime) ; passer le champ en datetime-local côté front |

---

## 🔑 Key Points to Remember

- **Trace d'audit** : `Audit_Global` → `C:\backup\*.sqlaudit`, rotation 20×100 Mo, capte tout
  le DML de `yesRems`. `action_id` complété d'espaces. Horodatage **UTC**. `AdmEvent` est
  **vide** sur le 115 (OMRAN ne structure pas ses RDV ; date dans `AlertValidityDate` à minuit).
- **Disponibilité article** : OCCUPE ⇔ `StkItem.ComFolderId` non nul — confirmé par la
  requête OMRAN elle-même. Annexe ⇔ `IsUnderItem = 1`. OMRAN filtre aussi par vocation
  (écart connu non repris).
- **Banc local** : lectures = yesRemsProd (`hikari-lecture`), écritures = yesRems
  (`hikari-source-de-verite`) ; visible via `sys.dm_exec_sessions`. Compte de test API :
  `Khadija` (AppUserCredential dans **yesRemsProd** ; login `readOnly` → lit la copie).
  Le 115 : compte `sasa` (mot de passe détenu par l'utilisateur).
- **Adresses** : `CfgProject`/`CfgTranche` ont Address/City/ZipCode (remplies 6/8 et 13-14/16)
  mais parfois l'adresse du siège, pas du site ; APO et LAZ sans adresse ; aucun GPS nulle part.
- **Rôles** : `ROLE_<AdmRole.Code>` ; sync manuelle : Commercial/Chef/Aide + admin.
- **Synchro** : vérifier via `AppSyncEtat` + contenu des tables, jamais le log ; `sync_*`
  seulement en deux-bases ; **un écart antérieur au CT est invisible des métriques**.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-archivage-trace-omran.md`, `FIX-auteur-interactions.md`,
  `FEATURE-mail-visite-localisation.md`, `FIX-selection-articles-hors-annexes.md` — specs 22/09.
- `src/main/java/.../config/ArchivageTraceOmran.java` + `db/migration/010_AppTraceOmran.sql`
  (appliquée yesRemsProd ; **à appliquer sur yesRemsDev**).
- `src/main/java/.../services/patrimoine/StkItemSpecifications.java` — `horsAnnexes()`.
- `monitoring/grafana-vue-generale.json` — dashboard à importer (uid `immo-back-general`).
- Scratchpad : `insert-preprospections-ferme-bretonne.sql` (déjà exécuté sur le 115).

---

## 🔧 Environment & Config

- Banc deux-bases : `DB_*`=yesRemsProd, `SOURCE_DB_*`=yesRems, `localhost,49687` (tsix).
  Backend lancé depuis IntelliJ (8080 + actuator 9091). Audit :
  `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis master.
- Nouvelles variables : `AUDIT_TRACE_PATH` (vide = inactif), `AUDIT_TRACE_MINUTES` (60).
- Tests : `./mvnw.cmd test` — **931 verts** au dernier passage (~4 min).
- Recette test API locale : `curl.exe -c jar -X POST /api/auth/connexion` puis `-b jar`.

---

## ⏭️ Next Step

> **Merger la PR `fix/selection-articles-hors-annexes`** (règle IsUnderItem + dashboard),
> puis `git checkout main && git pull`, supprimer la branche, et **importer
> `monitoring/grafana-vue-generale.json` dans le Grafana du VPS**.

### Backlog
- [ ] Appliquer `010_AppTraceOmran.sql` sur **yesRemsDev** ; poser un audit sur le **115**
      avant d'activer `AUDIT_TRACE_PATH` en prod.
- [ ] **1er déploiement deux-bases en prod** : ne PAS pré-remplir `AppSyncEtat` (payer la
      baseline) sauf si la copie est restaurée à l'instant de l'activation du CT (bug #27).
- [ ] Moniteur n8n : jauge `leads.dernier.timestamp` + alerte 24 h (écart moyen mesuré 3,5 h) ;
      côté n8n : Error Workflow + vérifier le workflow actif après redémarrage ; **VPN :
      mettre à jour la credential SQL de n8n AVANT de fermer la redirection du 115**.
- [ ] Heure de RDV : champ datetime côté front (bug #30) ; option backend `EndHour`/`AllDay`.
- [ ] Adresses projets : compléter APO et Villa Lazrak, corriger les adresses « siège »
      (ARS, ART, tranches MBR) — condition pour que le lien carte du mail pointe juste.
- [ ] Décider si d'autres rôles déclenchent la sync manuelle ; sous-tables hors CT restantes.
