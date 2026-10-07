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
