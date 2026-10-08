# PayeurDto

## role

Rôle sur ce dossier. `ACQUEREUR_PRINCIPAL` (`ComFolder.CfgCustomerId`, au plus un) ou `CO_ACQUEREUR`
(`ComFolderCustomer`).

## pieceIdentite

Pièce d'identité, au même format que sur l'« Autorisation d'encaissement » (`CIN: E400556`) — utile
pour distinguer deux homonymes dans une liste déroulante. `null` si ni le type ni le numéro ne sont
renseignés.

## quotePart

Quote-part du co-acquéreur en pourcentage (`ComFolderCustomer.PartPercent`). Toujours `null` pour
l'acquéreur principal, qui n'a pas de ligne `ComFolderCustomer`.
