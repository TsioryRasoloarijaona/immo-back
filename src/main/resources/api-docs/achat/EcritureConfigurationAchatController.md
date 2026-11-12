# EcritureConfigurationAchatController

## creerStatut

Ordre des contrôles : droit (403), puis toute la saisie (400), puis l'unicité du code (409) ; rien n'est écrit avant. Obligatoires : `ordre`, `code`, `description`, `typeId` — le message du 400 nomme le champ. `code` et `description` sont écrits sans espaces de bord, 255 caractères au plus ; `notes` 1000 au plus. `couleur` est un nom de couleur .NET (`Pink`, `DarkGreen`…, sans casse) ou vide ; `imageId` doit exister dans la bibliothèque (`/images`) ; chaque nom des listes de champs doit venir du catalogue ; un type de document au plus une fois. L'`ordre` n'est pas unique. Trace d'audit OMRAN `Yes.Buy.Config.BuyDocumentStatus`.

## modifierStatut

La fiche est **remplacée** : un champ absent prend sa valeur par défaut (`dureeVieJours` 0, booléens false, `imageId` et `notes` vides, listes vides) et les documents à joindre sont réécrits. Mêmes contrôles que la création, précédés du 404 ; le code du statut lui-même (autre casse comprise) n'est pas un doublon.

## supprimerStatut

Refusé (409) tant qu'une pièce, une transition (départ ou arrivée) ou une ligne d'historique cite le statut ; le message donne les trois nombres. Les documents à joindre du statut sont supprimés avec lui.

## creerTransition

Ordre des contrôles : droit (403), saisie (400 : statut de départ ou d'arrivée absent ou inconnu, départ = arrivée, gestion ou rôle inconnu), puis unicité (409) du triplet gestion / départ / arrivée — l'absence de gestion compte comme une gestion. `roleIds` vide ou absent = aucun rôle.

## modifierTransition

La transition est remplacée, ses rôles réécrits en bloc. Mêmes contrôles que la création, précédés du 404.
