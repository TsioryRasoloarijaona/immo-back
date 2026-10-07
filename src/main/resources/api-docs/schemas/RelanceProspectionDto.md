# RelanceProspectionDto

Une relance commerciale à traiter : l'échéance posée par un commercial sur une interaction, encore
armée. Une relance désarmée (prospection vendue) ou rattachée à une échéance de paiement n'apparaît
jamais ici.

## interactionId

Interaction qui porte la relance (`ComActionMessage`). C'est elle qu'il faut viser pour désarmer ou
reporter l'échéance, pas la prospection.

## preProspection

`true` s'il s'agit d'une pré-prospection — un lead non encore transformé, de type légataire
`ComPreProspection`, même discriminant que `GET /api/pre-prospections`. Une pré-prospection saisie
dans OMRAN peut déjà porter un commercial : `nonAffectee` n'est donc pas systématique.

## prospectNom

Nom du prospect : celui de la fiche tiers, à défaut le nom saisi au premier contact
(`TempDescription`) — c'est le cas des pré-prospections dont le prospect n'a pas encore été créé.
`null` seulement si aucun des deux n'est renseigné.

## telephone

Numéro joignable : le mobile de la fiche, à défaut son fixe, à défaut le numéro saisi au premier
contact. Aucune de ces sources ne suffit seule ; ensemble elles couvrent 8 591 des 8 613
prospections ouvertes.

## dateRelance

Jour où la relance était due. La colonne légataire est un `datetime` mais son heure ne porte aucune
information — les 3 765 relances armées sont toutes à minuit —, elle est donc ramenée au jour.

## commercialId

Commercial à qui la relance revient. Résolu par l'affectation de la prospection d'abord (97,1 % des
cas), à défaut par l'auteur de l'interaction. `null` quand aucune des deux sources ne renseigne
personne.

## nonAffectee

`true` quand la relance n'appartient à personne — 2,9 % des cas en base réelle. **Ces lignes sont
servies quelle que soit la `portee` demandée, y compris `MOI`** : n'étant à personne, les réserver à
un périmètre les rendrait invisibles. À présenter comme une file à récupérer.

## derniereInteractionDate

Date de l'interaction qui porte la relance — le dernier échange connu avec ce prospect au moment où
l'échéance a été posée.
