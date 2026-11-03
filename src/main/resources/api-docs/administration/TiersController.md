# TiersController

Fournisseurs (`CfgTierType` FOU), contacts (CNT), banques (BAN), organismes (ORG) et personnels (EMP) d'OMRAN, une famille par segment d'URL : une autre valeur répond 404, un identifiant d'un autre type aussi. Aucune donnée de paie n'est lue ni écrite. Réservé à l'Administrateur. Voir `instructions/FEATURE-admin-tiers.md`, `instructions/FEATURE-admin-banques-organismes.md` et `instructions/FEATURE-admin-personnels.md`.

## lister

La recherche est un « contient » sans casse ; `%`, `_` et `[` y sont pris à la lettre ; vide ou faite d'espaces = pas de filtre. Colonnes cherchées : code, description, raison sociale, mobile, téléphone, e-mail, ICE ; banques et organismes : code, description, mobile, téléphones, fax, e-mail, site, notes, adresse ; personnels : les colonnes générales sans l'ICE. `natureId` n'est lu que pour les fournisseurs. `contacts` joint les libellés de `CfgTierContact` par « , ». Personnels : `rib` et `iban` toujours null en liste, lus sur la fiche.

## creer

Personne morale : `raisonSociale` requise. Personne physique : `prenom`, `nom`, `titreId` et `sexeId` requis, `raisonSociale` et `description` valent « prénom nom ». Toujours requis : `personneMorale`, `paysResidenceId`, `deviseId`, `formeSocialeId`, et `mobile` ou `email`. Un doublon n'est pas refusé.

Banques et organismes : fiche réduite, `code`, `description` et `paysId` seuls requis, compteur jamais utilisé. La description passe en majuscules et la raison sociale la suit, le pays de résidence suit `paysId`, `matriculeFiscal` s'écrit aussi dans `IDNumber`. La forme sociale vient du paramètre OMRAN de la famille, la devise reste vide. Seuls comptent `code`, `description`, `paysId`, `natureId`, `matriculeFiscal`, `registreCommerce`, `ice`, `compteComptableId`, `journalId`, les téléphones et leurs indicatifs, `fax`, `email`, `siteWeb`, les adresses, `codePostal`, `villeId`, `ville`, `notes` ; tout autre champ est ignoré.

Personnels : personne physique seule, `personneMorale` vrai = 400 ; `formeSocialeId` et `deviseId` facultatifs, contrôlés s'ils sont fournis. `ice`, `matriculeFiscal`, `registreCommerce`, `douane`, `codeComptable`, `codeComptableCollectif`, `tauxRetenue`, `tauxTva`, `delaiPaiement`, `assujettissementId` et `estPrestataire` sont ignorés.

## modifier

Mêmes contrôles qu'à la création. Le corps remplace la fiche : un champ omis est vidé. Le code ne change pas, sauf pour les banques et organismes, où il se modifie sous la même règle d'unicité ; leurs colonnes hors de la liste de `creer` restent telles quelles. Personnels : les champs ignorés à la création et les colonnes de paie restent tels quels.

## supprimer

Une clé étrangère restrictive (dossier, mouvement, contact lié à un autre tiers…) refuse la suppression en 409 sans rien modifier. Sinon les cascades de la base emportent les liens et les actions du tiers. Personnels : un employé lié à un compte (`AdmUser.CfgEmployeeId`) est refusé en 409.

## transformerEnContact

Le type passe de EMP à CNT, la nature est vidée, l'auteur et la date de modification sont posés ; le reste de la ligne ne change pas. La ligne quitte Personnels et entre dans Contacts. Refus 409, rien modifié : employé lié à un compte, ou commercial (`ComFolder.CfgCommercialId`) d'un dossier non sorti, y compris sans statut. Être le commercial d'un client ne bloque pas.
