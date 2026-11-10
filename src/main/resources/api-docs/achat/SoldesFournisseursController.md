# SoldesFournisseursController

## soldes

Solde de chaque fournisseur, une ligne par (fournisseur, société), sur les factures et documents à régler (avoirs exclus) **des projets visibles de l'appelant** (habilitations projet, aucune exemption de rôle) : deux utilisateurs aux projets différents voient deux soldes différents pour le même fournisseur. Les règlements non affectés à une pièce ne sont pas comptés. À payer = `AmountToPay`, réglé = lignes de règlement non annulées, reste = différence (calcul `SoldeDocumentAchat`). `projetIds` hors périmètre est ignoré ; `search` compare le nom du fournisseur sans espaces ni casse ; `nonSoldes=true` masque les lignes soldées (|reste| ≤ 0,05). Non paginé, trié par nom de fournisseur puis code société ; `sort` est ignoré. `total` somme les lignes rendues.

## exporter

Mêmes filtres et mêmes lignes que `GET /api/achat/fournisseurs/soldes`, sans la ligne de total.
