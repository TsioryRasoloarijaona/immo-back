# ConfigurationProjetController

Configuration du module Projet : projets de chantier (`PrjProject`) et statuts de lot. Les projets sont réservés à l'Administrateur et montrés tous, hors périmètre ; les statuts de lot se lisent par tout utilisateur authentifié. Les types de lot passent par le moteur `/api/referentiels-admin/types-lot` (famille `PROJET`). Voir `instructions/FEATURE-projet-config.md`.

## creer

Le statut (« Nouveau », seul statut du workflow projet) et le type d'objet (`StrEntity` « PrjProject ») sont posés par le serveur. Défauts : non ventilé, taux 0, sans parties communes. Une habilitation (`AdmUserHabilitation`, portée projet seule) est créée pour l'utilisateur courant dans la même transaction : un échec de l'une annule l'autre.

## supprimer

Refusée en 409 « Utilisé par … » dès qu'une des 22 tables qui référencent `PrjProject` (lots, pièces d'achat, tranches, tâches, prêts, ventilations, réunions, prévisions, projet de référence…) a une ligne pour lui, y compris celles qu'une suppression en cascade viderait en base. Seules les habilitations du projet partent avec lui.

## statutsLot

Statuts des genres de lot (`StrEntity` dont le code commence par `PrjMarket` : entrepreneurs, fournisseurs, maîtres d'ouvrage), triés par genre puis par ordre. `couleur` est un nom de couleur .NET (`LimeGreen`…), à traduire côté écran.
