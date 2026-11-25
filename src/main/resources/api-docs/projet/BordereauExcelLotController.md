# BordereauExcelLotController

## exporter

Une ligne par groupe (Unité `GROUPE`, Code groupe = code du groupe parent), puis une ligne par ligne du bordereau
(Code groupe = code de son groupe). En-têtes : O.D.T., Code, Description, Unité, Quantité, P.U. HT, TVA, Remise, FODEC,
P.U. TTC, Notes, Code groupe. TVA et FODEC en pourcentage (20 = 20 %), Remise toujours vide. Nom du fichier :
`BDP_<code du lot>_<horodatage>.xlsx`.

## importer

Le classeur (en-têtes de l'export, `Code` obligatoire) est fusionné dans le bordereau existant, puis écrit par le même
chemin que le `PUT` du type demandé (mêmes montants, ventilation et refus). Appariement par Code, comparé après `trim`
et sensible à la casse ; une ligne absente du classeur est gardée telle quelle ; un code inconnu crée une ligne dans la
catégorie par défaut du lot. Une ligne d'Unité `GROUPE` est un groupe. Groupe parent : `Code groupe`, sinon le préfixe
du code avant le dernier point. P.U. TTC n'est lu que si P.U. HT vaut 0. Remise ignorée. O.D.T. numérique = ordre de la
ligne, sinon l'ordre est conservé (ligne existante) ou placé à la fin (ligne créée). Corps renvoyé : `BordereauDto`
(DEFINITIF) ou `BordereauEstimatifDto` (ESTIMATIF). Droits, verrou et visibilité sont contrôlés avant la lecture du
fichier.

### reponse 400

Fichier illisible, en-tête `Code` absent, type inconnu, code vide ou en double dans le classeur, code présent sur
plusieurs lignes du bordereau, nombre mal formé (message avec le numéro de ligne Excel), lot sans catégorie par défaut
alors qu'une ligne est à créer, ou mêmes refus de contenu que le `PUT`.
