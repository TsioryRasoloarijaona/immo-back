# PlanificationController

Planification Gantt d'OMRAN (`CfgGanttProject`, tâches `CfgGanttTask`, intervenants `CfgGanttOwner`). Trois natures : **modèle** (`ObjectId` et `VersionDate` NULL), **planification d'un projet** (`ObjectId` = projet, au plus une par projet), **version** (copie datée, `VersionDate` renseignée, lecture seule). Lecture : tout compte authentifié pour un modèle ; pour un projet ou une version, le projet (relu sur la ligne stockée) doit être dans le périmètre de l'appelant, sinon 404. Écritures : droit OMRAN `Yes.Cfg.Front.Gantt.GanttProject` (sauvegarde de version : `Yes.Prj.Front.Tdb.ProjectPrevision.toolSaveVersion`) ; ordre des refus 404 → 403 → 409 (version) → 400. Les écritures sur l'arbre renvoient la planification complète : rangs (`Sorting`), profondeurs (`Depth`) et texte des prédécesseurs sont recalculés par le serveur à chaque écriture. Voir `instructions/FEATURE-n9-planification.md` (R5-R14, D5-D7).

## creerModelePlanification

Corps `{code, description, notes?, dateDebut?}` ; date de début absente = aujourd'hui. Code unique parmi toutes les planifications (409).

## supprimerModelePlanification

Supprime aussi les versions du modèle, leurs tâches et intervenants. Jamais refusé : un clone est une copie sans lien.

## lirePlanification

Ouvre un modèle, la planification d'un projet ou une version (`nature` = VERSION : lecture seule). Tâches triées par rang ; `predecesseurs` en liste `{tacheId, type}`, `intervenants` en liste d'ids.

## sauvegarderVersionPlanification

Copie datée de la planification d'un projet (clone OMRAN `Clone(null, null, maintenant)`) : code « <code> - dd/MM/yyyy HH:mm:ss », description = date courte, tâches et intervenants copiés, lots non rattachés. 409 sur un modèle ou une version, ou si une version porte déjà ce code (deux sauvegardes dans la même seconde). `message` = « Version '<description>' sauvegardée correctement. ».

## modifierDateDebutPlanification

Corps `{dateDebut}`. Décale début et date de contrainte de toutes les tâches de l'écart entre l'ancienne et la nouvelle date ; l'échéance n'est décalée que si elle est postérieure à la **nouvelle** date de début (parité `GanttEditor.cs:ModifyStartDate`).

## creerTachePlanification

Corps facultatif (mêmes champs que la modification). Sans `parentId` : racine, code = nombre de racines + 1, « Tâche <code> », durée 1, début de la planification. Avec `parentId` : dernier enfant, code « <code du parent>.<n> », « Sous-tâche <code> », durée et début du parent. 400 si le parent n'est pas une tâche de cette planification.

## modifierTachePlanification

Champs absents ou null : inchangés. `parentId` déplace la tâche (indenter) ; `racine: true` la remonte en racine. 400 : code vide, durée ou alerte négative, avancement hors 0-100, parent d'une autre planification ou créant un cycle, prédécesseur inconnu ou égal à la tâche, intervenant étranger à la planification, type de tâche inconnu. Un type de dépendance absent vaut `FIN_DEBUT`.

## supprimerTachePlanification

Supprime la tâche et tous ses descendants ; ils disparaissent des prédécesseurs des autres tâches.

## ordonnerTachesPlanification

Corps `{tacheIds}` : toutes les sœurs d'un même parent, dans l'ordre voulu (chaque sœur garde son sous-arbre). 400 si la liste n'est pas exactement l'ensemble des sœurs d'un parent.

## supprimerIntervenantPlanification

L'intervenant est retiré des intervenants (`ResourceNames`) de toutes les tâches.

## lirePlanificationProjet

Planification principale du projet. Absente : 404 avec `code` = `PLANIFICATION_ABSENTE` et `detail` = « Aucune planification n'est définie pour le projet en cours ! » — le front propose alors de la créer depuis un modèle.

## clonerPlanificationProjet

Corps `{modeleId}`. Copie le modèle vers le projet : code « <code du projet> - dd/MM/yyyy HH:mm:ss », description « Version principale », notes copiées, début aujourd'hui ; tâches et intervenants copiés avec parents et intervenants remappés ; chaque tâche est rattachée au lot du projet de même catégorie de dépense, sinon à aucun.

## listerTachesLot

Tâches de la planification du projet du lot rattachées à ce lot (`PrjMarketId`). Lot lu comme sa fiche : hors projet habilité ou statut invisible = 404. Planification absente : même 404 `PLANIFICATION_ABSENTE` que `lirePlanificationProjet`.
