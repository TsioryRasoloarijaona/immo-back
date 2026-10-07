# ActiviteProspectionDto

Indicateurs d'activité d'une prospection, calculés par la base légataire (`ComSaleProspectionView`)
et non stockés.

**Le bloc entier vaut `null` sur une pré-prospection** : la vue qui le produit exclut par
construction les 3 817 pré-prospections, qui ont la leur. Afficher « 0 interaction » dans ce cas
serait faux — il faut afficher « non disponible ».

## joursInactivite

Nombre de jours écoulés depuis la dernière action. C'est le critère de tri des relances du logiciel
de bureau. Renseigné sur 100 % des prospections de vente.

## cin

CIN du prospect, telle que la porte la vue. **Rare : 2,7 %** (387 lignes sur 14 118). Une mesure
initiale annonçait 99 %, en comptant les non-`NULL` d'une colonne qui porte des chaînes vides — à
afficher conditionnellement, jamais comme une donnée d'identité fiable.

## dossierId

Identifiant du dossier de vente issu de cette prospection — **le lien de conversion**, jusqu'ici
absent de l'API. `null` tant que la prospection n'a pas été transformée, ce qui est le cas de 98 %
d'entre elles : c'est le taux de conversion réel (358 dossiers pour 14 118 prospections), pas une
donnée manquante.
