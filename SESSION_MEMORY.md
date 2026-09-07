# Session Memory
_Last updated: 2026-09-07T18:05+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective
Aligner l'API sur l'application de bureau légataire : **seules ADMOD et GHRIZA sont en
exploitation**, et **toute** donnée visible ou manipulable doit provenir de ces deux entités. Les
deux chantiers précédents (visibilité pilotée par `CfgCompany.IsActive`, fiche de réservation par
estampillage) sont poussés ; ce troisième les complète en rendant le filtre **transversal** au lieu
de le laisser cantonné au patrimoine et aux référentiels.

---

## ✅ What Worked

- **Le point d'accroche prévu par la session précédente était le bon.** Composer le prédicat
  d'entité dans les **5 méthodes `filtreXxx()` de `PerimetreCommercialService`** couvre 9 sites
  d'appel — dossiers, prospections ×3, encaissements ×2, devis, interactions — **sans modifier un
  seul service**.
- **Choisir la même sémantique que le périmètre pour les lignes non résolues.** Décision
  utilisateur : « entité non résoluble ⇒ visible », identique au §6.4 du périmètre. Les deux
  prédicats se composent alors sans se contredire, et la lambda est unique par méthode.
- **Mesurer la FK directe avant de s'y fier.** `StlSettlement.CfgCompanyId` concorde avec la
  société déduite du dossier sur **644 règlements de vente sur 644**, aucun règlement à cheval sur
  deux sociétés. D'où un prédicat direct sur la racine au lieu d'une sous-requête `EXISTS`.
- **Vérifier une régression par `git stash` plutôt que par raisonnement.** Quatre tests cassés
  « ressemblaient » à des défauts de fixtures préexistants. Sur la base de référence ils passaient :
  c'était bien mon changement. Sans cette vérification, une vraie régression CORS partait en
  production.
- **Une lambda unique par filtre, pas `Specification.and()` de deux spécifications** — sinon chacune
  repose ses propres `LEFT JOIN` et la requête porte la chaîne article → tranche → projet en double.

## ❌ What Failed

- **`cb.isTrue(coalesce(...))` sur SQL Server** → « Expression de type non booléen spécifiée dans un
  contexte où une condition est attendue ». **9 tests hotline cassés.** SQL Server n'a pas de type
  booléen : sur un chemin simple Hibernate rend `= 1`, sur une expression composée il émet
  l'expression nue. Toujours `cb.equal(expr, Boolean.TRUE)`.
- **Faire traverser la préflight CORS par l'intercepteur de périmètre.** Le contrôle devenu
  inconditionnel refusait les `OPTIONS`, ce qui aurait cassé **tout `PATCH` cross-origin** du
  frontend, y compris sur des ressources visibles. Corrigé par `CorsUtils.isPreFlightRequest`.
- **`path.get("cfgCompany").get("isActive")` en Criteria API** produit une **jointure interne
  implicite** et masque les lignes sans société. Toujours `join("cfgCompany", JoinType.LEFT)`.
- **Ne pas reprendre le retour de `save()`** sur une entité à id assigné : Spring Data passe par
  `merge()` et rend une *autre* instance. Rattacher l'originale fait voir à Hibernate une référence
  transiente au flush. Ce piège a mordu ma propre fixture **et** deux tests existants.
- **`-Dtest=A+B+C`** n'est pas la syntaxe de surefire (c'est `A,B,C`), et sous PowerShell tout
  `-Dxxx.yyy` doit être **entre guillemets** sinon Maven lit un phase de cycle de vie.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 1 | Suite injouable en CI : schéma légataire versionné nulle part | Fixed | `ci/schema/` + `.github/workflows/ci.yml` |
| 2 | `StkItemType`/`StkItemCategory` ne mappaient pas `StkKindId`, pourtant `NOT NULL` | Fixed | Association ajoutée + 105 `setStkKind` dans les fixtures |
| 5 | Formule « SOCIETE {nom} (groupe AYAD DEVELOPPEMENT) » appliquée à toute entité | Fixed | Paragraphe par marque |
| 6 | Clause d'immobilisation 10 % inconditionnelle | Fixed | APF seulement |
| 8 | `EntitesJuridiquesActives` figeait ADMOD+GHRIZA dans le code | Fixed | Bascule sur `CfgCompany.IsActive` + migration `005` |
| 10 | `GET .../documents/fiche-reservation` renvoie 409 sur un dossier ART | **Fixed** | **Clos par la visibilité transversale** : ces dossiers ne sont plus adressables, 404 en amont. Aucun gabarit MBR/ART à dessiner |
| 14 | `cb.isTrue(coalesce(...))` → SQL non booléen, 9 tests hotline | Fixed | `cb.equal(expr, Boolean.TRUE)` |
| 15 | **Préflight CORS refusée par `PerimetreInterceptor`** devenu inconditionnel | Fixed | Saut explicite sur `CorsUtils.isPreFlightRequest` |
| 16 | `DevisTest` : `StkItem` sans type/catégorie/vocation, jamais flushé donc jamais validé | Fixed | Référentiels posés dans les 2 fixtures |
| 17 | `ProspectionTranchePrincipaleTest` : `CfgTranche` détachée rattachée à une prospection gérée | Fixed | Retour de `save()` repris |
| 11 | Règlement annulé (`CANCELED`) garde ses lignes ; la somme des montants réglés ne filtre pas sur le statut | **Open** | Latent : aucune annulation n'est encore passée par l'API |
| 12 | `POST /echeancier` sans `exclureDuPrix` : la catégorie ne l'implique pas | **Open** | Non corrigé |
| 13 | Config IntelliJ : `S3_ENDPOINT` pointe la **production** | **Open** | Un lancement manuel écrit dans le vrai bucket MinIO |

---

## 🔑 Key Points to Remember

### Base de données — la règle
- **Toute étude de motif se fait contre `yesRemsProd`**, pas `yesRemsTest`. Base de **lecture** :
  les corrections se livrent en script SQL que l'utilisateur applique lui-même.
- **`yesRemsTest` est obsolète.** La suite tourne contre **`yesRemsCiCheck`**, via
  `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (voir Environment).

### Visibilité par entité — l'architecture retenue
- **`CfgCompany.IsActive` est déjà correct en base** : ADMOD (1 projet, 2 tranches) et GHRIZA
  (1 projet, 3 tranches) seules actives. Les 5 tranches correspondent exactement à la grille de
  l'application de bureau (`APF-HIDDEN`, `APF`, `ARB-ALC`, `ars hidden`, `ARS`). **La migration
  `005` n'a plus besoin d'être rejouée sur `yesRemsProd`.**
- **`VisibiliteEntites`** (`services/perimetre/`) : fabrique **statique** de prédicats, sans état —
  le filtre ne dépend pas de l'appelant. Trois formes : `depuisProjet`, `depuisSocieteDirecte`
  (encaissements), `depuisDeuxProjets` (interactions, prospection d'abord puis dossier).
- **Deux restrictions distinctes, superposées dans `PerimetreCommercialService`** : le périmètre
  (par compte, opt-in) et l'entité (**globale, permanente, appliquée même aux rôles exemptés dont
  `Administrateur`**). Le nom des méthodes `filtreXxx()` ne dit que la première — c'est documenté
  en tête de classe, ne pas s'y laisser prendre.
- **Le contrôle par id n'est plus conditionnel.** `verifierAccesXxx()` exécute désormais toujours
  une requête (`existsDansPerimetre` si restreint, sinon `existsVisible`). Sans cela le filtre ne
  tiendrait que sur les listes et se contournerait en énumérant les ids.
- **Conséquence non évidente** : cette requête JPQL déclenche un **auto-flush Hibernate plus tôt**
  qu'avant dans les tests `@Transactional`. Elle a révélé deux fixtures invalides qui survivaient
  uniquement parce qu'elles n'étaient jamais flushées avant le rollback (bugs #16 et #17). Si un
  test casse ainsi, la fixture est en cause, pas le filtre.
- **Les prospects ne sont pas filtrés** (décision utilisateur) : `CfgTier` n'a aucun lien vers une
  entité, un prospect est une personne, pas un bien. **Les clients le sont**, mais par
  construction : « client » = tier porté par au moins un `ComFolder` visible.

### Volumétrie du filtre (mesurée sur `yesRemsProd`)
Dossiers 358→116 · prospections 17 823→11 787 · encaissements de vente 644→332 · devis 298→71 ·
interactions 48 118→30 668. Restent visibles car sans entité résoluble : **192 prospections**,
**1 644 interactions**.

### Échéancier / règlements — modèle réel
- `ComDocument` (échéance, liée au dossier), `StlSettlement` (règlement, **sans lien vers le
  dossier**), `StlSettlementLine` (imputation polymorphe). Relation plusieurs-à-plusieurs réelle.
- `GET /api/encaissements` est **déjà restreint aux règlements de vente** (au moins une ligne
  portant un `ComDocumentId`) : les 2 599 règlements d'achat ne sont pas concernés.
- Les remboursements sont des **montants négatifs**. La suppression d'un règlement existe déjà
  (`PATCH .../reglements/{id}/statut` → `CANCELED`) ; suppression physique exclue.

### Sécurité / auth
- Cookie JWT `SameSite=Strict` ⇒ **suppose le front sur `tsiory.tech`**. `JWT_SECRET` fait
  exactement 32 caractères, le minimum HS256.
- Rôle `Administrateur` ∈ `RolesExemptesPerimetre` ⇒ aucun périmètre — **mais subit le filtre
  d'entité**, c'est précisément ce que verrouille `VisibiliteEntitesActivesTest`.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-visibilite-transversale-entites.md` — le contrat : règles R1–R5, volumétrie
  mesurée, surface à modifier, **et la section « Impact front »** à relayer à l'autre dépôt.
- `src/main/java/.../services/perimetre/VisibiliteEntites.java` — la fabrique de prédicats, avec les
  deux pièges documentés (jointure implicite, `isTrue` sur SQL Server).
- `src/main/java/.../services/perimetre/PerimetreCommercialService.java` — composition des deux
  restrictions ; `combine()` et le nouveau squelette `verifier()`.
- `src/main/java/.../web/PerimetreInterceptor.java` — saut de la préflight CORS.
- `src/test/java/.../controller/perimetre/VisibiliteEntitesActivesTest.java` — 8 tests ; le
  contre-test `prospectionSansTranche_doncAucuneEntite_resteVisible` est celui qui distingue une
  implémentation correcte d'une jointure interne.
- `src/main/java/.../services/dossier/document/GabaritFicheReservation.java` — coordonnées mesurées
  sur les fonds. **Source de vérité, pas le `.yes`.**
- `ci/schema/*.sql` + `.github/workflows/ci.yml` — schéma de test versionné. **Mergés.**

---

## 🔧 Environment & Config

- **Tests** : `495 tests, verts`. Lancer avec
  `$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"`
  (+ `USERNAME=tsix`, `PASSWORD=securePassword123`) puis `.\mvnw.cmd clean test`.
  Ajouter `"-Dspring.jpa.show-sql=false"` — **guillemets obligatoires sous PowerShell** — sinon la
  sortie est noyée sous le SQL de Hibernate.
- **SQL Server local** sur `49687`, user `tsix` : `yesRemsProd` (schéma complet + données réelles),
  `yesRemsCiCheck` (reconstruite depuis `ci/schema`), `yesRemsTest` (obsolète).
- **Compte de développement** : `t.admin` dans `yesRemsProd`, rôle `Administrateur`.
- **`CfgCompany.IsActive` en base `yesRemsProd`** : ADMOD et GHRIZA = 1, les 7 autres = 0. ✅ Conforme.

---

## ⏭️ Next Step

> **Committer, pousser, et faire fusionner la PR sur GitHub.** La branche
> `feat/entites-actives-et-fiche-reservation` porte alors trois unités : entités actives, fiche de
> réservation, visibilité transversale. Puis `sync` avant toute nouvelle branche.
>
> **À relayer au dépôt frontend** (autre PR, autre lane — ne pas le faire depuis ici) : la section
> « Impact front » de `FEATURE-visibilite-transversale-entites.md`. En particulier
> `GET /api/dossiers/{id}` peut désormais renvoyer **404** sur un id qui répondait 200 la veille,
> et tous les volumes de liste chutent.

### Backlog
- [ ] Rejouer `005_CfgCompany_IsActive.sql` sur **`yesRemsDev` (VPS)** — `yesRemsProd` est déjà bon.
- [ ] Déclarer le check **`Tests` comme requis** sur `main` — sans ça le workflow ne bloque rien.
- [ ] Garde-fou sur `S3_ENDPOINT` en local : un lancement manuel écrit dans le bucket de production.
- [ ] Règlement annulé : filtrer `sumAmountByComDocumentId` sur le statut du règlement (bug #11).
- [ ] `exclureDuPrix` : le dériver de la catégorie à défaut, et exposer l'écart au prix TTC (#12).
- [ ] Les DELETE manquants — commencer par les pièces jointes (`AppFileObject.DeletedAt` existe déjà,
      la lecture filtre dessus, **rien ne l'écrit jamais** : il ne manque que contrôleur et service).
- [ ] Endpoint de désactivation d'un compte (`AdmUser.Disabled` lu partout, jamais écrit).
- [ ] `deploy.yml` ne joue toujours pas les tests : la barrière est sur la PR, pas sur `main`.
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic` (4 fichiers réels).
- [ ] Sauvegarde du bucket MinIO — seule copie active des 8 Go.
- [ ] Révoquer le PAT GHCR et la clé de service MinIO passés en clair dans des conversations.
