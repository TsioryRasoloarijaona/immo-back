# Session Memory
_Last updated: 2026-09-08T12:05+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Combler les manques du backlog, deux features livrées dans cette session :

1. **Suppression des pièces jointes** — *mergée* (PR #16). Le socle existait entièrement
   (`AppFileObject.DeletedAt` né avec la migration `002`, respecté par la `@Formula` `hasFile` et
   par `findByAdmAttachedFileIdAndDeletedAtIsNull`) mais **aucun code ne l'écrivait**.
2. **KPI de performance des commerciaux** — *poussée, en attente de merge*. Deux endpoints :
   activité, réactivité, transformation, valeur.

---

## ✅ What Worked

- **Explorer les données AVANT de spécifier.** Sur les KPI, l'exploration a renversé le choix
  d'attribution évident (voir §Key Points) — une heure de `sqlcmd` a évité une feature qui aurait
  attribué à des commerciaux des appels qu'ils n'ont jamais passés.
- **Mesurer pour trancher un désaccord de doc.** Le javadoc de `ReglementStatutService` interdisait
  la garde 409 sur la suppression ; les chiffres ont montré que son argument (risque rétroactif) ne
  s'appliquait pas. Décision prise, javadoc **amendé** plutôt que contredit en silence.
- **`UserCreate` comme arbitre.** Pour savoir laquelle de deux colonnes désigne l'auteur d'une
  interaction, comparer chacune à la trace textuelle laissée par l'application Windows. Résultat
  sans appel : 37 186/37 186 contre 30 894/37 186.
- **Réutiliser les patrons déjà en place** plutôt qu'inventer : requêtes `…DansPerimetre` en
  double variante, `(soc.isActive = true OR soc.id IS NULL)` sur chaque agrégat, projection agrégée
  en Java comme `DashboardService`, `@PreAuthorize("hasRole('Administrateur')")`.
- **Séparer `exigerFichier` de `marquerSupprime`** : c'est ce qui permet d'intercaler une règle
  métier entre le 404 et l'action, donc de ne pas renvoyer 409 sur une pièce inconnue.
- **15 tests KPI verts du premier coup** — les fixtures ont été bâties en copiant
  `VisibiliteEntitesActivesTest`, qui contient déjà toute la chaîne société → projet → tranche.

## ❌ What Failed

- **`save()` au lieu de `saveAndFlush()`** dans `marquerSupprime` → 3 tests rouges, la pièce
  supprimée restait listée. Une **`@Formula` n'entre pas dans les espaces de requête** de son
  entité : Hibernate ignore que la liste dépend d'`AppFileObject` et ne flushe pas avant le
  `SELECT`.
- **Le flush ne suffit pas si l'entité a déjà été chargée.** Un 4ᵉ test restait rouge : le
  pointage avait chargé les `AdmAttachedFileSummary`, et Hibernate rend **l'instance du cache de
  premier niveau**, formule figée comprise. Corrigé par `entityManager.clear()` dans le test —
  pas par un contournement en production, où chaque requête a son propre contexte.
- **`ComFolder.StkHierarchyId` n'existe pas.** Le dossier passe par `StkItem` :
  `ComFolder.StkItemId → StkItem.StkHierarchyId`. La note d'une session précédente laissait
  croire le contraire.
- **`SUM(CASE WHEN EXISTS(...))` en T-SQL** → « Impossible d'exécuter une fonction d'agrégation sur
  une expression comportant un agrégat ou une sous-requête ». Passer par une CTE.
- **Comparaison de chaînes entre `AdmUser` et `CfgTier`** → conflit de collation
  (`SQL_Latin1_General_CP1_CS_AS` vs `French_CI_AS`). Ajouter `COLLATE French_CI_AS`.
- **`sqlcmd` a rendu un timeout de connexion** sur `yesRemsCiCheck` alors que `yesRemsProd`
  répondait. `-l 30` règle le problème — ne pas en conclure que la base est absente.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 14 | `cb.isTrue(coalesce(...))` → SQL non booléen | Fixed | `cb.equal(expr, Boolean.TRUE)` |
| 15 | Préflight CORS refusée par `PerimetreInterceptor` | Fixed | Saut sur `CorsUtils.isPreFlightRequest` |
| 18 | Pièce supprimée toujours listée : `@Formula` hors auto-flush | Fixed | `saveAndFlush` dans `marquerSupprime`, documenté sur place |
| 19 | Cache de premier niveau : `hasFile` figé à sa valeur de chargement | Fixed (test) | `entityManager.clear()` — aucun impact production |
| 20 | `ComActionMessage.CfgEmployeeId` prise pour l'auteur | Fixed | Mapping d'`AdmUserId` + avertissement en javadoc sur le champ piégeux |
| 21 | **Migration `006` non appliquée en prod** | **Open** | `DELETE` de pièce jointe échouera sur `yesRemsProd`/`yesRemsDev` tant que le script n'est pas joué |
| 11 | Règlement `CANCELED` : `sumAmountByComDocumentId` ne filtre pas sur le statut | **Open** | Latent, aucune annulation encore passée par l'API |
| 12 | `POST /echeancier` sans `exclureDuPrix` : la catégorie ne l'implique pas | **Open** | Non corrigé |
| 13 | Config IntelliJ : `S3_ENDPOINT` pointe la **production** | **Open** | Un lancement manuel écrit dans le vrai bucket MinIO |

---

## 🔑 Key Points to Remember

### Base de données — la règle
- **Toute étude de motif se fait contre `yesRemsProd`** (lecture seule ; les corrections se livrent
  en script SQL que l'utilisateur applique). `yesRemsTest` est obsolète ; la suite tourne contre
  **`yesRemsCiCheck`**, reconstruite depuis `ci/schema/`.
- Chemin société d'un dossier : `ComFolder.StkItemId → StkItem.StkHierarchyId → CfgTranche.CfgProjectId → CfgProject.CfgCompanyId`.
  Pour une prospection, `StkHierarchyId` est **direct**. Pour une interaction, `COALESCE(prospection, dossier)`.

### KPI commerciaux — l'attribution, décision structurante
- **L'activité est attribuée à son AUTEUR** (`ComActionMessage.AdmUserId`, **mappé par cette
  feature**) ; le **portefeuille au commercial ASSIGNÉ** (`CfgCommercialId`). Les deux coïncident
  dans 86 % des cas et l'écart est ce qu'on veut voir.
- **`ComActionMessage.CfgEmployeeId` n'est PAS l'auteur** — c'est l'employé *concerné*. Renseignée
  à 77 % seulement, et là où les deux existent elle désigne une personne **différente 844 fois**.
  Arbitrage par `UserCreate` : compte **37 186/37 186**, employé 30 894/37 186. Le champ porte
  désormais cet avertissement en javadoc.
- **La clé de regroupement est le commercial (`CfgTier`), jamais le compte** : un employé porte
  parfois deux comptes (1 cas mesuré). Le sens compte→employé est N→1, donc sûr ; l'inverse non.
- **`/api/kpi` n'est pas dans `COLLECTIONS_SCOPEES`** de `PerimetreInterceptor` : périmètre et
  filtre d'entité appliqués **explicitement à chaque agrégat**, comme `DashboardService`. Point de
  fuite de la feature, verrouillé par `activiteSurUneEntiteInactive_nestCompteeNullePart`.
- **`ComFolder.ReservationDate` est inutilisable** : 2 lignes sur 358. Le seul jalon de signature
  peuplé est `AgreementDate` (73/358) — c'est lui pour un futur KPI « vente signée ».
- Codes d'interaction figés dans `domain/InteractionKpi` : `Téléphone Sortant` (40 325),
  `Téléphone entrant` (867), `Visite` (4 311), `Le prospect veut faire une visite du projet`
  (5 715), `Rendez vous pris` (2 444). **Non seedés dans `ci/schema`** — les tests les créent.
- **« Visite décrochée » ≠ « visite réalisée »** : engagement obtenu au téléphone vs visite
  consignée. Ne jamais les additionner.
- Volumétrie (entités actives, 2025-2026) : 23 commerciaux actifs, 6 533 prospections dont
  **2 117 jamais contactées (32 %)**, délai moyen de première réponse 109 h, 36 dossiers créés.

### Suppression de pièce jointe — l'architecture retenue
- **Logique, jamais physique** : `DeletedAt` + `DeletedBy`. La ligne `AdmAttachedFile` reste (elle
  porte les métadonnées et l'auteur du dépôt) et bascule à `hasFile = false`.
- **L'objet S3 n'est jamais supprimé**, ni dans la transaction ni après : balayage hors ligne
  (`FEATURE-s3-storage.md` §3.4). Symétrique du dépôt, qui écrit S3 avant la base pour que la seule
  fuite possible soit un objet orphelin, jamais une ligne pointant vers le vide.
- **Deux fois le même DELETE ⇒ 404.** L'information utile est que la pièce n'est plus là.
- **`DeletedBy` est une colonne neuve** (migration `006`). Pas de réutilisation de
  `AdmAttachedFile.UserUpdate`, peuplée à 3 410/3 410 par l'application Windows.
- **L'Article reste en lecture seule** : `/api/articles` n'est pas couverte par
  `PerimetreInterceptor`, y ouvrir une écriture demande d'abord le contrôle de visibilité.
- **Garde 409** : un règlement `POINTED` ne peut pas perdre sa dernière preuve. Exception assumée
  au javadoc de `ReglementStatutService`, qui a été amendé. 17 `POINTED` en base, dont 3 avec
  exactement une pièce — les 3 seules lignes où le 409 peut se déclencher.

### Pièges Hibernate mis au jour (valables au-delà de ces features)
- Une **`@Formula` n'entre pas dans les espaces de requête** de son entité : Hibernate ignore
  qu'elle dépend d'une autre table et ne flushe pas avant la lecture. `saveAndFlush` obligatoire.
- Le **cache de premier niveau rend l'instance mémorisée**, formule figée comprise. Un test
  `@Transactional` qui écrit puis relit doit `clear()` pour reproduire la production.
- **Ne pas rattacher l'instance passée à `save()`** sur une entité à id assigné : Spring Data passe
  par `merge()` et rend une *autre* instance gérée.

### Sécurité / visibilité
- Deux restrictions superposées : **périmètre commercial** (par compte, opt-in) et **visibilité par
  entité juridique** (globale, permanente, appliquée même aux rôles exemptés dont `Administrateur`).
- Règle commune : **ligne sans projet/société résoluble ⇒ visible de tous** (§6.4 / R2). C'est ce
  qui permet de composer les deux prédicats sans qu'ils se contredisent.
- Hors visibilité ⇒ **404, jamais 403**.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-kpi-performance-commerciaux.md` — contrat KPI : §3 l'attribution et ses
  mesures, §5 la visibilité (point de fuite), §9 l'impact front.
- `instructions/FEATURE-suppression-pieces-jointes.md` — contrat suppression : règles R1–R5, §6 la
  justification chiffrée du 409, §9 l'impact front.
- `src/main/resources/db/migration/006_AppFileObject_DeletedBy.sql` — **à rejouer sur `yesRemsProd`
  et `yesRemsDev` (VPS)**. Déjà appliquée sur `yesRemsCiCheck` ; colonne présente dans
  `ci/schema/010_tables.sql`.
- `src/main/java/.../domain/InteractionKpi.java` — les codes de type/résultat avec leurs volumes.
- `src/main/java/.../services/kpi/PerformanceCommercialService.java` — fusion des trois
  projections, médiane du délai, taux `null` plutôt que zéro.
- `src/main/java/.../entities/com/ComActionMessage.java` — `admUser` (l'auteur) et l'avertissement
  sur `cfgEmployee`.
- `src/main/java/.../services/piecejointe/PieceJointeSupport.java` — `exigerFichier` /
  `marquerSupprime` / `supprimer` / `nombreDeFichiers`, pièges Hibernate documentés.
- `src/main/java/.../services/dossier/ReglementPieceJointeService.java` — garde 409, ordre imposé
  des contrôles (404 avant 409).
- `src/test/java/.../controller/kpi/KpiPerformanceCommerciauxTest.java` — 15 tests ; le décisif est
  `appelSaisiParUnChef_estCompteChezLeChefEtPasChezLeCommercialAssigne`.

---

## 🔧 Environment & Config

- **Tests** : `522 tests, verts` (495 en début de session). Lancer avec
  `$env:SPRING_DATASOURCE_URL = "jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"`
  (+ `USERNAME=tsix`, `PASSWORD=securePassword123`) puis `.\mvnw.cmd clean test`.
  Ajouter `"-Dspring.jpa.show-sql=false"` — **guillemets obligatoires sous PowerShell**.
- **SQL Server local** sur `49687`, user `tsix` : `yesRemsProd` (schéma complet + données réelles),
  `yesRemsCiCheck` (porte `DeletedBy`), `yesRemsTest` (obsolète). Ajouter `-l 30` à `sqlcmd` en cas
  de timeout de login.
- **Compte de développement** : `t.admin` dans `yesRemsProd`, rôle `Administrateur`.
- **Non vérifié** : l'endpoint KPI n'a jamais été exécuté contre `yesRemsProd` — un lancement local
  écrirait dans le bucket MinIO de production (bug #13). La logique SQL est validée par les
  requêtes d'exploration et les 15 tests, pas par une confrontation de bout en bout.

---

## ⏭️ Next Step

> **Attendre la fusion de la PR KPI sur GitHub, puis `sync`.** Branche
> `feat/kpi-performance-commerciaux`, commit `27b3e6c` (10 fichiers) + `d49f7ca`.
> PR : https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Fkpi-performance-commerciaux?expand=1
>
> **Ne pas couper de nouvelle branche avant le merge**, et demander l'accord avant de le faire.

### Backlog
- [ ] **Jouer `006_AppFileObject_DeletedBy.sql` sur `yesRemsProd` et `yesRemsDev`** — bug #21, la
      feature suppression est mergée mais inopérante en production sans cette colonne.
- [ ] Rejouer `005_CfgCompany_IsActive.sql` sur **`yesRemsDev` (VPS)** — `yesRemsProd` est bon.
- [ ] Relayer au dépôt frontend (autre lane) : §9 des deux specs. Purement additif dans les deux
      cas. Côté KPI, les taux sont des **`BigDecimal` 0-1 ou `null`** — `null` s'affiche « — »,
      jamais « 0 % ».
- [ ] Déclarer le check **`Tests` comme requis** sur `main` — sans ça le workflow ne bloque rien.
- [ ] Garde-fou sur `S3_ENDPOINT` en local (bug #13).
- [ ] **Balayage hors ligne du bucket** : rien ne supprime encore les objets dont `DeletedAt` est
      renseigné. Sans conséquence fonctionnelle, mais le coût de stockage ne redescend jamais.
- [ ] Règlement annulé : filtrer `sumAmountByComDocumentId` sur le statut (bug #11).
- [ ] `exclureDuPrix` : le dériver de la catégorie à défaut, exposer l'écart au prix TTC (#12).
- [ ] Suite des DELETE manquants : règlement (aujourd'hui `CANCELED` seulement), échéance,
      interaction.
- [ ] Endpoint de désactivation d'un compte (`AdmUser.Disabled` lu partout, jamais écrit).
- [ ] `deploy.yml` ne joue toujours pas les tests : la barrière est sur la PR, pas sur `main`.
- [ ] `ContentTypeParExtension` ignore `.jfif`/`.heic` (4 fichiers réels).
- [ ] Sauvegarde du bucket MinIO — seule copie active des 8 Go.
- [ ] Révoquer le PAT GHCR et la clé de service MinIO passés en clair dans des conversations.
