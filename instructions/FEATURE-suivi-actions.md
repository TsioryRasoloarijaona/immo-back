# FEATURE — Suivi des actions (écran OMRAN « Suivi des actions »)

_Branche : `feat/suivi-actions` — 23/09/2026_

Reproduire l'écran OMRAN de suivi des actions commerciales — les appels, visites, messages
saisis par chaque commercial, jour par jour — avec **les mêmes colonnes**, un **sélecteur de
colonnes** et le **groupement** par colonne, comme les autres listes (`?colonnes=`, `/groupes`).

Doc officielle : <https://help.omranestate.com/SUIVIDESACTIONS.html>.

---

## 1. Ce que fait OMRAN — trace d'audit

Écran rejoué le 23/09 (15:24 → 15:28 UTC, session 67), et le 22/09 18:26 avec un utilisateur.

| Étape | Requête |
|---|---|
| filtre **Utilisateur** | `SELECT * FROM AdmUser ORDER BY Description` |
| filtre **Action** | `SELECT * FROM ComActionMessageType ORDER BY Description` |
| filtre **Catégorie** | `SELECT * FROM ComActionMessageCategory ORDER BY Sorting` |
| filtre **Tranche** | `CfgTrancheView` à vendre (`IsForSale = 1`) et habilitées pour l'utilisateur |
| **la liste** | `SELECT * FROM ComActionMessageView WHERE CfgTrancheId IN (…) AND DocDate >= @P1 AND DocDate < @P2 [AND UserName = @P3]` |

- Période **fin exclue** : un jour = `[J, J+1[`.
- `UserName = @P3` n'apparaît que si un utilisateur est choisi.
- Action et catégorie ne sont **pas** dans le SQL : filtrées dans la grille.
- Aucun filtre `IsSystem` : les actions système sont listées.
- Le « par jour » n'est pas une requête : la grille regroupe la liste.

### La vue `ComActionMessageView`

Une ligne par `ComActionMessage` (48 213 = 48 213, **aucun doublon** malgré le `DISTINCT`). Colonnes
calculées reprises ici :

| Colonne vue | Règle |
|---|---|
| `CfgTrancheId` | catégorie **Prospection** (`69F8E7D6-…`) → tranche de la **prospection** ; sinon tranche de l'**article** |
| `ActionRank` | 1 + nombre d'actions **plus récentes** du même tiers et du même type, non système, non impayées |
| `AlertActive` | `AdmAlertData.Active` de l'action (0 si absente) |
| `FolderStatusDescription` (État) | dossier → statut du dossier ; prospection → statut du dossier né de la prospection, sinon la catégorie ; sinon la catégorie |
| `Personnel` | employé du compte → description du compte → commercial |

### Pourquoi pas la vue elle-même

Mesuré sur `yesRemsProd` : **6,6 s** pour une page de 50 sur un mois, **9,3 s** pour un comptage
sans filtre — son `DISTINCT` et son rang corrélé sont calculés sur toutes les lignes. Les mêmes
jointures écrites sur `ComActionMessage` : **23 ms** le comptage, **139 ms** la page, **324 ms**
le groupement tranche × type, **337 ms** le rang des 50 lignes d'une page. Et la vue n'existe pas
dans `yesRemsTest`.

⟹ liste construite sur l'entité `ComActionMessage` ; le rang, l'alerte active, l'état et le
personnel sont calculés **pour la page servie seulement** (ni groupables ni triables).

La tranche résolue est une `@JoinFormula` sur l'entité (`trancheResolue`) : la sous-requête est
dans la condition de jointure, pas dans le `GROUP BY` — SQL Server refuse le second (erreur 144).

**Parité vérifiée** sur `yesRemsProd`, comptage utilisateur × jour × tranche, vue OMRAN contre
notre règle : **5 623 groupes et 48 213 actions des deux côtés, 0 écart.**

---

## 2. Mesures — 48 213 actions (`yesRemsProd`)

| Colonne | Renseignée | Décision |
|---|---:|---|
| Utilisateur (`UserName`) | 48 186 — 51 valeurs | au catalogue, groupable |
| Action (type) | 48 204 | groupable |
| Catégorie | 48 205 | groupable |
| Résultat (sous-catégorie) | 44 650 | groupable |
| Tiers | 46 678 | groupable |
| Prospection | 46 601 | groupable |
| Tranche résolue | 46 559 — 14 tranches | groupable, **filtre** |
| Entité, Programme | 46 559 | groupables |
| Commercial (`CfgEmployeeId`) | 37 219 (77 %) | groupable |
| Date alerte | 39 363 | date |
| Message (`AlertMessage`) | 46 613 | **non groupable** : `nvarchar(max)` |
| Impayé | 930 à vrai | groupable |
| Système | 4 482 à vrai | groupable |
| Dossier | 1 507 | groupable |
| Article | 4 170 | groupable |
| Désistement, transfert client, transfert produit | **0** | **écartés** |
| SAV | 2 | écarté |
| Location / offre / syndic | 0 | écartés |

Sur 12 mois : téléphone sortant **5 258**, visites 329, téléphone entrant 90 — l'écran sert d'abord
à compter les appels.

---

## 3. Contrat

### `GET /api/suivi-actions`

Filtres (tous optionnels, combinés en ET) :

| Paramètre | Règle |
|---|---|
| `dateDebut`, `dateFin` | `DocDate >= dateDebut AND DocDate < dateFin` — **fin exclue**, comme OMRAN |
| `trancheId` (répétable) | tranche **résolue** (règle de la vue) |
| `utilisateur` (répétable) | `UserName`, égalité exacte |
| `typeId`, `categorieId` (répétables) | en SQL ici — OMRAN les applique dans sa grille |
| `systeme` | `true`/`false` ; absent = tout (comme OMRAN) |
| `groupeColonne`, `groupeValeur`, `granularite` | dépliage d'un groupe, comme partout |
| `colonnes` | sélection de colonnes, catalogue ci-dessous |

Tri par défaut : `docDate` décroissant. Pagination standard.

### `GET /api/suivi-actions/groupes`

Mêmes filtres, `groupBy` requis, `agregats`, `granularite`, `tri`, `inclureSansValeur` — même
moteur que les autres listes. **Appels exacts par jour d'un commercial** :
`?utilisateur=Khadija ELKORRI&typeId=<Téléphone Sortant>&groupBy=dateAction&granularite=JOUR`.

### `GET /api/referentiels/colonnes/suivi-actions`

Le catalogue, forme `ColonneDto` (nom, libellé, groupe, type, parDefaut, groupable, agregeable,
fonctions).

### Périmètre commercial

`/api/suivi-actions` n'est pas une collection de `PerimetreInterceptor` : le service applique
`PerimetreCommercialService#filtreInteractions` — le prédicat de la hotline et des relances
(projet par la prospection, puis par le dossier ; entités actives), aux listes comme aux groupes.

---

## 4. Catalogue (`ColonneSuiviAction`)

Par défaut = les colonnes de la doc OMRAN qui portent de l'information. Hors défaut = identifiants
et audit.

| Groupe | Colonnes |
|---|---|
| ACTION | `utilisateur`, `typeAction`, `rang`*, `dateAction`, `dateAlerte`, `alerteActive`*, `categorie`, `resultat`, `message`, `entrante`, `systeme`, `impaye` |
| ACTEURS | `commercial`, `personnel`*, `tiersNom` |
| RATTACHEMENT | `prospectionCode`, `dossierCode`, `etat`*, `articleCode`, `articleLibelle`, `trancheCode`, `trancheLibelle`, `programme`, `entite` |
| IDENTIFIANTS | `id`, `typeId`, `categorieId`, `tiersId`, `prospectionId`, `dossierId`, `trancheId` |
| AUDIT | `creePar`, `creeLe`, `modifiePar`, `modifieLe` |

\* calculées pour la page servie : ni groupables, ni agrégeables.

---

## 5. Hors périmètre

- Désistement, transferts, SAV, location, offre, syndic : 0 ou 2 lignes (§2).
- Le filtre tranche **à vendre + habilitée** d'OMRAN : notre périmètre commercial le remplace.
- Écriture : l'écran est une consultation (double-clic → fiche : le front a les ids).

---

## 6. Correctif — actions sans auteur

**Constat** (§ vérification du 23/09) : 13 actions du 18 au 22/09 sans `UserName`, sans
`AdmUserId`, libellé sans nom — invisibles sous tout commercial dans ce même écran OMRAN.

**Cause** : en deux bases, l'auteur était cherché dans la base d'**écriture** (source), alors que
les comptes de connexion du banc (`t.admin`, `Khadija`) n'existent que dans la **copie**. Non
trouvé ⟹ tout nul.

**Correctif** (`UtilisateurAffiche.courant()`, utilisé par `ProspectionService` et
`DossierService`) : compte dans la base d'écriture, sinon **description lue sur la copie**
(même libellé qu'OMRAN, donc regroupé avec ses actions), sinon identifiant du token. `AdmUserId`
n'est posé que si le compte existe dans la base d'écriture (clé étrangère). Un avertissement est
journalisé à chaque occurrence.

**Risque prod** : tout compte créé sur la base du VPS et absent du 115 produira des actions sans
`AdmUserId` après le passage en deux bases — le correctif garde le nom, mais synchroniser les
comptes vers la source reste nécessaire.

---

## Impact front

- Nouvel écran « Suivi des actions » : `GET /api/suivi-actions` (+ `/groupes`), catalogue
  `GET /api/referentiels/colonnes/suivi-actions`. Aucun endpoint existant modifié.
- Liste des utilisateurs du filtre : `GET /api/suivi-actions/groupes?groupBy=utilisateur`
  (les valeurs réellement présentes, avec leur effectif).
