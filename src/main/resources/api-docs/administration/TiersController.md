# TiersController

Fournisseurs (`CfgTierType` FOU) et contacts (CNT) d'OMRAN, une famille par segment d'URL : une autre valeur répond 404, un identifiant d'un autre type aussi. Aucune donnée de paie n'est lue ni écrite. Réservé à l'Administrateur. Voir `instructions/FEATURE-admin-tiers.md`.

## lister

La recherche est un « contient » sans casse ; `%`, `_` et `[` y sont pris à la lettre ; vide ou faite d'espaces = pas de filtre. `contacts` joint les libellés de `CfgTierContact` par « , ».

## creer

Personne morale : `raisonSociale` requise. Personne physique : `prenom`, `nom`, `titreId` et `sexeId` requis, `raisonSociale` et `description` valent « prénom nom ». Toujours requis : `personneMorale`, `paysResidenceId`, `deviseId`, `formeSocialeId`, et `mobile` ou `email`. Un doublon n'est pas refusé.

## modifier

Mêmes contrôles qu'à la création. Le corps remplace la fiche : un champ omis est vidé.

## supprimer

Une clé étrangère restrictive (dossier, mouvement, contact lié à un autre tiers…) refuse la suppression en 409 sans rien modifier. Sinon les cascades de la base emportent les liens et les actions du tiers.
