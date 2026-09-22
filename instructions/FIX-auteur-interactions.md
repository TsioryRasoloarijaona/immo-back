# FIX — L'auteur d'une interaction s'affichait « Système » au lieu de la personne connectée

## Symptôme

Dans OMRAN, ajouter une action sur une prospection retrace la personne connectée. Dans le CRM,
la même saisie s'affichait « faite par le Système ».

## Diagnostic (22/09/2026, mesuré sur le 115)

**L'écriture était déjà correcte.** `ProspectionService.addInteraction` pose l'auteur depuis le
jeton : `AdmUserId`, `UserName`, `UserCreate` = la personne connectée — exactement le motif
OMRAN (`UserName = UserCreate = AdmUser.Description` sur ses lignes). L'auth du CRM étant
fondée sur `AdmUser`, le jeton porte toujours l'identifiant.

**Le bug était à la lecture.** Les deux mappeurs d'`InteractionDto` (`ProspectionService.
toInteractionDto` et `HotlineService.toInteractionDto`) dérivaient `employeNom` du seul
`CfgEmployeeId`. Or, sur les **48 278** interactions réelles :

| Colonne | Remplie |
|---|---|
| `UserName` / `UserCreate` / `AdmUserId` | 48 276 (99,996 %) |
| `CfgEmployeeId` | 37 286 (77 %) |

Et les comptes créés par cette plateforme n'ont **jamais** d'employé lié. `employeNom` arrivait
donc `null`, et le frontend affichait son repli `employeNom || 'Système'` — y compris sur 23 %
des interactions saisies dans OMRAN lui-même.

## Correctif

`ProspectionService.nomAuteur(ComActionMessage)` — règle unique, partagée par les deux
mappeurs (ils avaient déjà divergé une fois) :

1. l'employé lié (`TierNomFormatter.nom(CfgEmployee)`), quand il existe ;
2. sinon `UserName` ;
3. sinon `UserCreate`.

`employeId` reste inchangé (le seul `CfgEmployeeId`) ; seule la sémantique d'`employeNom`
s'élargit : « nom d'affichage de l'auteur », toujours renseigné quand la base le connaît.

## Vérification

`ProspectionInteractionsTest` : interaction sans employé mais avec `UserName` →
`employeNom = "Khadija ELKORRI"` (et `employeId` absent) ; interaction avec employé →
l'employé reste prioritaire.

## Impact front

**Rien ne casse, mais deux améliorations deviennent possibles :**

- `employeNom` est désormais renseigné dès que la base connaît l'auteur — le repli
  `employeNom || 'Système'` (`ProspectionDetail.tsx`, `Hotline.tsx`) ne s'affichera plus que
  sur les 0,004 % de lignes réellement anonymes. Aucun changement requis.
- `employeId` peut être `null` alors qu'`employeNom` est renseigné : ne pas déduire « auteur
  inconnu » de l'absence d'`employeId`. Les `@Schema` des deux champs le disent.
