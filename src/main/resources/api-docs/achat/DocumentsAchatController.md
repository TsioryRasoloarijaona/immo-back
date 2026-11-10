# DocumentsAchatController

Pièces d'achat (`BuyDocument`) en **lecture seule** : factures, bons de commande, devis, documents à régler, bons de livraison, avoirs. Voir `instructions/FEATURE-achat-lecture-factures.md`.

**Visibilité, comme OMRAN** : une pièce est visible si et seulement si son projet (`PrjProjectId`) figure dans les habilitations projet de l'appelant (`AdmUserHabilitation.PrjProjectId`), **pour tous les rôles** (aucune exemption Administrateur / Direction générale). Une pièce sans projet n'est jamais visible. Une pièce hors périmètre répond 404, comme un id inconnu.

**Solde** : `montantRegle` = somme des lignes de règlement (`StlSettlementLine.Amount`) de la pièce dont le règlement n'est pas annulé (`CANCELED`) ; `resteARegler` = `montantAPayer` (`AmountToPay`) − réglé, à 2 décimales ; `solde` = |reste| ≤ 0,05 ; `tropRegle` = reste < −0,05. « Payé » ne se lit jamais dans le statut.

## liste

Pièces visibles, filtrées. `projetIds` est restreint au périmètre (un projet hors périmètre est ignoré, jamais d'erreur ; s'il ne reste rien, la page est vide). `nonSoldes=true` garde les pièces dont le reste dépasse 0,05 ; une pièce trop réglée n'y figure pas. `search` cherche, sans tenir compte de la casse, dans le code, la référence, la description et le nom du fournisseur. Tri par défaut : `dateDocument` décroissante puis `code` décroissant. Propriétés de tri acceptées : `code`, `nature`, `dateDocument`, `dateEcheance`, `reference`, `fournisseurNom`, `projetCode`, `lotCode`, `statutCode`, `montantTTC`, `montantAPayer`, `montantRegle`, `resteARegler`, `decompteCode` ; une autre propriété répond 400.

**Décompte lié** (`decompteId`, `decompteCode`, `decompteNumero`) : le décompte de chantier dont `PrjWorkingOut.BuyDocumentId` pointe la pièce (le premier par date puis code si plusieurs), `null` sinon. `BuyDocument.PrjWorkingOutId` (sens des avances) n'est jamais lu.

## fiche

Fiche d'une pièce visible : les champs de la ligne de liste (mêmes calculs), l'en-tête (notes, régularisation, timbre, entité, audit), les lignes (`Sorting`), la ventilation (taux par catégorie), l'historique des statuts (date croissante) et les règlements, annulés compris avec `annule = true` (exclus du réglé).
