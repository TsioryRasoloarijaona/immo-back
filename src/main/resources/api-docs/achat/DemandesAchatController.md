# DemandesAchatController

## lister

Droit `Yes.Buy.Front.CallOfTender.BuySelectionList` (sinon 403). Visibles : société habilitée ET (projet habilité OU sans projet) ET statut visible de l'appelant (aucune restriction pour un compte dont un rôle est libre). Une demande dont la procédure est confidentielle est absente sans le droit `…BuySelectionList.IsConfidential`. `champDate` : `DocDate`, `LaunchOfTenderDate`, `WithdrawalTenderDocumentsDate`, `TenderMaturityDate`, `TendersResortDate`, `BidOpeningDate`, `TendersReportDate`, `TenderDate`, `ContractSignatureDate` (colonnes de la demande), `ServiceOrderDate`, `WorkLaunchDate`, `WorkCompletionDate`, `ReattachmentDate`, `CancellationDate` (colonnes du lot).

## exporter

Droits `…BuySelectionList` et `…BuySelectionList.toolExport` ; mêmes lignes que la liste.

## fiche

Ordre des refus : 404 (inconnue ou hors périmètre) puis 403 (droit de lecture). Une demande confidentielle reste lisible sans le droit `IsConfidential` (`confidentielle = true` : le front masque les onglets Présélection, Offres, Modalités, BC, Autres).

## creer

Droit `…BuySelectionList.toolNew`. Code `yy/nnnnn` (plus grand numéro de l'année + 1) ; entité = celle de la procédure ; statut = premier statut de type Nouveau (tri `Sorting`) de l'entité visible de l'appelant ; dernière de son groupe ; déclencheur = employé du compte ; module Achat par défaut ; ligne d'historique « Création ». Le corps ne fixe jamais le code, le statut ni l'entité.

### reponse 400

Procédure, société ou description absente ; référence inconnue ; société ou projet non habilité ; procédure sans entité fonctionnelle (« Aucun Statut n'est sélectionné ou vous n'avez pas les droits sur les états de création de l'entité en cours ! ») ; montant estimé TTC refusé par la procédure (« La procédure d'achat selectionnée ne gère pas le montant estimé pour cette demande d'achat »).

### reponse 403

Droit `toolNew` manquant, ou aucun statut de type Nouveau de l'entité visible de l'appelant (« Vous n'avez pas les droits sur les états de création de l'entité '<entité>' ! »).

## modifier

Ordre des refus : 404 → 403 (`toolNew`) → 400 (mêmes règles que la création) → 409. Changer de procédure n'est permis qu'en statut de type Nouveau ; si l'entité de la procédure change, le statut revient au statut de départ de la nouvelle entité (sans historique).

### reponse 400

Mêmes cas que la création.

### reponse 409

Demande qui n'est pas la dernière de son groupe (lecture seule), ou procédure changée hors d'un statut de type Nouveau.

## supprimer

Ordre des refus : 404 → 403 (`toolDelete`) → 409. Supprime aussi les lignes filles (bordereau, lignes de stock, groupes, modalités de paiement, fournisseurs). Sans effet sur les prévisions du lot.

### reponse 409

Demande qui n'est pas la dernière de son groupe ; statut sans suppression permise (`CanDelete = 0`) pour un compte restreint (« Vous ne pouvez pas supprimer cette entité dans le statut : [<statut>] ! ») ; pièce d'achat liée.

## transitions

Compte restreint : transitions qui partent du statut stocké, ayant un intervenant sans mot de passe qui est un de ses rôles ou lui-même. Compte libre (un rôle sans restriction de séquence) : toutes les transitions de l'entité, une par statut d'arrivée (la première par ordre puis code), sauf vers le statut courant.

## executerTransition

Même calcul que la liste des transitions. Ordre des refus : 404 → 403 (lecture) → 400 `transitionId` absent → 409 (lecture seule, transition non proposée) → 400 / 403 second valideur → 400 commentaire → 409 (clôture avec succès, ci-dessous). Arrivée de type Terminé avec succès sur une demande à lot non multi-fournisseur : le bordereau définitif du lot est remplacé par celui de la demande (groupes gardant leurs ids, lignes hors stock), prix unitaire HT de chaque ligne = celui après remise de la ligne de même code de l'offre sélectionnée, aussi reporté sur la demande ; le lot reçoit le fournisseur, le mode et le délai de paiement et les 8 dates d'appel d'offre de la demande. Historique : `<transition>: <départ de la transition>=><arrivée>(<login> & <login du second>)` ; notes conservées seulement si la transition exige un commentaire ou une date. Mot de passe du second jamais conservé ni renvoyé.

### reponse 400

`transitionId` absent ; second valideur absent ou identifiants Arpent faux, compte désactivé ou sans identifiant web (« Login ou mot de passe incorrecte ! ») ; commentaire exigé vide (« Veuillez saisir un commentaire obligatoirement ! »).

### reponse 403

Droit de lecture manquant ; second valideur identique à l'appelant (« Le 2ème utilisateur doit être différent de l'utilisateur connecté ! ») ou non habilité sur la transition (« Le 2ème utilisateur n'a pas les habilitations requises pour cette action ! »).

### reponse 409

Transition non proposée à l'appelant (le corps porte `transitionsAutorisees`), demande non dernière ; clôture avec succès sans offre sélectionnée (« Aucune offre n'est sélectionnée ! »), code de ligne présent d'un seul côté entre le bordereau et l'offre (« Il y a une erreur au niveaux du bordereau de dépouillement ! »), ligne ou groupe du lot cité par un décompte.

## historique

Droit de lecture ; mêmes refus que la fiche.
