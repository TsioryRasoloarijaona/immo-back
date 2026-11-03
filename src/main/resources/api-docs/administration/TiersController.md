# TiersController

Fournisseurs (`CfgTierType` FOU), contacts (CNT), banques (BAN) et organismes (ORG) d'OMRAN, une famille par segment d'URL : une autre valeur répond 404, un identifiant d'un autre type aussi. Aucune donnée de paie n'est lue ni écrite. Réservé à l'Administrateur. Voir `instructions/FEATURE-admin-tiers.md` et `instructions/FEATURE-admin-banques-organismes.md`.

## lister

La recherche est un « contient » sans casse ; `%`, `_` et `[` y sont pris à la lettre ; vide ou faite d'espaces = pas de filtre. Colonnes cherchées : code, description, raison sociale, mobile, téléphone, e-mail, ICE ; banques et organismes : code, description, mobile, téléphones, fax, e-mail, site, notes, adresse. `natureId` n'est lu que pour les fournisseurs. `contacts` joint les libellés de `CfgTierContact` par « , ».

## creer

Personne morale : `raisonSociale` requise. Personne physique : `prenom`, `nom`, `titreId` et `sexeId` requis, `raisonSociale` et `description` valent « prénom nom ». Toujours requis : `personneMorale`, `paysResidenceId`, `deviseId`, `formeSocialeId`, et `mobile` ou `email`. Un doublon n'est pas refusé.

Banques et organismes : fiche réduite, `code`, `description` et `paysId` seuls requis, compteur jamais utilisé. La description passe en majuscules et la raison sociale la suit, le pays de résidence suit `paysId`, `matriculeFiscal` s'écrit aussi dans `IDNumber`. La forme sociale vient du paramètre OMRAN de la famille, la devise reste vide. Seuls comptent `code`, `description`, `paysId`, `natureId`, `matriculeFiscal`, `registreCommerce`, `ice`, `compteComptableId`, `journalId`, les téléphones et leurs indicatifs, `fax`, `email`, `siteWeb`, les adresses, `codePostal`, `villeId`, `ville`, `notes` ; tout autre champ est ignoré.

## modifier

Mêmes contrôles qu'à la création. Le corps remplace la fiche : un champ omis est vidé. Le code ne change pas, sauf pour les banques et organismes, où il se modifie sous la même règle d'unicité ; leurs colonnes hors de la liste de `creer` restent telles quelles.

## supprimer

Une clé étrangère restrictive (dossier, mouvement, contact lié à un autre tiers…) refuse la suppression en 409 sans rien modifier. Sinon les cascades de la base emportent les liens et les actions du tiers.
