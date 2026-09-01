# FEATURE — Règlement : statut « pointé », preuve de paiement, saisie complète

Branche : `feat/reglement-statut-preuve` (backend uniquement).
Aucune modification de schéma. Toutes les colonnes utilisées existent déjà dans `yesRems`.

---

## 1. Pourquoi

Un règlement créé par le CRM est aujourd'hui incomplet et sans état :

| Constat | Preuve |
|---|---|
| `StlSettlement.Status` n'est **jamais écrit** par l'API | `grep setStatus src/main` → 0 occurrence. Les 3 218 lignes legacy portent `IN_WORK`, les nôtres `NULL`. |
| Aucun contrôle du montant (ni `> 0`, ni contre le reste à payer) | `EcheancierService.addReglement` ne lit jamais le montant de l'échéance. Un sur-paiement donne `reste` négatif + statut calculé `PAYE` (`buildDto:250-283`). |
| `banqueObligatoire` annoncé au front mais **non appliqué** | `EcheancierService:85-88`, commentaire explicite. Seul `referenceObligatoire` est enforcé (`:89-92`). |
| `typeId` et `payeurId` optionnels | « Mode de versement » et « Versement effectué par » sortent **vides** sur l'autorisation d'encaissement. |

## 2. Les deux invariants mesurés (3 244 lignes réelles, zéro contre-exemple)

**Invariant 1 — `POINTED` ⟺ `SettlementDate` renseignée.**
Les 14 `POINTED` ont tous une `SettlementDate` ; aucun des 3 218 `IN_WORK` n'en a.
Statut et date sont deux encodages du même fait et n'ont **jamais** divergé.
L'API ne doit pas écrire la première incohérence de l'historique de cette base.

**Invariant 2 — « pointé ⇒ pièce jointe » est une règle métier NOUVELLE.**
Ce n'est pas un comportement legacy restauré, et les données ne peuvent pas la valider :

- table de contingence globale : `IN_WORK` 450 avec PJ / 2 768 sans ; `POINTED` **2 avec / 12 sans**.
- mais les deux pratiques ne se recouvrent presque pas dans le temps : la 1ʳᵉ PJ de règlement date
  du **18/01/2022**, alors que le pointage s'arrête en 2022-2023 (1 en 2020, 11 en 2021, 1 en 2022,
  1 en 2023, puis plus rien ; dernière modification d'un `POINTED` : 31/03/2023).
- sur la fenêtre de coexistence (règlements créés à partir du 18/01/2022) : **3 `POINTED`, dont
  2 avec PJ et 1 sans**. n=3 ne prouve rien.
- à l'inverse, **451 règlements récents portent une PJ sans être pointés** : les utilisateurs
  joignent déjà la preuve, ils ne pointent simplement plus.

**Conséquence normative :** la règle est un choix produit assumé, appliqué **au moment de la
transition uniquement**. Jamais en lecture, jamais rétroactivement — les 12 `POINTED` legacy sans PJ
doivent rester lisibles, et l'application desktop continue d'écrire dans `yesRems` sans rien
contrôler.

## 3. Statut — `domain/StatutReglement`

Enum pur (sans JPA), sur le modèle de `domain/StatutDossier` + `domain/TransitionsValidees`.

- Valeurs : `IN_WORK`, `POINTED`, `CANCELED` — les 3 seules présentes en données réelles.
  `NULL` (9 lignes legacy) est toléré **en lecture**, jamais écrit.
  `UNPAID`/`LOST` sont documentés comme filtres legacy sans aucune ligne réelle : **ne pas les
  introduire**.
- Transitions autorisées : `IN_WORK → POINTED`, `IN_WORK → CANCELED`. Rien d'autre.
- Toute transition hors table ⇒ **409** `ProblemDetail` avec `transitionsAutorisees`, via
  `InvalidStatutTransitionException` (déjà câblée dans `web/GlobalExceptionHandler:52-57`).
- Lecture tolérante : une valeur inconnue ou nulle en base ne doit **pas** faire échouer
  `GET /api/encaissements`, qui lit `status` en texte libre (`EncaissementService:186`).

## 4. Création — `POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements`

### 4.1 Champs

| Champ | Aujourd'hui | Cible |
|---|---|---|
| `montant` | non-null seulement | **requis**, **> 0**, et **≤ reste à payer de l'échéance** |
| `dateEcheance` | défaut silencieux `now()` | **requis** |
| `typeId` | optionnel | **requis** (alimente « Mode de versement ») |
| `reference` | requis si `referenceObligatoire` | **inchangé** — reste piloté par le flag du type |
| `banqueId` | annoncé, non appliqué | **requis si `type.banqueObligatoire`** (on applique enfin le flag) |
| `payeurId` | optionnel | **requis** (alimente « Versement effectué par ») |
| `dateReglement` | optionnel | **refusé à la création** (400) — cf. invariant 1 |
| `notes` | optionnel | inchangé |
| `statut` | inexistant | **non accepté** : toute création est `IN_WORK` (cf. §6) |

`reference` garde volontairement la règle existante : requise pour CHÈQUE / CHEQUE NOTAIRE /
SYNDIC / BANQUE / CERTIFIÉ / EFFET, libre pour ESPÈCES — c'est le paramétrage legacy de
`StlSettlementType`, déjà exposé au front via `TypeReglementDto.referenceObligatoire`.

### 4.2 Colonnes désormais écrites (et qui ne l'étaient pas)

- `Status = "IN_WORK"` — aligne les lignes API sur les 3 218 lignes legacy.
- `IsCredit = true` — encaissement client. Détermine aussi la `Category` des PJ (§5).
- `UserCreate` / `UserCreateDateTime` — corrige au passage le tri de `DossierHistoriqueService`,
  qui relègue aujourd'hui les règlements créés par l'API en bas de l'historique (`UserCreateDateTime`
  nul trié en dernier).

### 4.3 Reste à payer

`reste = ComDocument.Amount − Σ StlSettlementLine.Amount` de l'échéance (même calcul que
`EcheancierService.buildDto:250-283`, à factoriser plutôt qu'à recopier).
`montant > reste` ⇒ **400**, message indiquant le reste disponible.

### 4.4 Validation

Style manuel du dépôt : `InvalidRequestException` → 400 via `GlobalExceptionHandler:27-30`.
**Ne pas** introduire `@Valid` : il n'existe aucun handler `MethodArgumentNotValidException`, les
violations sortiraient donc hors du format `ProblemDetail` du projet.

## 5. Pièces jointes du règlement

Stockage déjà en place et **polymorphe** : `AdmAttachedFile.CategoryId` = PKey du porteur, sans FK
déclarée. 452 règlements réels portent déjà 460 fichiers (PDF 332, images 128, ~1,5 Mo en moyenne,
max 12 Mo). Rien à créer côté base.

```
GET  /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes
POST /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes              (multipart)
GET  /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes/{pieceId}/fichier
```

- `CategoryId` = `StlSettlement.PKey`.
- `Category` = **`"Yes.Stl.Credit.Settlement"`** : la valeur est entièrement déterminée par
  `IsCredit` en données réelles — 368 fichiers `IsCredit=1` portent cette valeur, 92 `IsCredit=0`
  portent `Yes.Stl.Debit.Settlement`, **zéro croisement**.
- `AdmAttachedFileTypeId` reste **null** : les 460 PJ de règlement réelles n'en portent aucune.
  Ne pas construire de sélecteur de type sur une preuve inexistante.
- Le règlement doit appartenir au dossier (jointure `StlSettlementLine`) sinon **404** — même
  garde que `AutorisationVersementService:66-68`.

### 5.1 Factorisation obligatoire

`PieceJointeService` (dossier) et `ArticlePieceJointeService` dupliquent déjà `toDto` **à
l'identique**, `findFichier` à la chaîne d'erreur près, et deux `@Query` JPQL identiques au nom de
paramètre près. Ajouter un 3ᵉ porteur par copie ferait **quatre** copies.

- extraire `AdmAttachedFileSummaryRepository.findByCategoryId(UUID ownerId)` générique et faire
  converger `findByDossierId` / `findByArticleId` dessus ;
- extraire un service/mapper commun portant `toDto` + `findFichier`, dans la lignée de
  `web/ContentTypeParExtension` (extrait précisément pour dédupliquer entre contrôleurs).

Réutiliser tel quel : `AdmAttachedFileRepository.findByIdAndCategoryId` (déjà générique malgré son
paramètre nommé `dossierId`), `ContentTypeParExtension`, et le mapping `@JdbcTypeCode(VARBINARY)`
de `AdmAttachedFile` — **ne pas y toucher** : le commentaire du code rappelle que `@Lob` fait
renvoyer du contenu vide par le driver SQL Server.

Garde-fous repris de `PieceJointeService.create:105-146` : fichier vide ⇒ 400, extension en
minuscules avec le point, légende = description sinon nom de fichier, `UserCreate` = nom affiché du
principal. Limite de taille : celle de Spring (`max-file-size=50MB`), inchangée.

## 6. Transition — `PATCH /api/dossiers/{id}/reglements/{reglementId}/statut`

```json
{ "statut": "POINTED", "dateReglement": "2026-09-01T10:00:00" }
```

Règles appliquées **dans le service**, jamais seulement documentées :

1. transition hors table ⇒ **409** avec `transitionsAutorisees` ;
2. `→ POINTED` sans **aucune** pièce jointe sur le règlement ⇒ **409**, message explicite
   (« une preuve de paiement est requise ») ;
3. `→ POINTED` exige `dateReglement` dans le body ⇒ **400** sinon ; écrit `SettlementDate`
   (invariant 1) ;
4. `→ CANCELED` laisse `SettlementDate` à `null` ;
5. la règle 2 ne vaut **qu'à la transition** : aucune vérification en lecture, aucun blocage sur la
   suppression d'une PJ, aucune rétroactivité sur les 12 `POINTED` legacy sans PJ.

**Le POST n'accepte pas `POINTED`.** Aucune PJ ne peut exister avant que le règlement existe : le
parcours « je saisis directement en pointé » est un enchaînement **côté front** de trois appels
(créer en `IN_WORK` → joindre la preuve → pointer). Le formulaire peut le présenter comme une seule
action ; le backend, lui, garde les trois étapes vérifiables.

`ReglementDto` expose désormais `statut` (et conserve `dateReglement`).

## 7. Autorisation d'encaissement

**Générée à la volée, jamais stockée.** L'endpoint existe déjà
(`GET /api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement`,
`DossierController:639-676`) ; le rendu est déjà conforme au modèle de référence
`instructions/templates/autorisation_virement/Report.pdf` (Opération, N° du bien, Prix total TTC,
Mode de versement, Montant en chiffres **et en lettres**, Émis le, clients + CIN, conditions
générales 48 h / Casablanca, et tout le « Cadre réservé à la banque »).

L'« automatique » se réduit donc à : **la réponse 201 de la création doit porter de quoi ouvrir le
document** (l'id du règlement suffit, il est déjà là) ; le front l'ouvre dans la foulée.

Deux champs sortaient vides faute de données à la saisie, et sont réglés par le §4 :
**Mode de versement** (`typeId` requis) et **Versement effectué par** (`payeurId` requis).

**`RIB` reste volontairement vide** : il vient de `StlSettlement.StlAccountId`, que l'API ne
renseigne pas — et il est **également vide sur l'exemplaire de référence**. Ne pas ajouter de champ
`compteId`.

⚠️ `AutorisationVersementTest:275` assère **exactement 1 page** (« Must fit on a single page, same
as the legacy sample »). Remplir les deux champs ci-dessus ne doit pas faire déborder en page 2.

## 8. OpenAPI (règle 10 de `CLAUDE.md`)

Chaque endpoint nouveau ou modifié : `@Operation` (résumé + règles métier appliquées),
`@ApiResponses` pour 400/404/409 avec leur déclencheur, `@Schema` sur chaque composant de record.
**En français.** `/api-docs` est la seule source de vérité du front.

## 9. Tests attendus

- `POST` : happy path 201 ; 400 par champ manquant ; 400 montant ≤ 0 ; 400 montant > reste à payer ;
  400 si `banqueObligatoire` et banque absente ; 400 si `dateReglement` fournie ; 404 dossier et
  échéance inconnus ; `Status` vaut bien `IN_WORK` après création.
- `PATCH` : **409 sans PJ** ; 200 avec PJ ; `SettlementDate` écrite ; 400 si `dateReglement`
  absente ; **409 sur transition invalide** (`POINTED → POINTED`, `CANCELED → POINTED`) ;
  404 règlement d'un autre dossier.
- PJ règlement : upload 201 + relecture ; `Category` = `Yes.Stl.Credit.Settlement` ; 400 fichier
  vide ; 404 règlement d'un autre dossier ; téléchargement avec le bon content-type.
- Invariant : aucun chemin ne produit `POINTED` sans `SettlementDate`, ni l'inverse.
- Régression : un règlement legacy `POINTED` **sans** PJ reste lisible (`GET` + document).
- `PerimetreInterceptorInvarianteTest` doit rester vert : les nouvelles routes gardent l'id du
  dossier en **segment 3** (`/api/dossiers/{id}/...`), condition structurelle du filtrage de
  périmètre.
- `AutorisationVersementTest` : toujours 1 page.

## 10. Impact front (hors périmètre de cette branche)

`typeId`, `payeurId`, `dateEcheance` deviennent obligatoires sur un endpoint que `crm-prototype`
appelle déjà : **rupture de contrat assumée**. Le front devra suivre dans sa propre branche et sa
propre PR, après `npm run docs:update` et diff de `docs/api-docsV4.json`.
