# CreationPieceAchatController

## modele

Valeurs proposées : date du jour, module Achat, type de marché `DefaultMarketType`, déclencheur = employé du compte, devise `DefaultCurrency`, statut de départ du workflow de la gestion de création (`BuyQuote`, `BuyOrder`, `BuyDelivery`, `BuyInvoiceDebit`), TVA par défaut `DefaultTva`. Aucune écriture, aucun numéro réservé.

### reponse 500

Configuration absente : gestion, module ou devise par défaut introuvable, ou workflow sans statut de départ de type « Nouveau ».

## creer

Ordre des refus : 400 nature non prise en charge → 404 société ou projet non habilité → 403 droit de création (code Achat « Nouveau » ; le code Projet › Lot compte pour une pièce à lot, jamais pour une facture) → 400 champs. Écrit la pièce, ses lignes, la ventilation des catégories (calculée depuis les lignes) et des projets, le numéro de la séquence de la nature, le statut de départ (null sans workflow) et, sauf pour un bon de livraison, une ligne d'historique. Montants sans arrondi : PU net = PU × (1 − remise), PU HT = PU net × (1 + FODEC), total HT = PU HT × quantité, TVA = total HT × taux. Une facture à lot reprend les taux de retenue du lot et le reste d'avance.

### reponse 400

Nature absente ou non prise en charge ; champ obligatoire absent (`dateDocument`, `societeId`, `moduleId`, `fournisseurId`, `typeMarcheId`), module ou type de marché inconnu ; ligne sans désignation, quantité, prix ou catégorie, ligne portant un `id` à la création, taux hors [0 ; 1] ; lot hors du projet ; ventilation des catégories ou des projets incorrecte (textes d'OMRAN).

### reponse 404

`Société introuvable` ou `Projet introuvable` (hors habilitations du compte, ou projet d'une ventilation non habilité).

### reponse 500

Configuration absente (gestion, séquence, devise, workflow sans départ « Nouveau »).

## modifier

La nature, le numéro, la gestion, le statut, les liens et les retenues ne changent jamais. Les lignes du corps remplacent celles de la pièce (id connu = mise à jour, null = nouvelle, absente = supprimée) ; un `declencheurId` null efface le déclencheur. Ordre des refus : 404 → 409 gérée ailleurs → 403 → 409 lecture seule → 400 → 409 quantités du bon de commande.

### reponse 400

Mêmes contrôles que la création ; id de ligne qui n'appartient pas à la pièce.

### reponse 404

`Pièce introuvable` (inconnue, hors périmètre, BC d'un autre déclencheur), `Société introuvable`, `Projet introuvable`.

### reponse 409

Pièce gérée ailleurs (document à régler, avance, frais de terrain, décompte, commission) ; statut en lecture seule (`Cette pièce est en lecture seule …`) ; facture rattachée à un bon de commande dont les quantités sont dépassées, si `LimitDeliveryAndInvoiceQuantityToOrder` est vrai (textes d'OMRAN).
