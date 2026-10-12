# GroupeActionnairesController

## creer

Réservé au rôle `Administrateur`. Obligatoires : `code` (unique sur toute la table, casse ignorée), `description`,
et des `lignes` dont les pourcentages (fractions : `0.4` = 40 %) font **exactement** 100 % — un groupe sans ligne
est donc refusé, comme `CfgGroupShareholder.Verif` d'OMRAN.

### reponse 400

Code ou description manquant ; sur `champ = lignes` : actionnaire absent ou inconnu, pourcentage absent ou hors
[0 ; 1], actionnaire en double (« L'actionnaire existe déjà dans la liste !!! »), somme différente de 100 % (« La
somme des pourcentages doit être égale à 100 % ! »).

## modifier

Champs absents inchangés. `lignes` présentes **remplacent** toutes les lignes du groupe, avec les mêmes contrôles
qu'à la création. Les entités juridiques déjà réparties depuis ce groupe ne sont pas recalculées (OMRAN ne le fait
pas non plus) : relancer `POST /api/entites-juridiques/{id}/repartition/depuis-groupe/{groupeId}`.

### reponse 400

Mêmes causes qu'à la création.

## supprimer

Refusée (409) tant qu'une entité juridique désigne ce groupe : en production la clé étrangère
`CfgCompany.CfgGroupShareholderId` est `ON DELETE CASCADE`, la base supprimerait l'entité avec le groupe. Le détail
liste les codes des entités concernées.
