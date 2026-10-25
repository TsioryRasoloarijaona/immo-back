# PremieresRelancesController

Les trois routes partagent une seule sélection : dossiers de vente portés par un article **non sous-article** de la tranche
(`StkItem.ComFolderId`), dont le statut n'est ni une annulation (`BoolField002`) ni un désistement (`BoolField003`) ; un
dossier sans statut est exclu. Réservées à l'Administrateur (403 avant tout autre refus).

## apercu

Ne lit que la base. Échéance = `Revival1Date` de la tranche + `AgreementFirstDelay` jours (absent = 0 jour, signalé dans
`avertissement`). `alerte` est vrai quand l'échéance actuelle du dossier (`Revival1ValidityDate`) diffère de la nouvelle
**et** que le dossier a une prospection : c'est exactement ce que le POST créera.

## executer

En une transaction (une erreur n'écrit rien) : pour chaque dossier de l'aperçu, l'alerte système « Titre foncier:
Echéance de la notification de la 1ère relance du dossier de vente N° <code>, date échéance: <jj/mm/aaaa> » (type Note,
catégorie Réservation si le statut est celui du paramètre `DefaultComFolderStatusReservation`, Livraison sinon, + ligne
`AdmAlertData`) quand `alerte` est vrai, puis `Revival1` = date de relance et `Revival1ValidityDate` = échéance. Relancer
avec la même date ne recrée pas d'alerte.

## lettres

Une page par dossier de la sélection portant déjà `Revival1`, rendue par le modèle de document actif
`RELANCE_TITRE_FONCIER / PREMIERE_RELANCE` (texte modifiable dans les modèles de documents). Restes dus prix, frais
(échéances exclues du prix) et total : même calcul que l'onglet Montants du dossier, règlements annulés exclus. Refus :
tranche sans date de 1re relance (400), aucun dossier relancé (400 « Aucun dossier à imprimer »).
