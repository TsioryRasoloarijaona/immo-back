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

## Champs obligatoires à la création — `POST /api/prospections`

Deuxième volet, même branche. La liste des champs obligatoires vient directement du
**message de validation de l'écran OMRAN** (prospection `26/05946`) :

> La valeur du champ [Notes] est obligatoire — [Source de contact] — [Type] —
> [Nature] — [Tranche].

Mapping libellé → colonne **vérifié sur la trace d'audit réelle** (au brouillon de
17:02 la ligne n'avait que `StkVocationId` + `TempDescription` ; les 5 colonnes
passées de `NULL` → remplies à 17:23 sont exactement celles réclamées) :

| Champ OMRAN | Colonne SQL | Champ API | Valeur test (26/05946) |
|---|---|---|---|
| Notes | `Notes` | `notes` | « note de omran » |
| Source de contact | `ComProspectionOriginId` | `origineId` | FACEBOOK |
| Type | `ComActionMessageTypeId` | `typeContactId` | Message entrant |
| Nature | `ComProspectionKindId` | `etapeId` | 1 A TRAITER |
| Tranche | `StkHierarchyId` | `trancheId` | AYAD PRIVATE FERME BRETONE |

`POST /api/prospections` exige désormais ces 5 (+ `prospectId`). Absents ⟶ **400**
nommant le champ ; `notes` vide/blanc est refusé (passe par `texteLibre()`).
`commercialId` **reste optionnel** — OMRAN ne le bloque pas à la création (il peut
être posé plus tard par `transformer`).

**Piège vérifié** : « Type » = `ComActionMessageType` (`typeContactId`), **pas** la
vocation (`StkVocationId` = VENTE, déjà présente au brouillon donc non réclamée). Le
front doit brancher le libellé « Type » sur `typeContactId`, jamais `vocationId`.

### Impact front (2)

`POST /api/prospections` avec seulement `{prospectId}` renvoie maintenant **400**. Le
formulaire « Nouvelle prospection » doit fournir Notes + Source de contact + Type +
Nature + Tranche. Source de vérité : `/api-docs` (DTO annoté `requiredMode = REQUIRED`).

## Autres tranches (`ComProspectionProject`) — synchro descendante manquante

Constaté sur la trace d'audit (21/09, 17:46) : OMRAN écrit les « autres tranches »
d'une prospection dans **`ComProspectionProject`** — `INSERT INTO
ComProspectionProject(stkHierarchyId, comProspectionId, stkItemId, userCreate, …)`,
une ligne par tranche secondaire (pas un batch). Pour lister, il fait
`SELECT PKey FROM ComProspectionProject WHERE ComProspectionId = …` ; le choix des
tranches disponibles vient du référentiel `CfgTranche`.

**Côté app la fonctionnalité existe déjà** : `GET/POST/DELETE
/api/prospections/{id}/autres-tranches` (mapping identique). Le problème était
**la synchro** : `ComProspectionProject` n'avait pas de Change Tracking sur la
source, et la synchro descendante lit la liste des tables suivies **dynamiquement**
depuis `sys.change_tracking_tables` (`SynchronisationDescendante.tablesSuivies()`,
ligne 260) — une table sans CT lui est donc invisible. **Aucun code à changer.**

Tables de détail de prospection qui étaient hors CT (idem : villes, profils) :

```sql
ALTER TABLE ComProspectionProject         ENABLE CHANGE_TRACKING;
ALTER TABLE ComProspectionCity            ENABLE CHANGE_TRACKING;
ALTER TABLE ComProspectionProfile         ENABLE CHANGE_TRACKING;
ALTER TABLE ComProspectionSelectedProfile ENABLE CHANGE_TRACKING;
ALTER TABLE ComProspectionProfileCategory ENABLE CHANGE_TRACKING;
```

(`ComProspectionCategory` et `ComProspectionType` étaient déjà suivies.) À appliquer
sur la source réelle (115) au déploiement ; réversible par `SET CHANGE_TRACKING = OFF`.

**Preuve sur banc local (21/09)** : CT activé sur `yesRems`, une instance backend
deux-bases tournant. Au cycle suivant, `ComProspectionProject` est entré dans le
périmètre (`AppSyncEtat` créé, baseline) puis a suivi en incrémental — les lignes
d'autres-tranches créées par un utilisateur OMRAN (« Macire Kamara ») sur la source
`yesRems` sont arrivées dans la copie `yesRemsProd` (INSERT, UPDATE et DELETE),
convergence source = copie vérifiée via `AppSyncEtat` et le contenu des tables (pas
le log, bufferisé). Les échecs transitoires observés au démarrage sont la
convergence FK normale (enfant avant parent → rejoué au cycle suivant).

## Fichiers

- `dto/prospection/TransformerProspectionRequest.java` — `trancheId` + `articleId`, `@Schema`.
- `dto/prospection/CreerProspectionRequest.java` — `@Schema requiredMode` sur les 5 champs obligatoires.
- `services/prospection/ProspectionService.java` — `transformer()` (invariant tranche) + `creerProspection()` (5 champs requis).
- `controller/prospection/ProspectionController.java` — springdoc sur `transformer` et `POST /api/prospections`.
- `test/.../ProspectionTransformerTest.java` — 7 cas (dont 400 sans tranche, 400 article incohérent).
- `test/.../ProspectionHistoriqueTest.java` — mis à jour (fournit `trancheId`).
- `test/.../ProspectionCreationTest.java` — 12 cas (un « manquant → 400 » par champ + notes vide).
- `test/.../CreationConformiteLegataireTest.java` — helper de création alimente les 5 champs.
