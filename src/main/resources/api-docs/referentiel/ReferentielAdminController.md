# ReferentielAdminController

Écran Configuration : les référentiels OMRAN (Articles, Prospections de terrains, Autres) en lecture pour tout compte
connecté et en écriture pour l'Administrateur. Une seule implémentation pilotée par le descripteur de chaque
référentiel (`GET /api/referentiels-admin`) : une ligne lue ou écrite est un objet JSON dont les clés sont les `nom`
des champs, plus `id`. Hors portée des clés d'API (403). Voir `FEATURE-patrimoine-referentiels.md`.

## descripteurs

Les 19 référentiels, dans l'ordre des menus OMRAN. Pour chacun : ses champs (type, obligatoire, appartenance à la clé
d'unicité, longueur maximale), la route de la liste de choix de chaque référence, les valeurs d'un opérateur, les
champs des lignes enfants (`LIGNES`), et les codes protégés.

## lignes

Lignes complètes, triées par ordre (`sorting`) puis code quand la table en a. Un référentiel par tranche (types de
présentation, quotes-parts exonérées, seuils de versement) ne liste jamais une ligne d'une tranche dont l'entité
juridique est inactive ; un seuil sans tranche est global. Les seuils de versement ne listent que ceux des dossiers
de vente (`ComFolder`) : les lignes des autres entités de workflow restent intactes et invisibles. `trancheId` et
`global` sont ignorés par un référentiel sans tranche.

## ligne

Même ligne que dans la liste. Les types de présentation portent `contenances`, la liste de leurs lignes de contenance.

## creerLigne

Tous les champs obligatoires du descripteur sont requis ; une clé hors descripteur → 400. Texte : au plus
`longueurMax` caractères, les obligatoires sont rognés ; `REFERENCE` : PKey d'une ligne de la liste de choix (sinon
400 nommant le champ), une tranche inconnue ou d'une entité inactive → 404 ; `FRACTION` entre 0 et 1 ; `OPERATEUR`
0 (sans contrôle), 1 (`>=`) ou 2 (`>`). Unicité : une autre ligne de même clé (code ; nature + code pour catégories et
types ; tranche + code pour les présentations ; tranche + année pour les quotes-parts ; tranche + statut pour les
seuils, une tranche absente comptant comme une valeur) → 409. Un seuil est toujours écrit pour les dossiers de vente
et son statut doit être un statut de dossier de vente (sinon 400).

## modifierLigne

Seuls les champs présents changent ; `null` vide un champ facultatif et un obligatoire refuse d'être vidé (400). Mêmes
contrôles qu'à la création, sur la ligne résultante. Le code d'une ligne protégée (vocations `VENTE`, `PROJET` ; natures
`BAT`, `TER`, `PROS TER`, `LOT DE TERRAIN`, reconnues par PKey comme par code) ne change pas → 409 ; sa description
reste modifiable. `contenances` présent remplace toutes les lignes de contenance de la présentation.

## supprimerLigne

Refusée (409) pour une ligne protégée, pour une ligne encore citée par une autre table (le `detail` nomme la
contrainte), et quand la base effacerait en cascade des données d'article : tracés de plan d'un étage, vues ou
contenances d'article, catégories et types d'une nature (le `detail` nomme la table enfant et le nombre de lignes).
Seule exception : les contenances d'un type de présentation partent avec lui.
