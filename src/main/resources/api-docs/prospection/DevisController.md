# DevisController

Offres de prix ("devis") faites aux prospects — vue globale tous prospections confondues. Voir aussi POST/GET /api/prospections/{id}/devis pour la vue nestée sur une seule prospection.

**Entités juridiques en exploitation.** Ne sont renvoyés que les devis dont la tranche relève d'une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Restriction globale et permanente, appliquée à tout appelant. Un devis sans tranche reste visible.

## findAll

Filtre optionnellement par recherche libre sur le code de l'offre, tranche(s), commercial, et plage
de dates sur la date de l'offre (dateDocument). Chaque ligne porte prospectionId/prospectionCode
pour la colonne "Prospection" et le lien cliquable vers la prospection d'origine.
sommePrixVenteTTCPage est calculé uniquement sur la page renvoyée, pas sur l'ensemble des résultats
filtrés — voir la description du champ.

**Périmètre commercial** : si l'appelant est restreint (`UtilisateurCourantDto.perimetreRestreint =
true`), seules les offres portant une tranche d'un de ses projets affectés sont renvoyées (plus
celles sans tranche, dont le projet ne se résout pas). Ce filtre s'ajoute aux filtres ci-dessus :
demander explicitement une `trancheId` hors périmètre renvoie une page vide, pas une erreur.
`sommePrixVenteTTCPage` porte donc sur les seules lignes visibles.

### param trancheId

Id(s) de tranche (CfgTranche.id). Répéter le paramètre pour plusieurs tranches
(?trancheId=a&trancheId=b). Absent = toutes les tranches.

## findById

Même visibilité que la liste (`GET /api/devis`) : une offre que l'appelant ne verrait pas dans la liste
répond 404, comme une offre inconnue. `offre.schemaFinancement` est le schéma saisi dans OMRAN s'il
existe (`schemaFinancementSaisi = true`), sinon le schéma calculé depuis la tranche.

## modifier

Mêmes champs que la création, tous optionnels : seul ce qui est fourni change (null = inchangé).
Mêmes contrôles qu'à la création (article de la tranche et non vendu, sous-articles enfants de
l'article, prix requis sans article) ; l'article déjà porté par l'offre n'est pas recontrôlé comme
vendu. Les montants sont recalculés par le calcul de la création quand l'article, les sous-articles,
le prix ou la remise sont fournis. Si l'article et les sous-articles ne changent pas, la base est
celle figée dans l'offre (prix catalogue ignorés) et seule la remise des sous-articles est réappliquée ;
sinon les sous-articles sont réécrits aux prix du catalogue, comme à la création. `UserUpdate` /
`UserUpdateDateTime` = l'utilisateur courant. Une offre qui a donné un dossier reste modifiable,
comme dans OMRAN, mais ne peut plus changer d'article.

Même périmètre que la liste : quiconque voit l'offre peut la modifier.

### reponse 400

Tranche, article, sous-article ou mode de paiement introuvable ou incohérent ; changement d'article
d'une offre qui a donné un dossier ; schéma de financement saisi dans OMRAN qui ne correspond plus au
prix de vente TTC après remise (`VerifSchema` d'OMRAN : AVANCE + CRÉDIT + AUTO-FINANCEMENT = prix,
au centime). Rien n'est écrit. Le message est dans `detail`.

## supprimer

Supprime l'offre avec ses sous-articles (`StlQuoteItem`) et son schéma (`StlQuoteDocument`), comme
la cascade de la base. Le numéro d'offre (`AdmSequence`) n'est pas rendu. Même périmètre que la liste.

### reponse 409

L'offre a donné un dossier (vente : `ComFolder.StlQuoteId`, ou location : `RntFolder.StlQuoteId`) :
elle ne peut pas être supprimée. Vérifié avant toute écriture ; le message nomme le dossier.
