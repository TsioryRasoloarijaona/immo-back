# ConfigurationAchatController

Configuration des pièces d'achat : statuts (`BuyDocumentStatus`) et transitions (`BuyDocumentWorkFlow`). Toute lecture est ouverte à tout compte connecté ; l'écriture (`POST`/`PUT`/`DELETE` des mêmes chemins) exige le droit OMRAN `Yes.Buy.Front.Config.BuyDocumentStatus`. Voir `instructions/FEATURE-achat-regle-commune.md` §4 et `instructions/FEATURE-statuts-achat-parite.md`.

## statuts

Une ligne par statut, triée par `ordre` puis `code`. `champsObligatoires` / `champsObligatoiresFournisseur` sont des noms du catalogue `/statuts/champs-obligatoires`, stockés sans effet sur la saisie des pièces. `nombreTransitions` compte les transitions qui citent le statut en départ **ou** en arrivée (une transition comptée une fois) ; `nombreHistoriques` les lignes de `BuyDocumentStatusHistory`. Ces trois nombres sont ceux qui bloquent la suppression.

## transitions

Les transitions sont **propres à une gestion** (`StrEntityId`). Tri : libellé de gestion (la ligne sans gestion, « Sans gestion », en dernier), puis `ordre`, puis `description`. `roles` est vide pour une transition sans rôle. `nombrePassages` compte les lignes d'historique (`BuyDocumentStatusHistory.buyDocumentWorkFlowId`) portant **l'identifiant** de la transition, quelle que soit la gestion actuelle de la pièce ; la ligne de création d'une pièce n'en porte aucune.
