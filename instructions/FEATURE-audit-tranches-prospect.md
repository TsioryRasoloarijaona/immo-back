# FEATURE — piste d'audit et tranches d'intérêt sur la fiche prospect

**Branche** : `feat/audit-tranches-prospect` (backend seul)
**Périmètre** : strictement **additif** — six champs de plus sur `GET /api/prospects/{id}`.
**Origine** : inventaire de `ComSaleProspectView` (97 colonnes) mené le 10/09/2026, reproduit en §1.

---

## 1. L'inventaire de `ComSaleProspectView`

La fiche prospect est le **3ᵉ écran du logiciel de bureau** (5 447 ouvertures sur 12 mois d'après
`AdmFormsLog`). Sa vue légataire porte 97 colonnes ; `ProspectDetailDto` en sert 58.

### Il n'y a pas une vue mais une famille

| Vue | Colonnes | Lignes | Rôle |
|---|---:|---:|---|
| `CfgTierView` | 134 | 14 516 | générique, **1:1 avec la table `CfgTier`** — aucun filtre |
| **`ComSaleProspectView`** | **97** | 13 664 | le prospect *de vente* — la référence de cette feature |
| `ComSaleCustomerView` | 101 | 332 | le client de vente |
| `RntProspectView` / `PrmProspectView` / `RomProspectView` | 97 / 99 / 99 | — | location, gestion, location-vente |

### Les 39 colonnes non servies

| Groupe | Nb | Verdict |
|---|---:|---|
| Doublons prouvés | 10 | sans objet |
| Mortes (0 ligne) ou quasi | 19 | ne jamais exposer |
| Peuplées mais **constantes** | 4 | aucune valeur informative |
| **Vrais manques** | **6** | l'objet de cette feature |

**Les 10 doublons** — les colonnes préfixées `Tier*`. Vérifié une par une :
`TierType`/`TierKind`/`TierNationality`/`TierProfession`/`TierSalary`/`TierCountry`/
`TierCountryResidence`/`TierBank` sont identiques à leur homologue sur les 13 664 lignes, et
`TierEmployee` est identique à `Commercial` — déjà servi comme `commercialNom`. `TierAttached`
est à 0 %.

**Les 4 constantes**, qui paraissent à 100 % et ne disent rien :

| Colonne(s) | Peuplée | Valeurs distinctes |
|---|---:|---|
| `StlCurrencyId`/`Code`/`Description` | 100 % | **1** — `MAD` partout |
| `AdmSocialFormId` + `SocialForm` | 100 % | **2** — « Personne physique » sur 13 661, vide sur 3 |

**Les 19 mortes** : `Bank`, `BankAgency`, `CfgBankId`, `Rib`, `Iban`, `FiscalRegistry`,
`FiscalDouane`, `Contact`, `CfgContactId`, `CodeCountable`, `Quality`, `Liability`,
`AdmLiabilityId`, `Asset`, `RsRatio`, `TierAttached` — **0 ligne**. Plus `IsProspect` (6 lignes),
`FiscalCode` (0,4 %) et `Age` (0,4 % de valeurs non nulles : colonne calculée qui vaut 0, même
piège que `Delai` côté prospection).

### Deux pièges rencontrés, à ne pas rejouer

**`Mobile1` n'est PAS un second numéro.** La vue le définit ainsi :

```sql
, LTRIM(RTRIM(ISNULL(CASE WHEN CfgTier.Mobile='' ... '(' + MobileIndicatif + ')' + ' ' + Mobile ...))) AS Mobile
, CfgTier.Mobile AS Mobile1
```

`Mobile` est la version **formatée** (`(212) 6 98 29 80 95`), `Mobile1` la colonne **brute** —
identique à `CfgTier.Mobile` sur les **13 664 lignes**. Nous servons déjà la brute sous `mobile` et
l'indicatif sous `indicatifMobile` : il n'y a rien à ajouter. `Mobile1` n'existe dans **aucune
table** du schéma, seulement dans les 8 vues prospect/client — le mapper aurait été exactement
l'erreur que le `CLAUDE.md` interdit.

**`ice` ne vient pas de `FiscalCode`** mais de la colonne `ICE` de `CfgTier`, absente de la vue.
`FiscalCode` (0,4 %) est bien non servie et n'en est pas un doublon.

### Et l'inverse : notre fiche est plus riche que la vue

`ProspectDetailDto` sert **cinq champs que `ComSaleProspectView` n'a pas** : `adresse2`,
`adresse3`, `codePostal`, `nombreEnfants` et `ice`, tous portés par `CfgTier`. L'écart n'est donc
pas à sens unique.

---

## 2. Les 6 champs ajoutés

### 2.1 Piste d'audit — 4 champs

Renseignés sur **~100 %** des 13 664 lignes, avec **55 auteurs distincts** : c'est une vraie piste,
pas un champ technique vide.

| Champ | Source | Peuplé |
|---|---|---:|
| `creePar` | `CfgTier.UserCreate` | 100 % |
| `creeLe` | `CfgTier.UserCreateDateTime` | 100 % |
| `modifiePar` | `CfgTier.UserUpdate` | 99,9 % |
| `modifieLe` | `CfgTier.UserUpdateDateTime` | 99,9 % |

**Aucune modification d'entité** : les quatre colonnes sont déjà mappées dans `CfgTier`. C'est une
pure addition de DTO — le coût de ce demi-lot est nul.

Nommage aligné sur `ProspectionListItemDto`, qui sert déjà exactement ces quatre champs sous ces
quatre noms. Ne pas inventer de variante.

### 2.2 Tranches d'intérêt — 2 champs

La vue les agrège en chaîne (`CfgTranches`, `CfgTrancheIds`), peuplées sur **98,9 %**, 44 valeurs
distinctes. **Nous servons une liste structurée**, pas la chaîne.

```json
"tranchesInteret": [
  { "id": "…", "code": "BELAIR", "description": "AYAD RESIDENCE BEL AIR" }
]
```

> **Pourquoi pas la chaîne de la vue.** Non pas parce qu'elle serait ambiguë — vérifié, **aucune
> des 16 descriptions de tranche ne contient de virgule**, donc le découpage marcherait
> aujourd'hui. Mais rien n'empêche qu'une future tranche en contienne, le séparateur n'étant
> garanti par aucune contrainte ; et un tableau JSON est de toute façon la forme juste pour une
> liste, avec les identifiants exploitables sans second appel.

**La règle d'agrégation, copiée de la définition de la vue** (`sys.sql_modules`) : union
**DISTINCT** de deux sources, sur toutes les prospections du prospect
(`ComProspection.CfgProspectId = CfgTier.PKey`) —

1. la **tranche principale** de chaque prospection (`ComProspection.StkHierarchyId`) ;
2. les **autres tranches** (`ComProspectionProject.StkHierarchyId`), c'est-à-dire les intérêts
   secondaires déjà exposés par `GET /api/prospections/{id}/autres-tranches`.

Cardinalité mesurée : **13 246 prospects ont 1 tranche, 254 en ont 2, 8 en ont 3.** La liste est
donc courte par nature — inutile de la paginer ou de la conditionner.

---

## 3. Implémentation

- `dto/prospect/TrancheInteretDto.java` — record `(UUID id, String code, String description)`.
- `ProspectDetailDto` — 4 champs d'audit + `List<TrancheInteretDto> tranchesInteret`, en fin de
  record pour ne pas déplacer les composants existants.
- `CfgTrancheRepository` — une requête, pas deux plus une fusion en Java :

  ```java
  @Query("""
      SELECT DISTINCT t FROM CfgTranche t
      WHERE t.id IN (SELECT p.stkHierarchy.id FROM ComProspection p WHERE p.cfgProspect.id = :prospectId)
         OR t.id IN (SELECT pp.stkHierarchy.id FROM ComProspectionProject pp
                     WHERE pp.comProspection.cfgProspect.id = :prospectId)
      ORDER BY t.description
      """)
  List<CfgTranche> tranchesInteret(@Param("prospectId") UUID prospectId);
  ```

  Deux sous-requêtes `IN` plutôt qu'un `UNION` : JPQL ne porte pas les opérations ensemblistes de
  façon portable, et le plan est équivalent sur une liste de cette taille.

- `ProspectService` — une requête de plus sur la fiche seulement. **La liste
  (`ProspectListItemDto`) n'est pas touchée** : elle n'a pas de sélection de colonnes, donc tout
  ajout y serait payé sur chaque ligne de chaque page.

---

## 4. Tests

- fiche d'un prospect → les 4 champs d'audit servis ;
- prospect ayant une prospection avec tranche principale → la tranche apparaît dans
  `tranchesInteret` ;
- prospect ayant une « autre tranche » (`ComProspectionProject`) → elle apparaît aussi ;
- **la même tranche en principale et en secondaire n'apparaît qu'une fois** — c'est le `DISTINCT`
  de la vue, et c'est le cas le plus facile à casser ;
- prospect sans prospection → **liste vide, jamais `null`** ;
- la liste `GET /api/prospects` reste inchangée.

---

## 5. Impact front

**Aucune rupture.** Six champs de plus sur `GET /api/prospects/{id}`, aucun retrait, aucun
changement de type. `GET /api/prospects` est inchangée.

Dans `crm-prototype/src/types.ts`, `ProspectDetailDto` gagne quatre champs et un tableau ; un
nouveau type `TrancheInteretDto` est à ajouter. `tranchesInteret` est **toujours présent** — vide
plutôt qu'absent quand le prospect n'a aucune prospection.
