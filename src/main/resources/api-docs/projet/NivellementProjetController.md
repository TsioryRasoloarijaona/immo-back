# NivellementProjetController

Bouton « Nivellement » de la fiche projet de configuration : arbre des niveaux du projet (`CfgTrancheLevelingDefinition`, `CfgTrancheId` = `PrjProject.PKey`). Lecture : tout compte qui voit le projet (habilitation `AdmUserHabilitation.PrjProjectId`). Écritures : rôle Administrateur (règle d'URL de la configuration projet) et projet visible. Le projet d'un nœud est lu sur la ligne stockée : un nœud d'un autre projet répond 404. Voir `instructions/FEATURE-n9-planification.md` (R4, D6, D7).

## lister

Liste à plat, triée par rang : l'arbre se reconstruit avec `parentId` (null = racine). Liste vide = aucun niveau défini.

## creer

Corps `{"parentId": uuid|null}`. Le serveur calcule le rang (nombre de frères + 1) et le code : code de la ligne du nivellement du projet (`StkLevelingLine`) dont le rang égale la profondeur du nouveau nœud, suivi du rang (« BLOC 1 », « ETAGE 2 »). 400 « Aucun niveau n'est défini pour ce projet : renseigner le nivellement du projet. » si le projet n'a pas de nivellement ; 400 « Vous ne pouvez pas ajouter un niveau inférieur » au-delà de la profondeur du nivellement. Parent d'un autre projet ou inconnu : 404.

## modifier

Corps `{"code": "…"}` : seul le code change, les autres champs sont ignorés.

## supprimer

Supprime le nœud et tous ses descendants. 409 « Suppression impossible : … » avec le nombre de références si le nœud ou un descendant est cité par un lot (`PrjMarketHierarchy.StkHierarchyId`), une journée de travail (`GrhWorkedDay`) ou un avancement de réunion (`BulMeetingPrjProjectProgressCriteriaLine`).
