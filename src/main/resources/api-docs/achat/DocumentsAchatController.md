# DocumentsAchatController

Pièces d'achat (`BuyDocument`) en **lecture seule** : factures, bons de commande, devis, documents à régler, bons de livraison, avoirs. Voir `instructions/FEATURE-achat-lecture-factures.md`.

**Visibilité, comme OMRAN** : une pièce est visible si et seulement si son projet (`PrjProjectId`) figure dans les habilitations projet de l'appelant (`AdmUserHabilitation.PrjProjectId`), **pour tous les rôles** (aucune exemption Administrateur / Direction générale). Une pièce sans projet n'est jamais visible. Une pièce hors périmètre répond 404, comme un id inconnu.

**Solde** : `montantRegle` = somme des lignes de règlement (`StlSettlementLine.Amount`) de la pièce dont le règlement n'est pas annulé (`CANCELED`) ; `resteARegler` = `montantAPayer` (`AmountToPay`) − réglé, à 2 décimales ; `solde` = |reste| ≤ 0,05 ; `aDeduire` = à payer < −0,05 (récupération d'avance sur décompte, jamais « trop réglée ») ; `tropRegle` = reste < −0,05 sur une pièce qui n'est pas à déduire. « Payé » ne se lit jamais dans le statut. Calcul unique : `SoldeDocumentAchat` (FEATURE-achat-impayes.md §3.1).

## liste

Pièces visibles, filtrées. `projetIds` est restreint au périmètre (un projet hors périmètre est ignoré, jamais d'erreur ; s'il ne reste rien, la page est vide). `nonSoldes=true` garde les pièces dont le reste dépasse 0,05 ; une pièce trop réglée ou à déduire n'y figure pas. `okPourPaiement=true` garde les pièces dont le statut est « OK pour paiement » ; avec `natures=INVOICE&natures=DOCUMENT_TO_PAY&nonSoldes=true`, c'est la liste « Validées, non réglées ». `search` cherche, sans tenir compte de la casse, dans le code, la référence, la description et le nom du fournisseur. Tri par défaut : `dateDocument` décroissante puis `code` décroissant. Propriétés de tri acceptées : `code`, `nature`, `dateDocument`, `dateEcheance`, `reference`, `fournisseurNom`, `projetCode`, `lotCode`, `statutCode`, `montantTTC`, `montantAPayer`, `montantRegle`, `resteARegler` ; une autre propriété répond 400.

## totaux

Totaux de la liste pour les mêmes filtres (même requête, toutes pages ; `sort`, `page` et `size` ignorés) : `nombre` = `totalElements` de la liste, `resteARegler` = Σ à payer − Σ réglé (signé), `resteDu` = Σ des seuls restes supérieurs à 0,05 et `nombreARegler` leur nombre. Aucun projet visible : tout à zéro.

## soldes

Solde de chaque fournisseur, une ligne par (fournisseur, société), sur les factures et documents à régler (avoirs exclus) **des projets visibles de l'appelant** (habilitations projet, aucune exemption de rôle) : deux utilisateurs aux projets différents voient deux soldes différents pour le même fournisseur. Les règlements non affectés à une pièce ne sont pas comptés. À payer = `AmountToPay`, réglé = lignes de règlement non annulées, reste = différence (calcul `SoldeDocumentAchat`). `projetIds` hors périmètre est ignoré ; `search` compare le nom du fournisseur sans espaces ni casse ; `nonSoldes=true` masque les lignes soldées (|reste| ≤ 0,05). Non paginé, trié par nom de fournisseur puis code société ; `sort` est ignoré. `total` somme les lignes rendues.

## exporterSoldes

Mêmes filtres et mêmes lignes que `GET /api/achat/fournisseurs/soldes`, sans la ligne de total.

## fiche

Fiche d'une pièce visible : les champs de la ligne de liste (mêmes calculs), l'en-tête (notes, régularisation, timbre, entité, audit), les lignes (`Sorting`), la ventilation (taux par catégorie), l'historique des statuts (date croissante) et les règlements, annulés compris avec `annule = true` (exclus du réglé).
