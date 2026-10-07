# ProspectionListItemDto

## fermee

Prospection archivée (`IsClosed`). **Seul indicateur fiable** : `dateFermeture`/`raisonFermeture`
survivent à un désarchivage. `null` équivaut à `false`. `?ouvertes=true` ne renvoie que
`false`/`null`, `?ouvertes=false` ne renvoie que `true`, l'absence du paramètre renvoie tout.

## prix

Budget déclaré du prospect. **Mesuré mort : renseigné sur 4 lignes sur 14 114**, et valant `0` —
jamais `null` — partout ailleurs. Retiré de la sélection par défaut pour cette raison ; reste
demandable via `?colonnes=prix`. Ne pas afficher comme un budget : ce serait annoncer « 0,00 DH »
sur tout le portefeuille.

## telephone

Téléphone du prospect : celui de sa fiche (mobile, à défaut fixe) et, à défaut, le numéro saisi lors
du premier contact. Le repli n'est pas décoratif — mesuré sur les 8 613 prospections ouvertes : 5
034 joignables par la fiche, 7 596 par le champ de premier contact, **8 591 par l'une ou l'autre**.

## email

Email du prospect, même règle de repli que `telephone`. Renseigné sur 4 752 des 8 613 prospections
ouvertes (55 %) — `null` est donc un cas courant, pas une anomalie.

## derniereAlerteDate

Date de validité de la dernière alerte de relance posée sur cette prospection, `null` s'il n'y en a
aucune (3 236 des 8 613 ouvertes en ont une). L'alerte provient toujours d'une **interaction** : la
colonne d'alerte portée par la prospection elle-même est morte en base — 0 ligne sur 8 613.

## derniereAlerteMessage

Message de cette dernière alerte — en pratique la note du commercial sur l'interaction qui l'a
posée. `null` si aucune alerte.

## joursInactivite

Jours écoulés depuis la dernière action — le tri de relance du logiciel de bureau. Renseigné sur 100
% des prospections de vente.

## dossierId

Identifiant du dossier issu de la prospection. `null` sur 98 % des lignes : c'est le taux de
conversion réel, pas une donnée manquante.

## mobileProspect

Mobile **brut de la fiche prospect**, sans le repli sur le numéro de premier contact qu'applique
`telephone`. 99,8 %. Utile pour éditer ou dédoublonner, là où `telephone` sert à appeler.

## societeId

Identifiant de la société / entité juridique. 99,8 %. Elle filtre déjà les résultats côté serveur
(seules les sociétés actives sont servies) sans avoir jamais été renvoyée.

## typeProspectionCode

Code du type de document — **c'est ce champ qu'il faut tester**, jamais le libellé.
`ComProspection`, `ComPreProspection`, `RntProspection`.

Sur `GET /api/pre-prospections`, toujours `ComPreProspection` ; sur `GET /api/prospections`, jamais
— les deux listes se partagent les lignes sur ce seul critère. Cette dernière garde les 4
prospections de location (`RntProspection`) et les 4 lignes sans type (`null`).
