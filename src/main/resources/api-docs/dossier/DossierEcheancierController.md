# DossierEcheancierController

Opérations du schéma de financement de la fiche dossier, comme OMRAN (aide « DOSSIERS DE VENTE », liste du schéma de financement) : « Si on ajoute (ou on augmente son montant) un document paramétré "Mettre à jour le schéma de financement" --> Le système soustrait son montant de la ligne "Autofinancement" sinon de la linge "Crédit". Si on supprime (on diminue son montant) un document paramétré " Mettre à jour le schéma de financement" --> Le système ajoute son montant à la ligne "Autofinancement" sinon à la ligne "Crédit". » Ce report s'applique aussi à `POST` et `PATCH /api/dossiers/{id}/echeancier`. Après chaque écriture, le prix contractuel, le montant d'accord et le net à payer du dossier sont recalculés et le dossier est signé.

## etat

Sommes signées par nature (une catégorie au débit se retranche), écart = prix après remise − (avance + crédit + autofinancement) au centime. `montantInitialPeriodicite` est l'autofinancement restant à régler, sinon prix − avance − crédit : c'est la valeur proposée et le plafond de `POST …/periodique`.

## supprimer

Refusée (409) si l'échéance a une ligne de règlement (même annulée), une ligne d'autorisation de versement, une ligne de facture, ou si elle est versée dans un dossier de recouvrement (`ComDocument.cs:VerifyLinksForDeletion`). Sinon ses actions et alertes sont supprimées, les lignes qui la désignaient comme pénalité sont détachées, puis l'écart est reporté si sa catégorie « met à jour le schéma ».

## genererPeriodique

Montant d'une période = montant total / nombre au centime ; la dernière ligne porte le reste, la somme est exacte. Les dates avancent d'un, deux, trois, six ou douze mois. L'écart est reporté depuis la dernière ligne, puis toute ligne du dossier ramenée à zéro et non réglée est retirée (`GeneratePeriodicity`). 400 si le montant dépasse `montantInitialPeriodicite`, ou si une ligne d'une catégorie « basée sur la date des finalisations » tombe avant le début des finalisations de la tranche.
