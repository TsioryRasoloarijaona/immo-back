# CreationPieceAchatController

## modele

Valeurs proposées : date du jour, module Achat, type de marché `DefaultMarketType`, déclencheur = employé du compte, devise `DefaultCurrency`, statut de départ du workflow de la gestion de création (`BuyQuote`, `BuyOrder`, `BuyDelivery`, `BuyInvoiceDebit`), TVA par défaut `DefaultTva`. Aucune écriture, aucun numéro réservé.

Avec `sources` (génération) : bon de commande ← 1 devis ; bon de livraison ← 1 bon de commande validé ; facture ← 1..n bons de livraison validés non facturés (même fournisseur, même bon de commande sauf `GenerateInvoiceFromManyOrders`) ; facture ← 1 bon de commande validé si `GenerateInvoiceFromOrder`. Le modèle reprend l'en-tête de la 1re source (société, projet, lot, fournisseur, module, déclencheur, type de marché, références, notes), sa devise et sa ventilation des projets ; `montantRegul` et `timbreFiscal` = sommes des sources ; date du jour, échéance recalculée pour une facture. Lignes de quantité non nulle, fusionnées sur (bordereau, catégorie, PU, remise, FODEC, TVA, code, désignation), quantités sommées ; chaque ligne garde son `ligneBordereauId`. Bon de livraison : quantité commandée moins le déjà livré par code sur tous les bons de livraison du bon de commande, lignes soldées omises. Gestion et statut de départ = ceux de la gestion d'arrivée de la source (`BuyOrder` → `BuyDelivery`, `PrjMarketMultiSupplierOrder` → `PrjMarketMultiSupplierDelivery`…). Ordre des refus : 404 source → 403 → 400 → 409.

Avoir (`REFUND`, jamais sans `sources`) ← exactement 1 facture de gestion `PrjInvoiceDebit` (gestion d'arrivée `BuyRefundDebit`). Le modèle reprend l'en-tête de la facture (société, fournisseur, module, devise, type de marché, lot, projet, bon de commande, notes), clone toutes ses lignes sans fusion, sa ventilation des projets ; `montantRegul` et `timbreFiscal` à 0. Facture sans ligne : `montantHT` / `montantTVA` de la facture et `categorieId` = sa première ventilation de catégorie. `avoirFinancier` = paramètre `DefaultRefundIsFinancial` (faux si absent). Droit : code « Nouveau » des avoirs (Achat, ou Projet › Lot pour une facture à lot).

### param sources

Ids des pièces sources (BuyDocument.PKey), séparés par des virgules, dans l'ordre (la 1re donne l'en-tête) ; absent = création libre.

### reponse 400

Nature absente ou non prise en charge (dont un avoir sans `sources`) ; `sources` sur une nature qui ne se génère pas (devis).

### reponse 404

Source inconnue, hors périmètre du compte ou bon de commande d'un autre déclencheur. Avoir : `La facture sélectionnée n'est pas présente dans notre référentiel !`.

### reponse 409

Génération refusée (textes d'OMRAN) : combinaison non prévue ; bon de commande ou bon de livraison non validé ; bons de livraison de fournisseurs ou de bons de commande différents ; bon de livraison déjà facturé ; facture depuis un bon de commande désactivée (`GenerateInvoiceFromOrder`) ; `Toutes les quantités de ce bon de commande sont déjà livrées.` ; `Génération non configurée pour la gestion <code>` (gestion source sans gestion d'arrivée).

### reponse 500

Configuration absente : gestion, module ou devise par défaut introuvable, ou workflow sans statut de départ de type « Nouveau ».

## creer

Ordre des refus : 400 nature non prise en charge → 404 source, société ou projet non habilité → 403 droit de création (code Achat « Nouveau » ; le code Projet › Lot compte pour une pièce à lot, jamais pour une facture) → 400 champs → 409 génération. Écrit la pièce, ses lignes, la ventilation des catégories (calculée depuis les lignes) et des projets, le numéro de la séquence de la nature, le statut de départ (null sans workflow) et, sauf pour un bon de livraison, une ligne d'historique. Montants sans arrondi : PU net = PU × (1 − remise), PU HT = PU net × (1 + FODEC), total HT = PU HT × quantité, TVA = total HT × taux. Une facture à lot reprend le reste d'avance du lot ; seule une facture générée reprend aussi ses taux de retenue.

Avec `sources` : mêmes contrôles que le modèle, rejoués ; la pièce prend la gestion d'arrivée et la devise de la 1re source et écrit les liens (`BuyQuoteId` du bon de commande, `BuyOrderId`, `BuyInvoiceId` des bons de livraison facturés, une ligne `BuyOrderInvoice` par bon de commande distinct) ; un bon de livraison est contrôlé contre les quantités du bon de commande si `LimitDeliveryAndInvoiceQuantityToOrder`. Un refus ne consomme aucun numéro.

Avoir : `InvoiceSourceId` = la facture, `BuyOrderId` = son bon de commande, numéro de la séquence « Avoir fournisseur », `IsFinancialRefund` = `avoirFinancier` (paramètre `DefaultRefundIsFinancial` si absent). Sans ligne, `montantHT` + `montantTVA` fixent les totaux (TTC = somme) ; avec lignes ils sont ignorés. Le net à payer de la facture source est recalculé (TTC et retenues moins la somme de ses avoirs non financiers), à la création, à la modification et à la suppression de l'avoir.

### reponse 400

Nature absente ou non prise en charge (dont un avoir sans `sources`) ; champ obligatoire absent (`dateDocument`, `societeId`, `moduleId`, `fournisseurId`, `typeMarcheId`), module ou type de marché inconnu ; ligne sans désignation, quantité, prix ou catégorie, ligne portant un `id` à la création, taux hors [0 ; 1] ; `lignes[i].ligneBordereauId` absent des lignes des sources ; `sources` sur un devis ; lot hors du projet ; ventilation des catégories ou des projets incorrecte (textes d'OMRAN).

### reponse 404

`Société introuvable` ou `Projet introuvable` (hors habilitations du compte, ou projet d'une ventilation non habilité) ; source inconnue, hors périmètre ou bon de commande d'un autre déclencheur.

### reponse 409

Génération refusée (mêmes textes que le modèle) ; quantités du bon de commande dépassées par un bon de livraison ou une facture générés, si `LimitDeliveryAndInvoiceQuantityToOrder` est vrai (textes d'OMRAN).

### reponse 500

Configuration absente (gestion, séquence, devise, workflow sans départ « Nouveau »).

## modifier

La nature, le numéro, la gestion, le statut, les liens (dont `ligneBordereauId`) et les retenues ne changent jamais ; `sources` est ignoré. Les lignes du corps remplacent celles de la pièce (id connu = mise à jour, null = nouvelle, absente = supprimée) ; un `declencheurId` null efface le déclencheur. Ordre des refus : 404 → 409 gérée ailleurs → 403 → 409 lecture seule → 400 → 409 quantités du bon de commande.

Avoir : la facture source, le lot et le numéro ne changent jamais (`lotId` du corps ignoré) ; `avoirFinancier` null garde la valeur stockée ; sans ligne, `montantHT` + `montantTVA` fixent les totaux ; le net à payer de la facture source est recalculé.

### reponse 400

Mêmes contrôles que la création ; id de ligne qui n'appartient pas à la pièce.

### reponse 404

`Pièce introuvable` (inconnue, hors périmètre, BC d'un autre déclencheur), `Société introuvable`, `Projet introuvable`.

### reponse 409

Pièce gérée ailleurs (document à régler, avance, frais de terrain, décompte, commission) ; statut en lecture seule (`Cette pièce est en lecture seule …`) ; facture rattachée à un bon de commande dont les quantités sont dépassées, si `LimitDeliveryAndInvoiceQuantityToOrder` est vrai (textes d'OMRAN).
