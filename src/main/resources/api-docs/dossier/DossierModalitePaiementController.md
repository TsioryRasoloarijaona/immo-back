# DossierModalitePaiementController

Bouton « Modalité de paiement » de la fiche dossier. Aide OMRAN : « Lorsqu'on clic sur " Modalité de paiement ", le système affiche le schéma de financement initialement créé. Remarque : La création du schéma de financement est manuel. On peut copier le schéma de financement en cliquant sur le bouton " Copier schéma de financement ". » Les lignes (`ComDocumentPaymentMethod`) gardent date, catégorie, montant, HT, TVA, remise et société de chaque échéance. Une copie est aussi prise automatiquement au premier passage du dossier à un statut de vente effective (drapeau 001), s'il n'a encore aucune ligne (`Sale.cs:CreateDocumentPaymentMethod`).

## copierSchema

Supprime toutes les lignes du dossier puis recopie chaque échéance actuelle (`Sale.cs:UpdateDocumentPaymentMethod`).
