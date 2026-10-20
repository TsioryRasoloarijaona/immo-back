# FraisTerrainController

Onglet « Coûts » de la fiche terrain : frais `StkExpense`. Le coût total du terrain en est la somme, calculée à la
lecture de la fiche. « Valider pour règlement » crée la facture fournisseur du frais (module Achat d'OMRAN) ; son
paiement reste hors du CRM.

## lire

Lignes du terrain, par date ; `factureId` / `factureCode` renseignés pour un frais facturé.

## enregistrer

Liste complète : `id` connu = mise à jour, absent = création, ligne absente du corps = suppression. Obligatoires
(400) : `date`, `montant` > 0, `typeFraisId` ; `notes` 1000 caractères au plus. `fournisseurId` absent = propriétaire
du terrain ; absent des deux → 400 « Renseignez le fournisseur du frais ou du terrain ». Un frais validé renvoyé
inchangé est laissé tel quel ; modifié ou retiré de la liste → 409, rien n'est écrit.

## valider

Une seule transaction : facture `BuyDocument` (`INVOICE`, `LAND_EXPENSE`) avec une ligne, une ventilation à 100 % sur
la catégorie comptable du type de frais et un historique de statut, code tiré de la séquence « Facture fournisseur »,
frais marqué validé. Montant HT = TTC = à payer = montant du frais, sans arrondi ni TVA ; date du jour, échéance =
date du jour + délai de paiement du fournisseur ; devise par défaut (`AdmParamsGuid DefaultCurrency`), statut de
départ du workflow de l'entité « Facture frais sur terrain ». Refus avant toute écriture : 404 (terrain masqué, frais
d'un autre terrain), 409 « Vous ne pouvez pas ajouter une facture pour un frais qui est déjà validé ! » (déjà validé
ou déjà facturé), 400 (fournisseur, type de frais sans catégorie comptable ou type de marché), 500 (module STL,
entité, devise, workflow ou séquence absents).
