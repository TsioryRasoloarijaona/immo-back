# FEATURE — Tranche (projet) obligatoire à la transformation en prospection

Branche : `feat/transformation-tranche-obligatoire` (coupée depuis
`feat/source-de-verite-115`, non encore mergée).

## Objectif

Rendre **obligatoire** le rattachement d'une tranche (projet) au moment où une
pré-prospection est transformée en prospection
(`POST /api/prospections/{id}/transformer`). Une prospection qualifiée sans projet
n'a pas de sens métier : c'est le rattachement à la tranche qui la place dans le
périmètre commercial et sous une entité juridique.

## Justification sur données réelles (`yesRems`, base rôle 115 en local)

Mesuré le 21/09/2026 sur `ComProspection` :

| population | total | avec tranche | sans tranche |
|---|---:|---:|---:|
| qualifiées (ont un commercial) | 14 725 | **14 700 (99,83 %)** | 25 |
| pré-prospections (sans commercial) | 2 966 | 2 869 (96,7 %) | 97 |

La règle formalise une pratique déjà quasi universelle : seules 25 prospections
qualifiées sur 14 725 dérogent. **Au niveau base, rien ne l'impose** — seule `PKey`
est `NOT NULL` sur `ComProspection` (vérifié `INFORMATION_SCHEMA.COLUMNS`). La
contrainte est donc **purement applicative** et **plus stricte que le légataire** :
la trace d'audit OMRAN (`C:\backup\*.sqlaudit`) montre qu'OMRAN lui-même a créé le
21/09 une prospection avec prospect + commercial + origine + étape **mais sans
`StkHierarchyId`** (code `26/05945`). C'est une décision produit assumée, pas un
alignement légataire.

## Comportement

`POST /api/prospections/{id}/transformer` — corps
`TransformerProspectionRequest` :

- `commercialId` (UUID) — **obligatoire**, inchangé.
- `trancheId` (UUID) — **nouveau, optionnel dans le corps mais la tranche est
  obligatoire à la sortie** :
  - fourni → la tranche est posée/remplacée, avec **contrôle de périmètre
    commercial** (403 si le projet n'est pas affecté à l'appelant) et **validation
    croisée de `articleId`** — mêmes règles que
    `PATCH /api/prospections/{id}/tranche-principale` ;
  - omis → la prospection doit **déjà** porter une tranche (posée à la création ou
    via `tranche-principale`), sinon **400**.
- `articleId` (UUID) — **nouveau, optionnel**. S'il est fourni il DOIT appartenir à
  la tranche effective (400 sinon). `articleId` sans `trancheId` ni tranche déjà
  posée → 400.

Réponses : `200` (transformée), `400` (pas de prospect lié / `commercialId` manquant
/ ref introuvable / article incohérent / **aucune tranche fournie ni déjà
présente**), `403` (tranche hors périmètre), `404` (prospection inconnue).

## Impact front

**Changement de contrat — un champ devient effectivement requis.** Avant, le front
pouvait appeler `transformer` avec seulement `{commercialId}`. Désormais, si la
pré-prospection n'a pas déjà de tranche, cet appel renvoie **400**. Le front doit :

- soit envoyer `trancheId` (et éventuellement `articleId`) dans le corps de
  `transformer` — recommandé : l'écran de transformation devrait demander le projet
  en même temps que le commercial ;
- soit s'assurer que la tranche est posée en amont
  (`PATCH /api/prospections/{id}/tranche-principale`) avant de transformer.

Le nouveau `403` (tranche hors périmètre) est à gérer comme sur
`tranche-principale`. La source de vérité reste `/api-docs` (springdoc à jour sur le
contrôleur et le DTO).

## Vérification (log OMRAN → yesRems local)

La trace d'audit locale (`C:\backup\*.sqlaudit`, `sys.fn_get_audit_file`) capture
bien les écritures d'OMRAN sur `yesRems` — entrées `application_name = 'OMRAN estate'`
sur `ComProspection` (IN/UP) confirmées pour la session du 21/09. C'est cette trace
qui a servi de source de vérité sur les colonnes réellement écrites par l'écran de
prospection et sur le fait qu'OMRAN n'impose pas la tranche.

## Fichiers

- `dto/prospection/TransformerProspectionRequest.java` — `trancheId` + `articleId`, `@Schema`.
- `services/prospection/ProspectionService.java` — `transformer()` : invariant tranche.
- `controller/prospection/ProspectionController.java` — annotations springdoc sur `transformer`.
- `test/.../ProspectionTransformerTest.java` — 7 cas (dont 400 sans tranche, 400 article incohérent).
- `test/.../ProspectionHistoriqueTest.java` — mis à jour (fournit `trancheId`).
