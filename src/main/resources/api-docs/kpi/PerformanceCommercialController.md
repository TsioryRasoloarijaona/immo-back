# PerformanceCommercialController

Indicateurs de performance des commerciaux sur une période — activité réalisée (appels, visites, interactions), portefeuille confié (prospections, réactivité) et résultat (dossiers, montants, taux de transformation).

Deux attributions cohabitent et il faut le savoir pour lire les chiffres : l'**activité** est comptée pour celui qui l'a saisie, le **portefeuille** pour le commercial assigné. Un chef qui appelle sur la prospection d'un commercial voit l'appel chez lui, pas chez l'autre.

Tous les compteurs respectent la visibilité par entité juridique et le périmètre commercial de l'appelant. Un commercial restreint verra donc des chiffres inférieurs à son activité réelle — c'est volontaire.

## moi

Les indicateurs du commercial rattaché au compte connecté, sur la période demandée — ou ceux du **compte
lui-même** s'il n'est rattaché à aucun employé (`commercialId` nul, `utilisateurId` renseigné, pas
de portefeuille).

**L'activité compte toutes les actions**, détaillées dans `actionsParType`, actions « Système »
exclues ; l'auteur suit la règle du suivi des actions.

**Renvoie 200 avec tous les compteurs à zéro** quand rien n'a été fait sur la période — pas une
erreur. Le 404 est réservé à un appel sans compte applicatif identifiable.

Les taux valent `null`, jamais `0`, quand leur dénominateur est nul : « aucun appel passé » et « des
appels, aucun rendez-vous » sont deux situations opposées. Afficher « — » plutôt que « 0 % ».

## tous

Classement des commerciaux sur la période, les plus performants d'abord (dossiers créés décroissant,
puis appels passés).

**Seuls les commerciaux ayant au moins une ligne sur la période apparaissent** — une interaction,
une prospection assignée ou un dossier. Ne pas supposer que cette liste couvre tous les commerciaux
de `GET /api/commerciaux` : sur la base réelle, 60 des 75 comptes sont désactivés et une vingtaine
de personnes seulement travaillent sur une période donnée.

**Lire chaque taux avec son effectif.** Sur une période courte le dénominateur des taux de
transformation est faible et le pourcentage très bruyant ; `dossiersCrees` et
`prospectionsAssignees` sont là pour ça.
