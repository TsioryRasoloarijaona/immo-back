# FEATURE — Désarchiver une prospection

Branche : `feat/desarchiver-prospection` (coupée depuis `main` le 24/09/2026).

## Objectif

Ajouter l'opération inverse de `PATCH /api/prospections/{id}/archiver` : rouvrir une
prospection archivée pour qu'elle réapparaisse dans le pipeline. Aligner au passage
l'archivage sur les effets de bord réels du légataire.

## Ce que fait OMRAN (trace d'audit `C:\backup\*.sqlaudit`, 24/09/2026)

L'utilisateur a archivé puis désarchivé la prospection `26/05947`
(`PKey = b2a7fe08-2122-4314-8e3e-1513c1b20fbd`) dans OMRAN. La trace et
`AdmWebHistorics.details` donnent la séquence exacte.

**Archivage (10:18:02)** — trois écritures :

1. `UPDATE ComProspection` (toutes colonnes, ORM). Diff réel d'après l'historique :
   `IsClosed` False→True, `ClosingDate` = maintenant,
   `ComProspectionClosingReasonId` = raison choisie (« ATTEND APPARTEMENT TEMOIN »).
2. `UPDATE ComActionMessage SET WithoutRevival=1 WHERE AlertValidityDate IS NOT NULL
   AND ComDocumentId IS NULL AND ComProspectionId = … AND IsUnpaid=0` — les relances
   de la prospection sont désarmées. **Même prédicat** que la transformation en vente
   (`ComActionMessageRepository.desarmerRelancesDeLaProspection`, posé par
   `DossierService.create`), plus `IsUnpaid=0` (colonne non mappée ici, ignorée).
3. `UPDATE AdmAlertData SET Active=0` sur les alertes de ces messages
   (table non mappée dans ce dépôt — non reproduit).

**Désarchivage (10:18:15)** — une seule écriture :

1. `UPDATE ComProspection`. Diff réel : `[["IsClosed","True","False"]]` — **rien
   d'autre**. `ClosingDate` et `ComProspectionClosingReasonId` **restent en place**,
   les relances désarmées ne sont **pas** réarmées, les alertes ne sont pas réactivées.

## Justification sur données réelles (`yesRemsProd`, prospections vente)

| `IsClosed` | `ClosingDate` | raison | lignes |
|---|---|---|---:|
| 0 | vide | vide | 4 702 |
| 0 | vide | posée | 7 |
| 0 | **posée** | vide | 1 |
| 0 | **posée** | **posée** | **88** |
| 1 | vide | vide | 536 |
| 1 | vide | posée | 1 |
| 1 | posée | vide | 349 |
| 1 | posée | posée | 8 442 |

Les 88 lignes ouvertes portant date **et** raison de clôture sont des prospections
désarchivées : OMRAN ne nettoie rien. Conséquences :

- **« archivée » se lit uniquement sur `IsClosed`** (`fermee` dans l'API), jamais sur
  `dateFermeture != null` — sinon on affiche 89 prospections ouvertes comme archivées ;
- notre désarchivage conserve date et raison, comme OMRAN, pour que l'historique de
  clôture reste lisible (et que la ligne reste identique à ce qu'OMRAN aurait produit).

## Comment reconnaître une archivée dans l'API (état des lieux, inchangé)

- Liste `GET /api/prospections` : `fermee` (`ProspectionListItemDto`), colonne groupable
  `ColonneProspection.FERMEE`. Le filtre `?ouvertes=true` reproduit la vue de travail
  OMRAN (`IsClosed = 0`, `NULL` accepté côté app). Sans `ouvertes`, les archivées sont
  mêlées aux ouvertes.
- Fiche `GET /api/prospections/{id}` : `fermee`, `dateFermeture`, `raisonFermetureId`,
  `raisonFermeture`, `raisonFermetureCode`, `raisonFermetureTexte`.
- Ces champs existaient sans `@Schema` : documentés dans cette branche.

## Comportement

### `PATCH /api/prospections/{id}/desarchiver` — nouveau

Sans corps.

- `IsClosed = false` ; `ClosingDate`, `ComProspectionClosingReasonId` et
  `ClosingReasonText` **conservés** (fidèle au légataire) ;
- `UserUpdate` / `UserUpdateDateTime` = utilisateur connecté / maintenant (OMRAN les
  réécrit à chaque modification) ;
- les relances désarmées à l'archivage ne sont pas réarmées (fidèle au légataire) ;
- réponse `200` → `ProspectionDetailDto` (`fermee = false`) ;
- `404` prospection inconnue ou hors périmètre commercial ;
- `409` la prospection n'est **pas** archivée (`IsClosed` false ou null). OMRAN ne
  refuse rien, mais côté API un désarchivage sans archivage est un appel incohérent :
  `ProblemDetail` RFC 7807.

### `PATCH /api/prospections/{id}/archiver` — aligné

Inchangé côté contrat (`raisonId` ou `raisonTexte` requis, `fermee = true`,
`dateFermeture = maintenant`). Ajouts, fidèles à la trace :

- appel de `desarmerRelancesDeLaProspection` — les relances en cours de la prospection
  sont désarmées (`WithoutRevival = 1`), comme lors de la transformation en vente.
  Sans cela, une prospection archivée continuait d'alimenter le tableau de bord des
  relances ;
- `UserUpdate` / `UserUpdateDateTime` renseignés.

Non reproduit : `AdmAlertData.Active = 0` (table hors dépôt) — le tableau de bord des
relances lit `WithoutRevival`, pas `AdmAlertData`, donc sans effet visible côté app.

## Impact front

Additif uniquement. Nouveau bouton « Désarchiver » possible sur une fiche `fermee = true`,
appelant `PATCH /api/prospections/{id}/desarchiver`. Après désarchivage, `dateFermeture`
et `raisonFermeture` restent renseignés : **le badge « Archivée » doit se brancher sur
`fermee`, pas sur la présence d'une date de clôture.**

## Fichiers

- `instructions/FEATURE-desarchiver-prospection.md` — ce document.
- `services/prospection/ProspectionService.java` — `desarchiver()` ; `archiver()` désarme
  les relances et pose `UserUpdate`.
- `controller/prospection/ProspectionController.java` — route `desarchiver`, springdoc sur
  `archiver` et `desarchiver`.
- `dto/prospection/ProspectionDetailDto.java`, `ProspectionListItemDto.java` — `@Schema`
  sur les champs de clôture.
- `test/.../ProspectionEtapeArchiverTest.java` — désarchiver : happy path (date et raison
  conservées), 404, 409 ; archiver : relances désarmées.
