# ConfigurationProjetController

Configuration du module Projet : projets de chantier (`PrjProject`) et statuts de lot. Les projets sont réservés à l'Administrateur et montrés tous, hors périmètre ; les statuts de lot se lisent par tout utilisateur authentifié. Les types de lot passent par le moteur `/api/referentiels-admin/types-lot` (famille `PROJET`). Voir `instructions/FEATURE-projet-config.md`.

## creer

Le statut (premier statut de type « Nouveau » du workflow projet) et le type d'objet (`StrEntity` « PrjProject ») sont posés par le serveur, avec une ligne d'historique `StrHistory` « Création ». Défauts : non ventilé, taux 0, sans parties communes. Une habilitation (`AdmUserHabilitation`, portée projet seule) est créée pour l'utilisateur courant dans la même transaction : un échec de l'une annule l'autre.

## modifier

Remplacement complet des champs obligatoires ; les champs facultatifs absents (booléens, taux, 4 dates, nivellement, projet référence, lot prorata) restent inchangés. Pour vider une date ou un lien, le nommer dans `champsAVider`. Le lot gestionnaire du compte prorata doit être un lot de ce projet ; le projet référence, un autre projet existant.

## supprimer

Refusée en 409 « Utilisé par … » dès qu'une des 22 tables qui référencent `PrjProject` (lots, pièces d'achat, tranches, tâches, prêts, ventilations, réunions, prévisions, projet de référence…) a une ligne pour lui, y compris celles qu'une suppression en cascade viderait en base. Seules les habilitations du projet partent avec lui.

## statutsLot

Statuts des genres de lot (`StrEntity` dont le code commence par `PrjMarket` : entrepreneurs, fournisseurs, maîtres d'ouvrage), triés par genre puis par ordre. `couleur` est un nom de couleur .NET (`LimeGreen`…), à traduire côté écran.
