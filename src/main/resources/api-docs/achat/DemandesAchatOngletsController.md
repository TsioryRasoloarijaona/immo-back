# DemandesAchatOngletsController

## fournisseurs

Droit `Yes.Buy.Front.CallOfTender.BuySelectionList` ; une procédure confidentielle exige aussi `…BuySelectionList.IsConfidential` (sinon 403). Ordre des refus : 404 puis 403.

## ajouterFournisseur

Ordre des refus : 404 → 403 (`…BuySelectionList.toolAddSupplier`) → 400 (`fournisseurId` absent ou inconnu, texte trop long) → 409.

### reponse 409

Demande qui n'est pas la dernière de son groupe ; statut hors de la phase présélection (`BoolField002`) ; fournisseur déjà présent (« Ce fournisseur est déja ajouté »).

## modifierFournisseur

Mêmes droits et refus que l'ajout ; le fournisseur peut être changé s'il n'est pas déjà présent. 404 aussi si `fid` n'appartient pas à la demande.

## supprimerFournisseur

Ordre des refus : 404 → 403 (`…toolDeleteSupplier`) → 409 : demande non dernière, hors phase présélection, ou fournisseur ayant au moins une offre.

## ecarterFournisseur

Ordre des refus : 404 → 403 (`…toolSelectDeselectSupplier`) → 409 : demande non dernière, hors phase présélection, ou fournisseur de l'offre sélectionnée. Écarte aussi toutes ses offres (`IsRejected`).

## reactiverFournisseur

Mêmes droits et refus que l'écart (sans le cas de l'offre sélectionnée) ; réactive aussi toutes ses offres.

## offres

Mêmes droits que la liste des fournisseurs (confidentialité comprise). Totaux calculés sur les lignes, prix après remise.

## creerOffre

Ordre des refus : 404 → 403 (`…toolAddQuote`) → 400 (`fournisseurId` absent ou non présélectionné) → 409 : demande non dernière, hors phase offres (`BoolField003`), fournisseur écarté, date de dépôt vide, demande sans projet. Crée un devis (gestion `BuyQuote` sans lot, `PrjMarketQuote` lot simple, `PrjMarketMultiSupplierQuote` lot multi-fournisseur), sans statut, daté et échu au dépôt, société, projet, lot, module, type de lot et devise de la demande ; une ligne par ligne du bordereau (ordre, code, désignation, unité, quantité, TVA, FODEC, catégorie), prix à 0.

## modifierOffre

Le corps liste les lignes à changer (`ligneId` = ligne de ce devis). Ordre des refus : 404 (demande, ou devis d'une autre demande) → 403 (`…toolAddQuote`) → 400 (ligne inconnue, prix négatif, remise hors de 0..1) → 409 (demande non dernière, hors phase offres, offre écartée). Totaux du devis recalculés.

## supprimerOffre

Ordre des refus : 404 → 403 (`…toolDeleteQuote`) → 409 : demande non dernière, hors phase offres, offre sélectionnée.

## selectionnerOffre

Ordre des refus : 404 → 403 (`…toolSelectDeselectQuote`) → 409 : demande non dernière, hors phase sélection (`BoolField004`), demande sans lot ou à lot multi-fournisseur, fournisseur écarté, lot ayant un décompte. Seule cette offre reste sélectionnée ; fournisseur de la demande = celui de l'offre ; délai de paiement = celui du fournisseur si celui de la demande est nul ou négatif.

## deselectionnerOffres

Mêmes droits et refus que la sélection (sans le fournisseur écarté). Toutes les offres désélectionnées, bordereau de la demande vidé (groupes et lignes), fournisseur de la demande retiré ; le bordereau du lot reste intact.

## comparaison

Mêmes droits que la liste des offres ; ouverte en phase offres ou sélection (sinon 409). Rend les offres non écartées.

## modalites

Mêmes droits que la liste des fournisseurs (confidentialité comprise).

## ajouterModalite

Ordre des refus : 404 → 403 (`…toolNew`, droit d'enregistrer la fiche) → 400 (`ordre` ou `code` absent) → 409 (demande non dernière).

## supprimerModalite

Ordre des refus : 404 (demande, ou modalité d'une autre demande) → 403 (`…toolNew`) → 409 (demande non dernière).

## bordereau

Droit de lecture de la demande. Taux de TVA et de FODEC en pourcentage, comme le bordereau d'un lot.

## enregistrerBordereau

Le corps est le bordereau entier : groupes et lignes absents sont supprimés. Ordre des refus : 404 → 403 (`…toolNew`) → 409 (demande non dernière, hors phase bordereau `BoolField001`) → 400.

### reponse 400

Champ obligatoire absent ou hors bornes (mêmes règles que le bordereau d'un lot), id d'un autre bordereau, code de ligne en double (« Le code <code> doit être unique pour les lignes du bordereaux ! »).

## importerBordereau

Classeur `.xlsx` au format de l'export (colonnes Groupe, Code, Désignation, Unité, Quantité, TVA %, FODEC %, Catégorie, Notes) ; groupe et catégorie désignés par leur code. Mêmes droits, refus et effet que l'enregistrement du bordereau ; 400 aussi si le fichier est illisible ou si un code de groupe ou de catégorie est inconnu.

## exporterBordereau

Droit de lecture de la demande ; une ligne de classeur par ligne du bordereau, groupe désigné par son code.

## bonsCommande

Mêmes droits que la liste des fournisseurs (confidentialité comprise). Pièces d'achat dont `BuySelectionId` est la demande, sauf les devis. Le bon de commande depuis une offre se crée par la génération des pièces d'achat (`GET /api/achat/documents/modele?nature=ORDER&sources=<offre>`) ; `phases.bonCommande` de la fiche dit quand OMRAN l'autorise.

## renouveler

Ordre des refus : 404 → 403 (`…toolNew`) → 409 : demande non dernière, statut hors type Annulation (« Pour pouvoir générer un nouvel appel d'offre il faut d'abords annuler l'actuel ! »). Nouvelle demande du même groupe, dernière, au statut de départ de l'entité, nouveau code ; copie l'en-tête, le bordereau, les modalités et les fournisseurs non écartés ayant déposé (références et dates vidées) ; l'ancienne n'est plus la dernière.
