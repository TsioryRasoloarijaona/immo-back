# InteractionDto

## note

**Le texte saisi par le commercial — c'est ce qu'il faut afficher dans le journal.** Stocké dans
`ComActionMessage.AlertMessage`, qui malgré son nom ne relève pas des relances : 6 502 interactions
portent une note sans aucune date d'alerte. `null` quand rien n'a été saisi (3,4 % des lignes
réelles).

## description

**Libellé généré, pas un contenu.** Vaut `JJ/MM/AAAA HH:MM <auteur>` sur 100 % des lignes réelles
(46 546 sur 46 546, zéro exception), c'est-à-dire exactement `date` + `employeNom`, déjà présents
ci-dessus. Conservé pour ne rien casser, mais **l'afficher revient à répéter trois fois la même
information** — utilisez `note`.

## employeId

Id de l'employé lié (`CfgEmployeeId`). Renseigné sur 77 % des interactions réelles seulement —
`null` ne veut pas dire auteur inconnu, voir `employeNom`.

## employeNom

**Nom d'affichage de l'auteur — toujours renseigné quand la base le connaît (99,996 % des lignes
réelles).** L'employé lié quand il existe, sinon le compte qui a saisi l'interaction (`UserName`,
puis `UserCreate`) — c'est ce que le logiciel de bureau affiche. Ne PAS afficher de repli type «
Système » côté client sur la seule absence d'`employeId`.

## dateRdv

Date du rendez-vous, présente uniquement quand resultat est "Rendez vous pris" ou "RDV reporté" et
qu'une date a été fournie à la création. null sinon.

## categorieId

Id de la **catégorie** de l'interaction — à ne pas confondre avec `resultatId`, qui est la
sous-catégorie. Renseignée sur 100 % des 48 160 interactions réelles.

## categorie

Libellé de la catégorie. Sert de discriminant : `Prospection` (46 666 lignes), `Dossier de vente
(Réservation)` (1 325), `Dossier de vente (Livraison)` (167), `Réclamation` (2).

## trancheId

Id de la tranche de rattachement. Déduite comme le fait le logiciel de bureau : la tranche de la
**prospection** quand `categorie` vaut "Prospection", celle du **lot** sinon. Renseignée sur 96 %
des interactions ; `null` quand ni l'une ni l'autre n'en porte.
