# ActionsPieceAchatController

## modifierStatut

Transitions candidates : celles de la **gestion de la pièce** (`StrEntityId`) qui partent du statut courant, triées par ordre (`Sorting`). Une transition est permise au compte si l'un de ses rôles (`AdmUserRole`) figure dans les rôles de la transition, **ou** si elle est validée par le déclencheur et que le déclencheur de la pièce est l'employé du compte. L'Administrateur n'a **aucune** exemption. Ordre des refus : 404 pièce → 400 `statutId` absent → 409 → 403 → 400 note vide (après `trim`) exigée par la transition, ou note de plus de 1000 caractères. Écrit le statut, l'auteur et la date de modification, et une ligne d'historique (note ou vide, transition retenue = la première autorisée par ordre). Une pièce à statut en lecture seule peut changer de statut. Les statuts acceptés sont exactement ceux de `actions.transitions` de la fiche.

### reponse 409

Aucune transition de la gestion ne mène du statut courant au statut demandé (pièce sans workflow et statut courant vide compris). Le corps porte `transitionsPermises` : les statuts d'arrivée permis au compte, même forme que `actions.transitions`.

## supprimer

Natures : devis, bon de commande, bon de livraison, facture, avoir. Ordre des refus : 404 → 403 → 409 (un document à régler, sans droit OMRAN de suppression, répond 409 dès après le 404). Supprime aussi les lignes, les ventilations catégorie et projet, l'historique des statuts et les liens `BuyOrderInvoice` côté facture ; vide `BuyQuoteId` des pièces issues d'un devis supprimé et `BuyInvoiceId` des pièces liées à une facture supprimée. Tout ou rien.

### reponse 409

Dans l'ordre : pièce gérée ailleurs (document à régler, pièce à `ReferenceType` — avance, frais de terrain —, facture de décompte) ; bon de commande ou facture à statut en lecture seule (texte d'OMRAN) ; règlement lié ; pièce fille (BL ou facture d'un BC, avoir d'une facture, lien `BuyOrderInvoice`) ; écriture comptable.

## affecterBonCommande

Écrit seulement `BuyOrderId` de la facture et l'auteur de modification ; aucune ligne `BuyOrderInvoice`. Réaffecter une facture déjà rattachée est permis ; le statut de la facture n'est pas regardé. Ordre des refus : 404 pièce → 409 nature → 403 → 400 → 404 bon de commande → 409.

### reponse 404

`Pièce introuvable` (facture inconnue, hors périmètre) ou `Bon de commande introuvable` (inconnu, pas un bon de commande, hors périmètre ou d'un autre déclencheur pour un compte restreint).

### reponse 409

Pièce qui n'est pas une facture ; facture gérée ailleurs ou à avance ; bon de commande d'un autre fournisseur, d'une autre société ou — la facture ayant un lot — d'un autre lot ; si le paramètre `LimitDeliveryAndInvoiceQuantityToOrder` est vrai, article absent du bon de commande ou quantité facturée au-delà de la quantité commandée (textes d'OMRAN).
