# ConfigurationAchatController

Configuration des pièces d'achat en **lecture seule** : statuts (`BuyDocumentStatus`) et transitions (`BuyDocumentWorkFlow`). OMRAN reste maître du paramétrage ; aucune écriture. Tout compte connecté lit. Voir `instructions/FEATURE-achat-regle-commune.md` §4.

## transitions

Les transitions sont **propres à une gestion** (`StrEntityId`). Tri : libellé de gestion (la ligne sans gestion, « Sans gestion », en dernier), puis `ordre`, puis `description`. `roles` est vide pour une transition sans rôle. `nombrePassages` compte les lignes d'historique (`BuyDocumentStatusHistory.buyDocumentWorkFlowId`) portant **l'identifiant** de la transition, quelle que soit la gestion actuelle de la pièce ; la ligne de création d'une pièce n'en porte aucune.
