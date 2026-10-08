# PieceJointeDto

## category

Catégorie legacy brute de la pièce jointe (ex. "Yes.Com.Sales.Sale", "Dossier de vente N° ... du
..."). Purement informatif, sans logique métier dessus côté API.

## type

Type de document (ex. "PIECE D'IDENTITE", "CONTRAT", "FICHE PERMISSION REMISE DES CLES"...),
référentiel AdmAttachedFileType. Null si non renseigné en base (cas de certains dépôts anciens ou
faits hors de cette API).

## dateCreation

Date/heure de dépôt du fichier. Peut être absente (null) sur d'anciennes pièces jointes créées avant
le suivi de cette colonne dans les données legacy.

## creePar

Identifiant/login de l'utilisateur ayant déposé le fichier, tel qu'enregistré en base. Peut être
null si non renseigné (legacy) ou pour un dépôt effectué via cette API.
