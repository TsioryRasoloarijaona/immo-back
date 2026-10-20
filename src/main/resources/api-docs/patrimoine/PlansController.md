# PlansController

## tranchesAvecPlans

Tranches ayant au moins un plan (`StkHierarchyPlan`), restreintes comme toutes les routes de plans : entité juridique
active et projet dans le périmètre commercial de l'appelant (rôles exemptés : tout). Triées par code. Route hors portée
des clés d'API.

## plansDeLaTranche

Métadonnées seulement : l'image (jusqu'à 3,4 Mo) ne se lit que par `/api/plans/{planId}/image`. `nombreTraces` compte
les tracés de l'étage du plan **portés par des articles de la tranche** (un étage est partagé entre tranches). Toutes
les routes de plans répondent 404 quand la tranche est inconnue, d'une entité inactive (Administrateur compris) ou hors
du périmètre commercial de l'appelant. Hors portée des clés d'API (403).

## image

Binaire décodé de la data-URI enregistrée (`image/png` ou `image/jpeg`). L'`ETag` est calculé sans lire l'image (PKey,
taille de l'image, dates de création et de mise à jour) : il change à chaque réimport. Le front charge l'image par
`fetch` (`credentials: 'include'`) puis `URL.createObjectURL` ; le cache HTTP renvoie `If-None-Match` et reçoit 304.

## traces

Tracés de l'étage du plan dont l'article est **de la tranche du plan** (`StkHierarchyPath.StkFloorId` = étage du plan
et `StkItem.StkHierarchyId` = tranche du plan). Occupant = le dossier de vente de l'article dont le statut est actif
(ni annulation ni résiliation) ; `StkItem.ComFolderId` n'est pas lu. Sans occupant : `libre` vrai, statut, couleur,
dossier et client nuls. `couleur` = `StrStatus.ColorName` brut (nom de couleur CSS : Magenta, Gold…), nul s'il n'est
pas renseigné : le front applique alors sa couleur neutre. `estSousArticle` vrai : le front refuse la prospection
(« Vous ne pouvez pas ajouter une prospection pour un sous article »).

## plansDeLArticle

Plans de la tranche de l'article dont l'étage porte un tracé de cet article. L'étage du tracé fait foi : il peut
différer de l'étage de la fiche article (`StkItem.StkFloorId`).

## importer

Règles de l'import OMRAN. Le fichier doit contenir un `<image width height x y xlink:href>` (ou `href` SVG 2) et au
moins un `<path id d>`. Refus 400, rien n'est écrit, messages exacts : « Votre fichier ne contient aucune segmentation
! », « Votre fichier ne contient pas une image de plan ! », « La valeur de la largeur et/ou de la hauteur incorrecte !
», « Il existe dans ce fichier une affectation d'un segment à plusieur articles ! » (même `id` deux fois). Sécurité :
DOCTYPE, entités et XInclude refusés (400) ; image acceptée seulement en `data:image/png;base64,` ou
`data:image/jpeg;base64,` dont les octets sont bien un PNG ou un JPEG (400 sinon) ; `d` limité aux commandes de tracé,
nombres, virgules et espaces (400 sinon) ; plus de 50 Mo → 413.

Plan : un par (tranche, étage) ; créé avec Code = Description = code de l'étage, ou mis à jour (image, largeur,
hauteur, x, y ; Code et Description gardés). Tracés : `id` = code d'article de la tranche (sous-articles compris),
cherché en majuscules puis avec `_` → espace ; introuvable → ligne INTROUVABLE « Le code article saisi est introuvable
dans notre référenciel », les autres tracés sont importés. Tracé de l'article sur cet étage absent → AJOUTE, identique
→ INCHANGE (« n'a pas été modifié »), différent → MODIFIE. `apercu=true` rend le même bilan sans rien écrire. Une
seule transaction.

## purger

Supprime les tracés des articles de la tranche (tous étages), puis ses plans ; une ligne d'audit `Deleted` par plan.
Une tranche sans plan répond aussi 204. Les plans et tracés des autres tranches du même étage sont gardés.
