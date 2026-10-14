# DossierSousArticlesController

Onglet « Sous articles » de la fiche dossier. Aide OMRAN : « Lorsqu'on ajoute ou on supprime un sous article, le système ne modifie pas le montant remise, mais il modifie le taux de remise. » Le taux devient montant de remise HT / prix HT avant remise (article + sous-articles), et s'applique au prix stocké de l'article et à chaque sous-article (`Sale.cs:RecalcDiscount`) ; le prix de vente HT et TTC suivent. Le schéma de financement n'est pas modifié : son écart se lit sur `GET /api/dossiers/{id}/echeancier/schema`. Chaque ajout ou retrait écrit une ligne d'historique « AFFECTATION » ou « ANNULATION AFFECTATION » et signe le dossier. Écritures réservées au rôle Administrateur (elles changent le prix).

## ajouter

L'article doit être un sous-article libre (`IsUnderItem`, sans `ParentId`, ni dossier ni location, pas « toujours affecté ») de la tranche et de la vocation du dossier, comme le sélecteur d'OMRAN (`UnderItems.cs`). Il est rattaché au dossier et à l'article principal.

## retirer

Refusé (409) pour un sous-article « affecté par défaut » : « Vous ne pouvez pas supprimer un sous article affecté par défaut à l'article en cours ! ». L'article du stock est libéré (`ComFolderId` et `ParentId` remis à NULL, `IsUnderItem` = sous-article d'origine).
