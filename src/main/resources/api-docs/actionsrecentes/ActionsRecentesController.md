# ActionsRecentesController

Le panneau « Actions récentes » : ce qui vient d'être fait sur les prospections, pré-prospections et dossiers, avec de quoi rouvrir la fiche. Détail : `instructions/FEATURE-actions-recentes.md`.

## lister

Union de trois sources, triée par `date` décroissante puis tronquée à `limite` :

- l'historique des prospections (`StrHistory` sans `StrConfigEntityId`) : création, prospect créé ou
  rattaché, transformation, changement d'étape, archivage, désarchivage ;
- l'historique des statuts de dossier (`type = DOSSIER_STATUT`) ;
- les interactions et notes (`ComActionMessage`, `type = INTERACTION`).

**Portée.** `MOI` : seulement les lignes dont l'auteur est l'appelant — une ligne sans auteur n'y
figure jamais ; un compte sans `AdmUser` reçoit une liste vide. `EQUIPE` : toutes les lignes des
projets du périmètre commercial de l'appelant. Les deux portées appliquent **le même périmètre que
les listes** (exempté ou sans affectation : aucun filtre ; fiche sans projet : visible) : une
action sur un projet retiré du périmètre disparaît aussi de `MOI`.

Une ligne d'historique dont la fiche a été supprimée est ignorée, ainsi qu'une interaction
rattachée à aucune fiche.

**Ordre** : `date` décroissante, puis `id` décroissant comparé **en texte majuscule** (pas l'ordre
binaire d'un `uniqueidentifier`).

**« Voir plus ».** Rappeler avec `avant` = `date` **et** `avantId` = `id` de la dernière action
reçue : la suite repart exactement après elle, sans doublon ni trou, même quand plusieurs actions
partagent la même date.

### param avant

Curseur, date-heure ISO-8601 : la `date` de la dernière action reçue. Seul (sans `avantId`), il ne
sert que les actions de date strictement antérieure — des actions de même date peuvent alors
manquer. Absent = les plus récentes.

### param avantId

Curseur, avec `avant` : l'`id` de la dernière action reçue. Sont servies les actions de date
antérieure, ou de même date et d'`id` inférieur dans l'ordre ci-dessus. Sans `avant` : 400.
