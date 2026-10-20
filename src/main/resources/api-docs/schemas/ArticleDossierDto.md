# ArticleDossierDto

## total

Vente : net à payer du dossier (`MontantsDossier`, même calcul que l'onglet Montants de la fiche dossier : prix de vente
+ frais exclus du prix ; sur un dossier annulé ou résilié, le déjà encaissé). Location et syndic : null (OMRAN lit ces
montants dans des vues non reprises).

## regle

Vente : total réglé du dossier (règlements signés, `MontantsDossier`). Location et syndic : null.

## reste

Vente : `total` − `regle`, 0 sur un dossier sorti. Location et syndic : null.
