# ChampsSpecifiquesController

Champs spécifiques d'OMRAN (GEN0007) : l'Administrateur définit des champs libres par porteur (`AdmSpecificField` rattaché
à la ligne `AdmSpecificFieldEntity` du code de classe OMRAN, créée à la première définition), chaque objet porte ses
valeurs (`AdmSpecificFieldValue`, `NameSpaceId` = id de l'objet). Lecture pour tout compte qui voit l'objet (étude :
Administrateur et Direction Générale seulement, comme son audit) ; écritures réservées à l'Administrateur.

## lire

Toutes les définitions du porteur, triées `CategorySorting`, `Category`, `Sorting`. `valeur` = valeur enregistrée pour
l'objet, sinon la valeur par défaut de la définition.

## enregistrerValeurs

Formats : date `aaaa-mm-jj`, décimal / montant au point (`1234.5`), entier, pourcentage **en fraction** (`0.2` pour 20 %),
booléen `true` / `false`, liste : un des choix. Écrit `Value` (texte) et la colonne typée (`DateValue`, `DecimalValue`,
`IntValue`, `BoolValue`) ; une liste n'écrit que `Value`. Refus 400 nommant le champ : format invalide, choix hors liste,
champ obligatoire vide (« La valeur du champ [<libellé>] est obligatoire !! »), `champId` d'un autre porteur. Tout ou
rien : une valeur refusée n'en écrit aucune.

## creerDefinition

Le champ apparaît aussitôt sur tous les objets du porteur. Les choix d'une liste sont enregistrés séparés par « ; »
(`ComboValue`).

## supprimerDefinition

Supprime la définition **et ses valeurs sur tous les objets du porteur**, comme le bouton « Supprimer » d'OMRAN : le front
demande une confirmation explicite.
