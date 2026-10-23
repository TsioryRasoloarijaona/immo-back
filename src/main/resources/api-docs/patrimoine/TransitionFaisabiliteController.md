# TransitionFaisabiliteController

Statuts et transitions d'une étude (OMRAN `FeasibilityStudyWorkFlow`), historique, pièces jointes (porteur « etude »,
`AdmAttachedFile.Category` = `Yes.Stk.StkFeasibilityStudy`) et export Excel. Lecture Administrateur + Direction
Générale ; dépôt et suppression de pièce Administrateur ; aucune clé d'API (403 `HORS_PORTEE`). Étude inconnue ou de
société inactive → 404.

## transitions

Transitions dont le statut de départ est le statut courant et dont un rôle autorisé (`AdmRole.Code`) est un rôle du
jeton de l'appelant ; une transition sans rôle n'est proposée à personne. Pour chacune : champs obligatoires de la
transition (`renseigne` faux = texte vide ou date absente ; un nombre, même 0, est toujours renseigné, comme OMRAN) et
documents exigés (`fourni` = une pièce jointe vivante de l'étude de ce type ; `IsMandatory` n'est pas lu : toute ligne
est exigée). `possible` = étude non bloquée, champs renseignés, documents fournis.

## executerTransition

Contrôles dans l'ordre : étude bloquée → 409 « Étude bloquée » ; transition inconnue → 404 ; transition hors de celles
de `GET …/transitions` (départ ≠ statut courant, aucun rôle commun) → 409 avec `transitionsAutorisees` (descriptions
des transitions permises) ; champs vides de la transition et documents manquants → 400, tous les messages ; champs
obligatoires du nouveau statut vides → 400, rien n'est écrit. Sinon : statut écrit, ligne d'historique (date,
statut, utilisateur), recalcul complet de l'étude (bilan, trésorerie) dans la même transaction. La note obligatoire,
la notification et l'e-mail de la transition sont stockés sans effet (comme OMRAN).

### reponse 400

`detail` : « Transition : » puis une ligne « Le champ « X » est obligatoire » par champ vide de la transition, puis une
ligne « Le document de type : X est obligatoire » par document manquant ; ou « Statuts : » puis les champs vides du
statut d'arrivée ; ou `transitionId` absent.

### reponse 409

Étude bloquée, ou transition non permise à l'appelant depuis le statut courant (propriété `transitionsAutorisees`).

## historique

Une ligne par changement de statut, création comprise : date, code, libellé et ordre du statut, utilisateur
(`AdmUser.Description`).

## pieces

Pièces vivantes de l'étude ; une pièce supprimée logiquement ne compte plus pour un document exigé.

## ajouterPiece

Le `typeId` choisi au dépôt est celui que compte un document exigé par une transition. Étude bloquée → 409 (OMRAN
masque « Fichiers attachés » en verrouillé).

## exporter

11 feuilles : Identité, Bilan, Terrain, Exécution, Délais, Financement, Données, Unités, Tranches, Unités par tranche,
Bilan détaillé. Valeurs du calcul partagé (bilan stocké, prévisions, trésorerie), jamais recalculées dans l'export.
Sans tranche, « Tranches » et « Unités par tranche » n'ont que leurs titres. Permis sur une étude bloquée.
