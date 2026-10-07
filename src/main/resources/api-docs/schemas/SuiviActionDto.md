# SuiviActionDto

Une action commerciale (appel, visite, message, note…) telle que l'affiche l'écran OMRAN « Suivi des
actions ». Seules les colonnes demandées par `?colonnes=` sont présentes — catalogue : `GET
/api/referentiels/colonnes/suivi-actions`.

## utilisateur

Auteur de l'action tel qu'écrit sur la ligne (`UserName`) — la colonne sur laquelle filtre l'écran
OMRAN. « Système » ou vide pour une action générée.

## rang

Rang de l'action parmi celles du même type pour le même tiers, **1 = la plus récente** (hors actions
système et impayées) — la règle de la vue OMRAN. Calculé pour la page servie : ni triable ni
groupable.

## alerteActive

L'alerte de cette action est-elle encore active (`AdmAlertData.Active`) ? `false` si aucune alerte.
Calculé pour la page servie.

## systeme

Action générée par le système (`IsSystem`) — relance, événement… Listée comme dans OMRAN ; filtrable
par `?systeme=false`.

## commercial

Commercial concerné (`CfgEmployeeId` → `CfgTier.Description`) — pas l'auteur, qui est `utilisateur`.
Vide sur 23 % des actions réelles.

## personnel

Personnel : employé du compte auteur, sinon le compte, sinon le commercial — règle de la vue OMRAN.
Calculé pour la page servie.

## tiersNom

Tiers (prospect/client) de l'action : `CfgTier.Description` du tiers posé sur l'action, à défaut la
fiche prospect de la prospection rattachée, à défaut le nom saisi au premier contact
(`TempDescription`, cas des pré-prospections). Premier texte non blanc, nettoyé ; `null` si aucun.
Le groupement (`groupBy=tiersNom`) et son dépliage suivent la même cascade. `tiersId` reste l'id du
seul tiers de l'action.

## etat

État : statut du dossier de l'action, sinon du dossier né de sa prospection, sinon sa catégorie —
règle de la vue OMRAN. Calculé pour la page servie.
