# Session Memory
_Last updated: 2026-09-14T08:10+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Aligner nos endpoints sur ce que le logiciel de bureau (OMRAN Estate) fait réellement**, en
partant de sa **trace d'audit SQL Server** plutôt que de captures d'écran ou de lectures de vue.

| Lot | Sujet | État |
|---|---|---|
| G | Groupement et agrégats sur les listes | **mergé** (PR #43) |
| H | Groupement sur *toutes* les colonnes + groupe vide écarté | **mergé** (PR #44) |
| I | Colonnes du dossier vues dans la trace | **mergé** (PR #45) |
| J | Conformité au légataire : écritures manquantes | **mergé** (PR #46) |
| K | Encaissements : `IsCredit=1` + filtre par tranche | **mergé** (PR #47) |
| **L** | **Encaissements : sélecteur de colonnes + groupement** | **poussé, PR en attente** |

**833 tests verts** (815 en début de session, +18).

---

## ✅ What Worked

- **La trace d'audit est vivante et interrogeable à la demande** — elle couvre désormais
  jusqu'au 14/09, 4 281 lignes. Requête, depuis `master`, compte `tsix` :
  ```sql
  SELECT event_time, action_id, succeeded, server_principal_name,
         database_name, schema_name, object_name, statement
  FROM sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)
  ORDER BY event_time DESC;
  ```
- **Savoir ce que la trace NE peut pas dire.** L'écran encaissements fait
  `SELECT StlSettlementView.*` — 107 colonnes, aucune nommée — et le légataire **ne stocke pas la
  disposition de sa grille en base** (aucune table `*Layout*`/`*Grid*`/`*Column*` ; `AdmFormsLog`
  ne journalise que l'ouverture d'écran). La trace prouve la *séquence* et le *filtre*, jamais
  l'affichage. Le relais est alors la mesure.
- **Mesurer distinctes + non-zéro, jamais `COUNT(colonne)`.** Sur les encaissements ça a évité
  deux erreurs symétriques en une passe : croire `Group1Label/2/3` utiles (0/643) et croire
  `Category`/`ItemNumber`/`ItemDescription` pleines (643/643 mais **1 seule valeur distincte** —
  des placeholders `None` / `Non définie`).
- **Vérifier le type SQL avant de déclarer une colonne groupable.** `Notes` est `nvarchar(max)` :
  le `COUNT(DISTINCT)` de `compterGroupes` l'aurait refusé en 500. Trouvé avant d'écrire, pas après.
- **Le moteur de groupement générique tient ses promesses** : le lot L n'a ajouté **aucune ligne**
  à `GroupementService`, seulement un catalogue porteur de `Chemin`.
- **Sonder la cardinalité avant d'offrir un axe** : type=10, entité=5, banque=17, auteur=17,
  tranche=11 — tous bons ; tiers=309 sur 643 — offert quand même, c'est la règle du lot H.

---

## ❌ What Failed

- **`COUNT(colonne)` pour juger une colonne utile** → compte les non-nuls. Cinq occurrences
  cumulées : `PenalityDescription`, `FinalTitleNumber`, `MapLatitude`, puis `Category` /
  `ItemNumber` / `ItemDescription` côté encaissements.
- **Compter les colonnes d'une vue pour chiffrer un écart** → surestime massivement (`H1/H2/H3`).
- **`cb.coalesce(cb.sum(x), BigDecimal.ZERO)`** → arrondit la somme d'argent à l'unité en silence.
  Substituer le `NULL` en Java. Test de non-régression rejoué sur le catalogue encaissements.
- **`@Modifying(clearAutomatically = true)`** → détache l'entité que l'appelant vient de créer.
- **`GROUP BY` sur une expression contenant une sous-requête** → SQL Server refuse.
- **Piper `sqlcmd` dans `head`** → tronque silencieusement la sortie et fait croire qu'une section
  de requête n'a rien rendu. Rediriger vers un fichier, puis lire.
- **`sqlcmd -W` et `-y` sont mutuellement exclusifs** — l'un des deux, jamais les deux.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1 | 54 tests en erreur, `Nom d'objet/de colonne non valide` | **Fixed** | Dérive de `yesRemsTest`, base réalignée depuis `yesRemsProd` |
| 2 | Somme d'argent arrondie à l'unité (190,75 → 191,0) | **Fixed** | `COALESCE` SQL retiré, substitution en Java |
| 3 | `PerimetreInterceptorInvarianteTest` — routes `/groupes` non déclarées | **Fixed** | Ajoutées à `ROUTES_SANS_ID_AUTORISEES` avec justification (dont `/api/encaissements/groupes` au lot L) |
| 4 | `@Formula` rendant `null` dans les tests | **Fixed** | `flush` + `clear` avant l'appel HTTP |
| 5 | `GROUP BY` avec sous-requête → 500 | **Fixed** | Colonne déclarée `agregeable` seulement → 400 lisible |
| 6 | `clearAutomatically` détachant le dossier créé | **Fixed** | `flushAutomatically` |
| 7 | `TransientPropertyValueException` dans les tests | **Fixed** | Réutiliser la valeur de retour de `save()` |
| 8 | Nos interactions exclues du « dernier échange » | **Fixed** | `IsSystem` non posé ; script `007_…sql` |
| 9 | Prospection restée active après la vente | **Fixed** | `IsClosed`/`ClosingDate` dans `DossierService.create` |
| 10 | Dossiers absents de `StrHistory` | **Fixed** | Table lue mais jamais écrite |
| 11 | Décaissement de −470 000 DH faussant les totaux | **Fixed** | `IsCredit = 1` (lot K), étendu au groupement au lot L |

---

## 🔑 Key Points to Remember

### ⚠️ QUATRE bases locales, qui ne disent pas la même chose

| Base | Tables | Rôle |
|---|---:|---|
| `yesRems` | — | **celle qu'Omran utilise** — c'est elle que l'audit trace |
| `yesRemsProd` | 462 | restauration légataire, **celle que `CLAUDE.md` prescrit pour mesurer** |
| `yesRemsCiCheck` | 167 | bâtie depuis `ci/schema/*.sql` |
| `yesRemsTest` | 86+ | **le défaut** de `src/test/resources/application.properties` |

`yesRems` ≠ `yesRemsProd` : 387 dossiers contre 358. Vérifier `database_name` dans l'audit avant
de rapporter une mesure à une trace. **`StlSettlementView` n'existe pas dans `yesRemsTest`** (0
colonne) — raison de plus pour bâtir les catalogues sur les entités, jamais sur la vue.

### L'audit SQL et ses limites

- **Vivant** : une requête lancée maintenant y apparaît en quelques secondes. L'utilisateur peut
  rejouer un écran à la demande pour en produire la trace.
- **Tronque à 4 000 caractères**, **ne montre pas les valeurs** (`@P1…@Pn`), **décalage d'une
  heure** (UTC vs local). Les requêtes sur `sys.all_objects` sont l'IDE, pas Omran.
- **Un `SELECT vue.*` ne dit rien de l'affichage.** C'est le cas de l'écran encaissements.

### La séquence transformation prospection → vente

1. `UPDATE ComProspection` ✅ · 2. `INSERT AdmAttachedFile` ❌ · 3. `AdmSequence` ✅ ·
4. `INSERT ComFolder` ✅ · 5. `ComDocument` ×3 ✅ · 6. **`ComActionMessage` ×4 + `AdmAlertData` ×4
— alertes de jalon ❌ (reste, bloqué sur décision produit)** · 7. `StrHistory` ✅ ·
8. désarmement des relances ✅ · 9. `StkItem.comFolderId` ✅ · 10. `StrFolderStatusHistory` ✅ ·
11. `UPDATE CfgTier` — re-normalisation, **pas** une transition.

### `UPDATE CfgTier` : le statut client est DÉRIVÉ, jamais stocké

Ne jamais écrire un « type client » à la création — ce serait inventer un état que le légataire ne
stocke pas.

### Deux couches de permissions chez Omran, une seule chez nous

En plus du périmètre par société, Omran filtre **par statut visible** via `StrStatusIntervener` ×
`StrIntervener`. **Non implémenté.**

### Les catalogues `Colonne*` pilotent DEUX features

`ColonneDossier` / `ColonneProspection` / `ColonneProspect` / **`ColonneEncaissement`** alimentent
le sélecteur de colonnes **ET** le groupement. Toute entrée ajoutée doit avoir son champ dans le
DTO, sinon le sélecteur annonce une colonne absente. Une colonne avec un `Chemin` devient groupable
**sans une ligne dans le moteur**.

### La règle qui décide de ce qui est groupable

Un chemin qui traverse une association **to-many** n'est jamais groupable : il compte plusieurs
fois les lignes multi-rattachées et brise l'invariant « somme des effectifs = `totalElements` ».
C'est ce qui exclut `dossierId`/`dossierCode` des encaissements (654 couples pour 643 règlements,
8 multi-dossiers). Les colonnes **calculées en Java** (`montantBrutRestant`, `montantNet`) sont
exclues pour une autre raison : elles n'existent pas en base.

---

## 📁 Important Files & Paths

- **`instructions/FEATURE-colonnes-groupement-encaissements.md`** — le lot en cours (L). §1 ce que
  la trace ne peut pas dire, §3 les colonnes ajoutées avec leur mesure, §4 les écartées et
  pourquoi, §5.2 le refus structurel du groupement par dossier.
- `instructions/FEATURE-encaissements-conformite-legataire.md` — lot K. §4 la décision produit
  ouverte sur les encaissements non affectés.
- `instructions/FEATURE-conformite-legataire.md` — lot J. §5 le reste (alertes de jalon).
- `instructions/FEATURE-groupement-listes.md` — lot G. ⚠️ **§2 et §4.1 périmés**, bandeau en tête.
- `src/main/java/.../domain/ColonneEncaissement.java` — catalogue du lot L, chaque entrée porte sa
  mesure en javadoc.
- `src/main/java/.../domain/Chemin.java` — le chemin JPA des colonnes de catalogue.
- `src/main/java/.../services/groupement/GroupementService.java` — moteur générique des **quatre**
  listes désormais.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la
  main**, rattrape 14 interactions écrites sans drapeaux.

---

## 🔧 Environment & Config

- **Lire l'audit d'Omran** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)`
  depuis `master`, compte `tsix`. Scopé sur la base `yesRems`.
- **Mesurer** : `sqlcmd -S localhost,49687 -U tsix -P securePassword123 -d yesRemsProd -C`.
  Rediriger vers un fichier plutôt que de piper dans `head`. `-W` et `-y` sont exclusifs.
- **Tests** : `./mvnw.cmd test` fonctionne nu. Avant de suspecter le code sur une colonne absente,
  **comparer les bases** (`INFORMATION_SCHEMA.COLUMNS`).
- **Mesurer une requête** : boucler sur **une seule** connexion, le cache est froid aux deux
  premières passes. **Toujours mesurer trois fois.**
- ⚠️ **Ne jamais réécrire un fichier source via PowerShell** (encodage cassé plusieurs fois).
- ⚠️ Identifiants de service en clair au fil des sessions — dette connue, non traitée.

### État des branches

**Branche en cours : `feat/colonnes-groupement-encaissements`, 1 commit, poussée, PR en attente.**
Le front (`crm-prototype`) n'est pas touché — **tous ces lots sont backend seul**.

| Repo | Branche | État |
|---|---|---|
| back | **`feat/colonnes-groupement-encaissements`** | lot L, 833 tests verts — **en attente de merge** |
| back | `feat/groupement-listes`, `feat/groupement-toutes-colonnes`, `feat/colonnes-dossier-ecran-legataire`, `feat/conformite-legataire`, `feat/encaissements-conformite-legataire` | **mergées — supprimables** (`git branch -d`, jamais `-D`) |
| back | `fix/ci-pollution-doubles-envoi` | PR #38 mergée mais la locale a divergé |
| back | `docs/memoire-de-session`, `feat/remise-dossier` | non mergées |

---

## ⏭️ Next Step

> **Attendre la merge de la PR `feat/colonnes-groupement-encaissements`**, puis `sync`.
>
> **Puis demander à l'utilisateur sa politique d'alertes de jalon** — c'est ce qui bloque l'étape 4
> du lot J, et c'est une décision **produit, pas technique** : quels jalons déclenchent une alerte,
> à quelle échéance, avec quel libellé. La trace donne la forme (« l'état RESERVATION du dossier… »
> à +7 j, « FRAIS SYNDIC (16 800 DH) arrivé à échéance »), pas la règle.

### Backlog

- [ ] **Alertes de jalon du dossier** — 4 `ComActionMessage` + 4 `AdmAlertData` à la vente.
      Bloqué sur la décision produit ci-dessus.
- [ ] **Encaissements non affectés** — 16 pièces, 6 435 400 DH, invisibles car sans ligne.
      Décision produit ouverte (`FEATURE-encaissements-conformite-legataire.md` §4).
- [ ] **Seconde couche de permissions** — filtrage par statut visible
      (`StrStatusIntervener` × `StrIntervener`). Le plus structurant des restes.
- [ ] **Diff sémantique de `ComSaleProspectionView`** — 148 colonnes contre nos 56, en résolvant
      chaque colonne vers son origine réelle, **jamais par comptage brut**.
- [ ] **Verrous pessimistes** `AdmWebLockEntities` + `AdmWebSessions`.
- [ ] **Journalisation d'usage** `AdmWebHistorics` / `AdmFormsLog`.
- [ ] **Onglets « Dates » / « Autres montants » du dossier** — attendent leurs captures.
- [ ] **Décision produit : filtrer les doublons de pré-prospection ?** 798 sur 3 818 (20,9 %).
- [ ] **Fiche tiers** — `ProspectDetailDto` 52 champs contre 97 dans `ComSaleProspectView`.
- [ ] Supprimer les branches mergées.
