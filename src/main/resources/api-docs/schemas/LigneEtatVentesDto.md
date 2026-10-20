# LigneEtatVentesDto

## prixVenteM2Ttc

Prix de vente au m² TTC du dossier, annexes comprises : même valeur que `prixVenteM2Ttc` de `GET /api/dossiers`. Null sur une ligne libre. Rapport : jamais sommé.

## prixVenteM2Ht

Prix de vente au m² HT du dossier, annexes comprises, comme `prixVenteM2Ht` de `GET /api/dossiers`. Null sur une ligne libre. Jamais sommé.

## netAPayer

« Net à payer » du dossier : prix + modifications + frais au crédit + pénalités (sur un dossier annulé ou résilié : réglé hors annulation − résiliation). Même calcul que `netAPayer` de `GET /api/dossiers`, pas la colonne stockée `TotalToPay`. Null sur une ligne libre.

## netAPayerRegle

Comme `netAPayerRegle` de `GET /api/dossiers`. Null sur une ligne libre.

## netAPayerReste

`netAPayer − netAPayerRegle`. Null sur une ligne libre.

## avance

Σ des échéances d'avance (`IsSaleAdvance`) au crédit, comme `avance` de `GET /api/dossiers`. Null sur une ligne libre.

## avanceReglee

Réglé des échéances d'avance, au crédit ou non. Null sur une ligne libre.

## avanceReste

`avance − avanceReglee`. Null sur une ligne libre.

## autofinancement

Σ des échéances d'autofinancement au crédit, comme la liste des dossiers. Null sur une ligne libre.

## autofinancementRegle

Réglé des échéances d'autofinancement. Null sur une ligne libre.

## autofinancementReste

`autofinancement − autofinancementRegle`. Null sur une ligne libre.

## credit

Σ des échéances de crédit bancaire au crédit, comme la liste des dossiers. Null sur une ligne libre.

## creditRegle

Réglé des échéances de crédit bancaire (décaissements en négatif). Null sur une ligne libre.

## creditReste

`credit − creditRegle`. Null sur une ligne libre.

## frais

Σ des échéances de frais au crédit (hors prix de vente), comme la liste des dossiers. Null sur une ligne libre.

## fraisRegles

Réglé des échéances de frais. Null sur une ligne libre.

## fraisReste

`frais − fraisRegles`. Null sur une ligne libre.

## modifications

`ComFolder.ModificationsAmount` (0 si absent). Null sur une ligne libre.

## modificationsReglees

Réglé des échéances de modification (`IsSaleModification`). Null sur une ligne libre.

## modificationsReste

`modifications − modificationsReglees`. Null sur une ligne libre.

## tropPercu

Ce que le client a payé en plus du net à payer, en positif ; 0 pour un dossier débiteur. Null sur une ligne libre.

## tropPercuRegle

Part du trop-perçu déjà remboursée. Null sur une ligne libre.

## tropPercuReste

Trop-perçu restant à rembourser. Null sur une ligne libre.

## penalites

Σ des échéances de pénalité (`IsSalePenality`), signées par la catégorie (au crédit +, sinon −). Null sur une ligne libre.

## penalitesRegles

Réglé des échéances de pénalité. Null sur une ligne libre.

## penalitesReste

`penalites − penalitesRegles`. Null sur une ligne libre.

## prixContractuelRegle

Réglé des échéances comprises dans le prix (`ExcludeFromPrice = 0`), comme la liste des dossiers. Null sur une ligne libre.

## prixContractuelReste

`ComFolder.AgreementPrice − prixContractuelRegle`. Null sur une ligne libre.

## tauxAvance

Avance en % du prix après remise (40 = 40 %), 0 si ce prix est nul. Null sur une ligne libre. Jamais sommé.

## tauxAvanceReel

Avance réglée en % de l'avance, 0 sans avance. Null sur une ligne libre. Jamais sommé.
